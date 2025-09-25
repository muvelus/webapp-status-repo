# AWS Deployment Guide

## Overview
This guide provides step-by-step instructions for deploying the Engineer Work Platform on AWS with cost optimization in mind.

## AWS Requirements

### Required AWS Credentials and Configuration

#### IAM Permissions Required
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:*",
                "rds:*",
                "s3:*",
                "cloudwatch:*",
                "logs:*",
                "iam:PassRole",
                "elasticloadbalancing:*"
            ],
            "Resource": "*"
        }
    ]
}
```

#### Required Environment Variables
```bash
export AWS_ACCESS_KEY_ID="your-access-key"
export AWS_SECRET_ACCESS_KEY="your-secret-key"
export AWS_DEFAULT_REGION="us-east-1"
export AWS_ACCOUNT_ID="your-account-id"
```

## Cost-Optimized Architecture

### Single EC2 Instance Deployment
For the initial version, we recommend running all components on a single EC2 instance:

- **Instance Type**: `t3.medium` (2 vCPU, 4 GB RAM)
- **Storage**: 20 GB gp3 EBS volume
- **Operating System**: Ubuntu 22.04 LTS
- **Estimated Monthly Cost**: ~$30-40 USD

### Components on Single Instance
1. **Java Spring Boot Backend** (Port 8080)
2. **React Frontend** (Served via Nginx on Port 80/443)
3. **PostgreSQL Database** (Local installation)
4. **Ollama LLM Service** (Port 11434)

## Step-by-Step Deployment

### 1. Launch EC2 Instance

```bash
# Create security group
aws ec2 create-security-group \
    --group-name engineer-platform-sg \
    --description "Security group for Engineer Work Platform"

# Add inbound rules
aws ec2 authorize-security-group-ingress \
    --group-name engineer-platform-sg \
    --protocol tcp \
    --port 22 \
    --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
    --group-name engineer-platform-sg \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
    --group-name engineer-platform-sg \
    --protocol tcp \
    --port 443 \
    --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
    --group-name engineer-platform-sg \
    --protocol tcp \
    --port 8080 \
    --cidr 0.0.0.0/0

# Launch instance
aws ec2 run-instances \
    --image-id ami-0c02fb55956c7d316 \
    --count 1 \
    --instance-type t3.medium \
    --key-name your-key-pair \
    --security-groups engineer-platform-sg \
    --block-device-mappings DeviceName=/dev/sda1,Ebs={VolumeSize=20,VolumeType=gp3}
```

### 2. Server Setup Script

```bash
#!/bin/bash
# server-setup.sh

# Update system
sudo apt update && sudo apt upgrade -y

# Install Java 17
sudo apt install -y openjdk-17-jdk

# Install Maven
sudo apt install -y maven

# Install Node.js 18
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# Install PostgreSQL
sudo apt install -y postgresql postgresql-contrib

# Install Nginx
sudo apt install -y nginx

# Install Docker (for containerized deployment option)
sudo apt install -y docker.io
sudo systemctl enable docker
sudo usermod -aG docker ubuntu

# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Configure PostgreSQL
sudo -u postgres createuser --interactive --pwprompt engineerplatform
sudo -u postgres createdb engineerplatform_db -O engineerplatform

# Configure firewall
sudo ufw allow 22
sudo ufw allow 80
sudo ufw allow 443
sudo ufw allow 8080
sudo ufw --force enable
```

### 3. Application Deployment

#### Backend Deployment
```bash
# Clone repository
git clone https://github.com/muvelus/engineer-work-platform.git
cd engineer-work-platform/backend-service/engineer-work-backend

