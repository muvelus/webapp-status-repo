# Engineer Work Platform

A comprehensive platform that aggregates, analyzes, and summarizes an engineer's work across various collaboration and development tools.

## Project Overview

This platform consists of two main components:
1. **Backend Service (MCP Server)** - Java Spring Boot application with Maven
2. **Web Application** - React TypeScript frontend with modern UI

## Architecture

### Backend Service
- **Language/Framework**: Java 17 with Spring Boot 3.1.5
- **Build Tool**: Maven
- **Architecture**: RESTful API Service
- **Database**: H2 (development), PostgreSQL (production)
- **Security**: JWT Authentication with Spring Security
- **Local LLM**: Ollama integration for summarization

### Frontend Application
- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **UI Library**: Tailwind CSS with shadcn/ui components
- **State Management**: React Context API
- **Charts**: Recharts for data visualization

## Core Features

### Backend Features
1. **API Integrations**:
   - GitHub (commits, pull requests, comments, repo activity)
   - Zoom (meeting recordings/transcripts, chat)
   - Microsoft Teams (meeting recordings/transcripts, chat)
   - Slack (public/private channel messages)
   - Confluence (document creation, edits, comments)
   - Google Docs (document creation, edits, comments)
   - Jira (ticket creation, updates, comments, status changes)
   - Google Calendar & Microsoft Outlook (meeting context and scheduling)
   - Gmail (email context)

2. **Core Functionality**:
   - Engineer Contribution Summary
   - Automated Meeting Minutes (MoM)
   - Customer Issue Resolution Tracking
   - Documentation Activity Tracking
   - Daily and Weekly Summary Reports
   - Automated Email Reports

### Frontend Features
1. **Authentication**: Secure login with JWT and SSO support
2. **User Dashboard**: Work summary with time-based filtering
3. **Manager & Leadership View**: Team oversight and reporting
4. **Scheduled Email Reports**: Configurable automated reports
5. **Meeting MoM Repository**: Searchable meeting minutes

## Database Schema

### Core Tables
- **users**: User accounts with roles and integration credentials
- **teams**: Team structure and management
- **work_summaries**: Daily/weekly work aggregations
- **meeting_minutes**: AI-generated meeting summaries
- **report_schedules**: Automated report configurations

### Key Relationships
- Users can have managers (self-referencing)
- Teams have team leads and members (many-to-many)
- Work summaries belong to users
- Meeting participants (many-to-many between users and meetings)

## Local Development

### Prerequisites
- Java 17
- Maven 3.6+
- Node.js 18+
- npm/yarn/pnpm

### Backend Setup
```bash
cd backend-service/engineer-work-backend
mvn clean install
mvn spring-boot:run
```

### Frontend Setup
```bash
cd webapp-ui
npm install
npm run dev
```

### Environment Configuration
Create `.env` file in webapp-ui:
```
VITE_API_BASE_URL=http://localhost:8080/api
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Work Summaries
- `GET /api/work-summaries` - Get user work summaries
- `POST /api/work-summaries/generate` - Generate new summary
- `GET /api/work-summaries/{id}` - Get specific summary

### Meetings
- `GET /api/meetings` - Get user meetings
- `POST /api/meetings/{id}/regenerate` - Regenerate meeting minutes

### Team Management
- `GET /api/teams` - Get user teams
- `GET /api/teams/{id}/members` - Get team members
- `GET /api/teams/{id}/summaries` - Get team work summaries

### User Management
- `GET /api/users/profile` - Get user profile
- `PUT /api/users/profile` - Update user profile

## AWS Deployment

### Recommended Architecture
- **EC2 Instance**: t3.medium (cost-effective for initial deployment)
- **Database**: RDS PostgreSQL (db.t3.micro for development)
- **Storage**: EBS for application data
- **Load Balancer**: Application Load Balancer (optional for production)

### Required AWS Services
1. **EC2** - Application hosting
2. **RDS** - PostgreSQL database
3. **S3** - Static file storage (optional)
4. **CloudWatch** - Monitoring and logging
5. **Route 53** - DNS management (optional)

### Deployment Steps
1. Launch EC2 instance with Ubuntu 22.04
2. Install Java 17, Maven, Node.js
3. Set up PostgreSQL RDS instance
4. Configure security groups for ports 80, 443, 8080
5. Deploy application using Docker or direct deployment
6. Set up SSL certificates with Let's Encrypt
7. Configure CloudWatch for monitoring

## Ollama Integration

### Recommended Model
**Model**: `llama2:7b` or `mistral:7b`
- **Reasoning**: Good balance of performance and resource efficiency
- **Memory Requirements**: ~8GB RAM
- **Use Cases**: Meeting summarization, action item extraction, work summary generation

### Installation
```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Pull recommended model
ollama pull llama2:7b

# Start Ollama service
ollama serve
```

## Security Features

1. **JWT Authentication**: Secure token-based authentication
2. **Role-Based Access Control**: ENGINEER, MANAGER, LEADER, ADMIN roles
3. **CORS Configuration**: Secure cross-origin requests
4. **Password Encryption**: BCrypt password hashing
5. **API Rate Limiting**: Protection against abuse
6. **Input Validation**: Comprehensive request validation

## Monitoring & Logging

### Backend Logging
- **Framework**: SLF4J with Logback
- **Levels**: DEBUG, INFO, WARN, ERROR
- **Features**: Structured logging with correlation IDs

### Monitoring
- **Health Checks**: Spring Boot Actuator endpoints
- **Metrics**: Application performance metrics
- **Alerts**: CloudWatch alarms for critical issues

## Development Guidelines

### Code Quality
- Follow Java OOP principles
- Implement comprehensive error handling
- Write unit and integration tests
- Use proper logging throughout the application
- Follow REST API best practices

### Security Best Practices
- Never commit secrets or API keys
- Use environment variables for configuration
- Implement proper input validation
- Follow OWASP security guidelines
- Regular security audits and updates

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement changes with tests
4. Run linting and tests
5. Submit a pull request

## License

This project is proprietary software developed for internal use.
