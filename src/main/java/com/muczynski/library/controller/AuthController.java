// (c) Copyright 2025 by Muczynski
package com.muczynski.library.controller;

import com.muczynski.library.domain.User;
import com.muczynski.library.dto.CurrentUserDto;
import com.muczynski.library.dto.LoginRequestDto;
import com.muczynski.library.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication endpoints (login, logout, current user).
 * Handles form-based authentication for the React frontend.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Login endpoint for React frontend.
     * Accepts username and SHA-256 hashed password.
     */
    @PostMapping("/login")
    public ResponseEntity<CurrentUserDto> login(@RequestBody LoginRequestDto loginRequest,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        // Find user by username
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        // Verify password
        // Frontend sends SHA-256 hash, database stores BCrypt(SHA-256(password))
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        // Create authentication token
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                user.getAuthorities()
            );

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Save to session
        request.getSession().setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
        );

        // Return current user info
        CurrentUserDto currentUser = new CurrentUserDto(
            user.getId(),
            user.getUsername(),
            user.getHighestAuthority(),
            user.getSsoSubjectId()
        );

        return ResponseEntity.ok(currentUser);
    }

    /**
     * Logout endpoint for React frontend.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        request.getSession().invalidate();
        return ResponseEntity.ok().build();
    }

    /**
     * Get current authenticated user info.
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CurrentUserDto currentUser = new CurrentUserDto(
            user.getId(),
            user.getUsername(),
            user.getHighestAuthority(),
            user.getSsoSubjectId()
        );

        return ResponseEntity.ok(currentUser);
    }
}