# Update application.yml for production
cat > src/main/resources/application-prod.yml << EOF
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:postgresql://localhost:5432/engineerplatform_db
    username: engineerplatform
    password: \${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

logging:
  level:
    com.engineerplatform: INFO
  file:
    name: /var/log/engineer-platform/backend.log

ollama:
  base-url: http://localhost:11434
EOF

# Build application
mvn clean package -DskipTests

# Create systemd service
sudo tee /etc/systemd/system/engineer-platform-backend.service << EOF
[Unit]
Description=Engineer Platform Backend
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/engineer-work-platform/backend-service/engineer-work-backend
ExecStart=/usr/bin/java -jar target/engineer-work-backend-1.0.0.jar --spring.profiles.active=prod
Environment=DB_PASSWORD=your_db_password
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl enable engineer-platform-backend
sudo systemctl start engineer-platform-backend
```

#### Frontend Deployment
```bash
cd /home/ubuntu/engineer-work-platform/webapp-ui

# Create production environment file
cat > .env.production << EOF
VITE_API_BASE_URL=https://your-domain.com/api
EOF

# Build frontend
npm install
npm run build

# Configure Nginx
sudo tee /etc/nginx/sites-available/engineer-platform << EOF
server {
    listen 80;
    server_name your-domain.com;
    
    # Redirect HTTP to HTTPS
    return 301 https://\$server_name\$request_uri;
}

server {
    listen 443 ssl;
    server_name your-domain.com;
    
    # SSL configuration (add your certificates)
    ssl_certificate /etc/ssl/certs/your-cert.pem;
    ssl_certificate_key /etc/ssl/private/your-key.pem;
    
    # Frontend
    location / {
        root /home/ubuntu/engineer-work-platform/webapp-ui/dist;
        try_files \$uri \$uri/ /index.html;
    }
    
    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

sudo ln -s /etc/nginx/sites-available/engineer-platform /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

#### Ollama Setup
```bash
# Start Ollama service
sudo systemctl enable ollama
sudo systemctl start ollama

# Pull recommended model
ollama pull llama2:7b

# Create systemd service for Ollama
sudo tee /etc/systemd/system/ollama.service << EOF
[Unit]
Description=Ollama Service
After=network.target

[Service]
Type=simple
User=ubuntu
ExecStart=/usr/local/bin/ollama serve
Environment=OLLAMA_HOST=0.0.0.0:11434
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl enable ollama
sudo systemctl start ollama
```

### 4. SSL Certificate Setup

```bash
# Install Certbot
sudo apt install -y certbot python3-certbot-nginx

# Obtain SSL certificate
sudo certbot --nginx -d your-domain.com

# Auto-renewal
sudo crontab -e
# Add: 0 12 * * * /usr/bin/certbot renew --quiet
```

### 5. Monitoring Setup

```bash
# Install CloudWatch agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i amazon-cloudwatch-agent.deb

# Configure CloudWatch agent
sudo tee /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json << EOF
{
    "logs": {
        "logs_collected": {
            "files": {
                "collect_list": [
                    {
                        "file_path": "/var/log/engineer-platform/backend.log",
                        "log_group_name": "engineer-platform-backend",
                        "log_stream_name": "{instance_id}"
                    },
                    {
                        "file_path": "/var/log/nginx/access.log",
                        "log_group_name": "engineer-platform-nginx",
                        "log_stream_name": "{instance_id}"
                    }
                ]
            }
        }
    },
    "metrics": {
        "namespace": "EngineerPlatform",
        "metrics_collected": {
            "cpu": {
                "measurement": ["cpu_usage_idle", "cpu_usage_iowait"],
                "metrics_collection_interval": 60
            },
            "disk": {
                "measurement": ["used_percent"],
                "metrics_collection_interval": 60,
                "resources": ["*"]
            },
            "mem": {
                "measurement": ["mem_used_percent"],
                "metrics_collection_interval": 60
            }
        }
    }
}
EOF

sudo systemctl enable amazon-cloudwatch-agent
sudo systemctl start amazon-cloudwatch-agent
```

## Database Schema Migration

### Production Database Setup
```sql
-- Connect to PostgreSQL as engineerplatform user
-- The schema will be automatically created by Hibernate on first run
-- with spring.jpa.hibernate.ddl-auto=update

-- Optional: Create indexes for performance
CREATE INDEX idx_work_summaries_user_date ON work_summaries(user_id, summary_date);
CREATE INDEX idx_meeting_participants_user ON meeting_participants(user_id);
CREATE INDEX idx_meeting_participants_meeting ON meeting_participants(meeting_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
```

## Backup Strategy

```bash
# Database backup script
#!/bin/bash
# backup-db.sh

BACKUP_DIR="/home/ubuntu/backups"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="engineerplatform_db"

mkdir -p $BACKUP_DIR

pg_dump -U engineerplatform -h localhost $DB_NAME > $BACKUP_DIR/db_backup_$DATE.sql

# Upload to S3 (optional)
aws s3 cp $BACKUP_DIR/db_backup_$DATE.sql s3://your-backup-bucket/database/

# Keep only last 7 days of backups
find $BACKUP_DIR -name "db_backup_*.sql" -mtime +7 -delete

# Add to crontab for daily backups
# 0 2 * * * /home/ubuntu/backup-db.sh
```

## Cost Optimization Tips

1. **Use Spot Instances**: For development environments
2. **Schedule Shutdown**: Stop instances during non-business hours
3. **Monitor Usage**: Use CloudWatch to track resource utilization
4. **Right-size Instances**: Start small and scale up as needed
5. **Use Reserved Instances**: For production workloads with predictable usage

## Troubleshooting

### Common Issues
1. **Port 8080 not accessible**: Check security group and firewall rules
2. **Database connection failed**: Verify PostgreSQL service and credentials
3. **Ollama not responding**: Check service status and memory usage
4. **Frontend not loading**: Verify Nginx configuration and build files

### Log Locations
- Backend: `/var/log/engineer-platform/backend.log`
- Nginx: `/var/log/nginx/access.log` and `/var/log/nginx/error.log`
- PostgreSQL: `/var/log/postgresql/postgresql-14-main.log`
- Ollama: `journalctl -u ollama`

## Security Checklist

- [ ] Update all system packages
- [ ] Configure firewall (UFW)
- [ ] Set up SSL certificates
- [ ] Use strong database passwords
- [ ] Configure fail2ban for SSH protection
- [ ] Regular security updates
- [ ] Monitor access logs
- [ ] Backup encryption
