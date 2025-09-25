package com.engineerplatform.backend.controller;

import com.engineerplatform.backend.dto.UserUpdateDto;
import com.engineerplatform.backend.model.User;
import com.engineerplatform.backend.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        logger.debug("Fetching current user profile for: {}", authentication.getName());
        
        try {
            User user = userService.getUserByUsername(authentication.getName());
            return ResponseEntity.ok(createUserResponse(user));
            
        } catch (Exception e) {
            logger.error("Error fetching current user profile for {}: {}", 
                        authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch user profile: " + e.getMessage()));
        }
    }
    
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(
            @Valid @RequestBody UserUpdateDto updateDto,
            Authentication authentication) {
        
        logger.info("Updating user profile for: {}", authentication.getName());
        
        try {
            User currentUser = userService.getUserByUsername(authentication.getName());
            User updatedUser = userService.updateUser(currentUser.getId(), updateDto);
            
            return ResponseEntity.ok(Map.of(
                "message", "User profile updated successfully",
                "user", createUserResponse(updatedUser)
            ));
            
        } catch (Exception e) {
            logger.error("Error updating user profile for {}: {}", 
                        authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update user profile: " + e.getMessage()));
        }
    }
    
    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('MANAGER', 'LEADER', 'ADMIN')")
    public ResponseEntity<?> getTeamMembers(Authentication authentication) {
        logger.debug("Fetching team members for manager: {}", authentication.getName());
        
        try {
            User manager = userService.getUserByUsername(authentication.getName());
            List<User> teamMembers = userService.getDirectReports(manager);
            
            List<Map<String, Object>> teamResponse = teamMembers.stream()
                .map(this::createUserResponse)
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "teamMembers", teamResponse,
                "totalCount", teamMembers.size()
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching team members for {}: {}", 
                        authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch team members: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'LEADER', 'ADMIN')")
    public ResponseEntity<?> getUserById(
            @PathVariable Long userId,
            Authentication authentication) {
        
        logger.debug("Fetching user {} by manager {}", userId, authentication.getName());
        
        try {
            User manager = userService.getUserByUsername(authentication.getName());
            User targetUser = userService.getUserById(userId);
            
            if (!userService.canManagerAccessUser(manager, targetUser)) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied: You cannot view this user's profile"));
            }
            
            return ResponseEntity.ok(createUserResponse(targetUser));
            
        } catch (Exception e) {
            logger.error("Error fetching user {} by manager {}: {}", 
                        userId, authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch user: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateDto updateDto,
            Authentication authentication) {
        
        logger.info("Admin {} updating user {}", authentication.getName(), userId);
        
        try {
            User updatedUser = userService.updateUser(userId, updateDto);
            
            return ResponseEntity.ok(Map.of(
                "message", "User updated successfully",
                "user", createUserResponse(updatedUser)
            ));
            
        } catch (Exception e) {
            logger.error("Error updating user {} by admin {}: {}", 
                        userId, authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update user: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{userId}/assign-manager")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> assignManager(
            @PathVariable Long userId,
            @RequestParam Long managerId,
            Authentication authentication) {
        
        logger.info("Admin {} assigning manager {} to user {}", 
                   authentication.getName(), managerId, userId);
        
        try {
            User updatedUser = userService.assignManager(userId, managerId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Manager assigned successfully",
                "user", createUserResponse(updatedUser)
            ));
            
        } catch (Exception e) {
            logger.error("Error assigning manager {} to user {} by admin {}: {}", 
                        managerId, userId, authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to assign manager: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(
            @PathVariable Long userId,
            Authentication authentication) {
        
        logger.info("Admin {} deactivating user {}", authentication.getName(), userId);
        
        try {
            userService.deactivateUser(userId);
            
            return ResponseEntity.ok(Map.of(
                "message", "User deactivated successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error deactivating user {} by admin {}: {}", 
                        userId, authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to deactivate user: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{userId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> activateUser(
            @PathVariable Long userId,
            Authentication authentication) {
        
        logger.info("Admin {} activating user {}", authentication.getName(), userId);
        
        try {
            userService.activateUser(userId);
            
            return ResponseEntity.ok(Map.of(
                "message", "User activated successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error activating user {} by admin {}: {}", 
                        userId, authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to activate user: " + e.getMessage()));
        }
    }
    
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        logger.debug("Admin {} fetching all users", authentication.getName());
        
        try {
            List<User> users = userService.getAllUsers();
            
            List<Map<String, Object>> usersResponse = users.stream()
                .map(this::createUserResponse)
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "users", usersResponse,
                "totalCount", users.size()
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching all users by admin {}: {}", 
                        authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
        }
    }
    
    @GetMapping("/managers")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> getAllManagers(Authentication authentication) {
        logger.debug("Admin {} fetching all managers", authentication.getName());
        
        try {
            List<User> managers = userService.getAllManagers();
            
            List<Map<String, Object>> managersResponse = managers.stream()
                .map(this::createUserResponse)
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "managers", managersResponse,
                "totalCount", managers.size()
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching all managers by admin {}: {}", 
                        authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch managers: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("fullName", user.getFullName());
        response.put("role", user.getRole().name());
        response.put("enabled", user.isEnabled());
        response.put("createdAt", user.getCreatedAt());
        response.put("lastLogin", user.getLastLogin() != null ? user.getLastLogin() : null);
        response.put("githubUsername", user.getGithubUsername() != null ? user.getGithubUsername() : "");
        response.put("slackUserId", user.getSlackUserId() != null ? user.getSlackUserId() : "");
        response.put("jiraUsername", user.getJiraUsername() != null ? user.getJiraUsername() : "");
        response.put("confluenceUsername", user.getConfluenceUsername() != null ? user.getConfluenceUsername() : "");
        response.put("googleEmail", user.getGoogleEmail() != null ? user.getGoogleEmail() : "");
        response.put("microsoftEmail", user.getMicrosoftEmail() != null ? user.getMicrosoftEmail() : "");
        response.put("managerId", user.getManager() != null ? user.getManager().getId() : null);
        response.put("managerName", user.getManager() != null ? user.getManager().getFullName() : null);
        return response;
    }
}
