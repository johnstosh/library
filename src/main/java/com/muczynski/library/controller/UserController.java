/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.CreateUserDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        try {
            logger.info("GET /api/users/me called, authentication: {}", authentication != null ? authentication.getClass().getName() : "null");

            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Authentication is null or not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String username = null;
            Set<String> authorities = null;

            // Handle both UserDetails (form login) and OAuth2User (SSO login)
            Object principal = authentication.getPrincipal();
            logger.info("Principal type: {}", principal != null ? principal.getClass().getName() : "null");
            if (principal instanceof UserDetails) {
                // Traditional form-based login
                UserDetails userDetails = (UserDetails) principal;
                username = userDetails.getUsername();
                authorities = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());
            } else if (principal instanceof OAuth2User) {
                // OAuth2 SSO login
                OAuth2User oauth2User = (OAuth2User) principal;
                // For Google OAuth2, the email is used as the username
                username = oauth2User.getAttribute("email");
                if (username == null) {
                    // Fallback to 'sub' if email not available
                    username = oauth2User.getAttribute("sub");
                }
                authorities = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());
            } else {
                logger.warn("Unknown principal type: {}", principal.getClass().getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if (username == null) {
                logger.warn("Could not determine username from principal");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            logger.info("Username determined: {}, authorities: {}", username, authorities);

            UserDto userDto = new UserDto();
            userDto.setUsername(username);
            userDto.setAuthorities(authorities);

            // Load full user details including ID and API key by username
            UserDto fullUser = userService.getUserByUsername(username);
            if (fullUser != null) {
                logger.info("Found user in database: ID={}, username={}, ssoProvider={}", fullUser.getId(), fullUser.getUsername(), fullUser.getSsoProvider());
                userDto.setId(fullUser.getId());
                userDto.setXaiApiKey(fullUser.getXaiApiKey());
                userDto.setSsoProvider(fullUser.getSsoProvider());
                userDto.setEmail(fullUser.getEmail());
            } else {
                logger.warn("User not found in database for username: {}", username);
            }

            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            logger.warn("Failed to get current user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<UserDto> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.warn("Failed to retrieve all users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserDto user = userService.getUserById(id);
            return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.warn("Failed to retrieve user by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserDto createUserDto) {
        try {
            UserDto createdUser = userService.createUser(createUserDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            logger.warn("Failed to create user with DTO {}: {}", createUserDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/public/register")
    public ResponseEntity<?> registerUser(@RequestBody CreateUserDto createUserDto) {
        logger.info("=== USERS PUBLIC REGISTER ENDPOINT CALLED ===");
        logger.info("Received user registration request - username: {}, authority: {}",
                    createUserDto.getUsername(), createUserDto.getAuthority());
        try {
            if (!"USER".equals(createUserDto.getAuthority())) {
                logger.warn("Rejected registration - authority must be USER, got: {}", createUserDto.getAuthority());
                return ResponseEntity.badRequest().build();
            }
            UserDto createdUser = userService.createUser(createUserDto);
            logger.info("Successfully created user with ID: {}", createdUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            logger.error("Failed to register public user with DTO {}: {}", createUserDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody CreateUserDto createUserDto) {
        try {
            UserDto updatedUser = userService.updateUser(id, createUserDto);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            logger.warn("Failed to update user ID {} with DTO {}: {}", id, createUserDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/apikey")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateApiKey(@PathVariable Long id, @RequestBody UserDto userDto) {
        try {
            userService.updateApiKey(id, userDto.getXaiApiKey());
            UserDto updatedUser = userService.getUserById(id);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            logger.warn("Failed to update API key for user ID {} with key length {}: {}", id, userDto.getXaiApiKey() != null ? userDto.getXaiApiKey().length() : 0, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.warn("Failed to delete user ID {}: {}", id, e.getMessage(), e);
            if (e.getMessage().contains("active loan")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", e.getMessage()));
            }
            throw e;
        }
    }
}
