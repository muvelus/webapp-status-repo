package com.engineerplatform.backend.service;

import com.engineerplatform.backend.dto.UserRegistrationDto;
import com.engineerplatform.backend.dto.UserUpdateDto;
import com.engineerplatform.backend.exception.ResourceNotFoundException;
import com.engineerplatform.backend.exception.UserAlreadyExistsException;
import com.engineerplatform.backend.model.User;
import com.engineerplatform.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UserService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
    
    public User createUser(UserRegistrationDto registrationDto) {
        logger.info("Creating new user with email: {}", registrationDto.getEmail());
        
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("User with email already exists: " + registrationDto.getEmail());
        }
        
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new UserAlreadyExistsException("User with username already exists: " + registrationDto.getUsername());
        }
        
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRole(registrationDto.getRole() != null ? registrationDto.getRole() : User.Role.ENGINEER);
        
        User savedUser = userRepository.save(user);
        logger.info("Successfully created user with ID: {}", savedUser.getId());
        return savedUser;
    }
    
    public User updateUser(Long userId, UserUpdateDto updateDto) {
        logger.info("Updating user with ID: {}", userId);
        
        User user = getUserById(userId);
        
        if (updateDto.getFirstName() != null) {
            user.setFirstName(updateDto.getFirstName());
        }
        if (updateDto.getLastName() != null) {
            user.setLastName(updateDto.getLastName());
        }
        if (updateDto.getGithubUsername() != null) {
            user.setGithubUsername(updateDto.getGithubUsername());
        }
        if (updateDto.getSlackUserId() != null) {
            user.setSlackUserId(updateDto.getSlackUserId());
        }
        if (updateDto.getJiraUsername() != null) {
            user.setJiraUsername(updateDto.getJiraUsername());
        }
        if (updateDto.getConfluenceUsername() != null) {
            user.setConfluenceUsername(updateDto.getConfluenceUsername());
        }
        if (updateDto.getGoogleEmail() != null) {
            user.setGoogleEmail(updateDto.getGoogleEmail());
        }
        if (updateDto.getMicrosoftEmail() != null) {
            user.setMicrosoftEmail(updateDto.getMicrosoftEmail());
        }
        if (updateDto.getRole() != null) {
            user.setRole(updateDto.getRole());
        }
        
        User updatedUser = userRepository.save(user);
        logger.info("Successfully updated user with ID: {}", updatedUser.getId());
        return updatedUser;
    }
    
    public User getUserById(Long id) {
        logger.debug("Fetching user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }
    
    public User getUserByUsername(String username) {
        logger.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
    
    public User getUserByEmail(String email) {
        logger.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
    
    public List<User> getAllUsers() {
        logger.debug("Fetching all users");
        return userRepository.findAll();
    }
    
    public List<User> getActiveUsers() {
        logger.debug("Fetching all active users");
        return userRepository.findAllActiveUsers();
    }
    
    public List<User> getUsersByRole(User.Role role) {
        logger.debug("Fetching users by role: {}", role);
        return userRepository.findByRole(role);
    }
    
    public List<User> getDirectReports(User manager) {
        logger.debug("Fetching direct reports for manager: {}", manager.getUsername());
        return userRepository.findDirectReports(manager);
    }
    
    public List<User> getAllManagers() {
        logger.debug("Fetching all managers");
        return userRepository.findAllManagers();
    }
    
    public User assignManager(Long userId, Long managerId) {
        logger.info("Assigning manager {} to user {}", managerId, userId);
        
        User user = getUserById(userId);
        User manager = getUserById(managerId);
        
        if (!manager.isManager()) {
            throw new IllegalArgumentException("Assigned manager must have manager role");
        }
        
        user.setManager(manager);
        User updatedUser = userRepository.save(user);
        logger.info("Successfully assigned manager to user with ID: {}", updatedUser.getId());
        return updatedUser;
    }
    
    public void updateLastLogin(String username) {
        logger.debug("Updating last login for user: {}", username);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            logger.debug("Updated last login for user: {}", username);
        }
    }
    
    public void deactivateUser(Long userId) {
        logger.info("Deactivating user with ID: {}", userId);
        User user = getUserById(userId);
        user.setEnabled(false);
        userRepository.save(user);
        logger.info("Successfully deactivated user with ID: {}", userId);
    }
    
    public void activateUser(Long userId) {
        logger.info("Activating user with ID: {}", userId);
        User user = getUserById(userId);
        user.setEnabled(true);
        userRepository.save(user);
        logger.info("Successfully activated user with ID: {}", userId);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public Optional<User> findByGithubUsername(String githubUsername) {
        return userRepository.findByGithubUsername(githubUsername);
    }
    
    public Optional<User> findBySlackUserId(String slackUserId) {
        return userRepository.findBySlackUserId(slackUserId);
    }
    
    public Optional<User> findByJiraUsername(String jiraUsername) {
        return userRepository.findByJiraUsername(jiraUsername);
    }
    
    public Optional<User> findByGoogleEmail(String googleEmail) {
        return userRepository.findByGoogleEmail(googleEmail);
    }
    
    public Optional<User> findByMicrosoftEmail(String microsoftEmail) {
        return userRepository.findByMicrosoftEmail(microsoftEmail);
    }
    
    public boolean canManagerAccessUser(User manager, User targetUser) {
        logger.debug("Checking if manager {} can access user {}", manager.getUsername(), targetUser.getUsername());
        
        if (manager.getRole() == User.Role.ADMIN) {
            return true;
        }
        
        if (manager.getRole() == User.Role.LEADER || manager.getRole() == User.Role.MANAGER) {
            if (targetUser.getManager() != null && targetUser.getManager().getId().equals(manager.getId())) {
                return true;
            }
            
            List<User> directReports = getDirectReports(manager);
            for (User report : directReports) {
                if (report.getId().equals(targetUser.getId())) {
                    return true;
                }
                if (canManagerAccessUser(manager, report)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
