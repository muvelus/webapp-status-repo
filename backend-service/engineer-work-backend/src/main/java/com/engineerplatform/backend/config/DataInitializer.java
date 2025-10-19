package com.engineerplatform.backend.config;

import com.engineerplatform.backend.model.User;
import com.engineerplatform.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Data initializer that creates default test users on application startup.
 * This ensures there are users available for testing and development.
 */
@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting data initialization...");
        
        if (userRepository.count() == 0) {
            logger.info("No users found. Creating default test users...");
            
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setRole(User.Role.ADMIN);
            admin.setEnabled(true);
            admin.setAccountNonExpired(true);
            admin.setAccountNonLocked(true);
            admin.setCredentialsNonExpired(true);
            userRepository.save(admin);
            logger.info("Created admin user: username='admin', password='password123'");
            
            User engineer = new User();
            engineer.setUsername("testuser");
            engineer.setEmail("testuser@example.com");
            engineer.setFirstName("Test");
            engineer.setLastName("User");
            engineer.setPassword(passwordEncoder.encode("password123"));
            engineer.setRole(User.Role.ENGINEER);
            engineer.setEnabled(true);
            engineer.setAccountNonExpired(true);
            engineer.setAccountNonLocked(true);
            engineer.setCredentialsNonExpired(true);
            userRepository.save(engineer);
            logger.info("Created test user: username='testuser', password='password123'");
            
            User manager = new User();
            manager.setUsername("manager");
            manager.setEmail("manager@example.com");
            manager.setFirstName("Manager");
            manager.setLastName("User");
            manager.setPassword(passwordEncoder.encode("password123"));
            manager.setRole(User.Role.MANAGER);
            manager.setEnabled(true);
            manager.setAccountNonExpired(true);
            manager.setAccountNonLocked(true);
            manager.setCredentialsNonExpired(true);
            userRepository.save(manager);
            logger.info("Created manager user: username='manager', password='password123'");
            
            logger.info("Data initialization completed successfully!");
        } else {
            logger.info("Users already exist. Skipping data initialization.");
            
            logger.info("Checking for users with NULL boolean fields...");
            userRepository.findAll().forEach(user -> {
                boolean updated = false;
                
                if (!user.isEnabled()) {
                    user.setEnabled(true);
                    updated = true;
                }
                if (!user.isAccountNonExpired()) {
                    user.setAccountNonExpired(true);
                    updated = true;
                }
                if (!user.isAccountNonLocked()) {
                    user.setAccountNonLocked(true);
                    updated = true;
                }
                if (!user.isCredentialsNonExpired()) {
                    user.setCredentialsNonExpired(true);
                    updated = true;
                }
                
                if (updated) {
                    userRepository.save(user);
                    logger.info("Fixed boolean fields for user: {}", user.getUsername());
                }
            });
        }
    }
}
