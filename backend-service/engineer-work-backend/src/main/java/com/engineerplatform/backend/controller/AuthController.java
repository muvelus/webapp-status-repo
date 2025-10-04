package com.engineerplatform.backend.controller;

import com.engineerplatform.backend.dto.JwtResponse;
import com.engineerplatform.backend.dto.LoginRequest;
import com.engineerplatform.backend.dto.UserRegistrationDto;
import com.engineerplatform.backend.model.User;
import com.engineerplatform.backend.security.JwtUtil;
import com.engineerplatform.backend.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    
    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                         UserService userService,
                         JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String identifier = loginRequest.getUsername();
        logger.info("Authentication attempt for identifier: {}", identifier);
        
        try {
            String username = identifier;
            
            // Convert email to username if needed
            if (identifier != null && identifier.contains("@")) {
                try {
                    User userByEmail = userService.getUserByEmail(identifier);
                    username = userByEmail.getUsername();
                    logger.info("Email login: {} -> username: {}", identifier, username);
                } catch (Exception e) {
                    logger.warn("Email not found: {}", identifier);
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid email or password"));
                }
            }
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );
            
            // Rest of authentication logic...
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.getUserByUsername(userDetails.getUsername());
            
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("role", user.getRole().name());
            claims.put("email", user.getEmail());
            
            String jwt = jwtUtil.generateToken(userDetails, claims);
            userService.updateLastLogin(user.getUsername());
            
            return ResponseEntity.ok(new JwtResponse(
                jwt, user.getId(), user.getUsername(), 
                user.getEmail(), user.getRole().name(), 
                jwtUtil.getExpirationTime()
            ));
            
        } catch (Exception e) {
            logger.error("Authentication failed for {}: {}", identifier, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid email or password"));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        logger.info("Registration attempt for user: {}", registrationDto.getUsername());
        
        try {
            if (userService.existsByUsername(registrationDto.getUsername())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username is already taken"));
            }
            
            if (userService.existsByEmail(registrationDto.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is already in use"));
            }
            
            User user = userService.createUser(registrationDto);
            
            logger.info("Successful registration for user: {}", registrationDto.getUsername());
            
            return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "userId", user.getId(),
                "username", user.getUsername()
            ));
            
        } catch (Exception e) {
            logger.error("Registration failed for user {}: {}", registrationDto.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        logger.debug("Token refresh request received");
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid authorization header"));
            }
            
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);
            
            if (username != null && jwtUtil.isTokenValid(token)) {
                UserDetails userDetails = userService.loadUserByUsername(username);
                User user = userService.getUserByUsername(username);
                
                Map<String, Object> claims = new HashMap<>();
                claims.put("userId", user.getId());
                claims.put("role", user.getRole().name());
                claims.put("email", user.getEmail());
                
                String newToken = jwtUtil.generateToken(userDetails, claims);
                
                logger.debug("Token refreshed successfully for user: {}", username);
                
                return ResponseEntity.ok(new JwtResponse(
                    newToken,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name(),
                    jwtUtil.getExpirationTime()
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid or expired token"));
            }
            
        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Token refresh failed"));
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("valid", false, "error", "Invalid authorization header"));
            }
            
            String token = authHeader.substring(7);
            boolean isValid = jwtUtil.isTokenValid(token);
            
            if (isValid) {
                String username = jwtUtil.extractUsername(token);
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "username", username
                ));
            } else {
                return ResponseEntity.ok(Map.of("valid", false));
            }
            
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("valid", false));
        }
    }
}
