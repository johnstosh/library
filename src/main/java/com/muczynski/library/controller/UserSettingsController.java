/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.service.UserSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/user-settings")
public class UserSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsController.class);

    @Autowired
    private UserSettingsService userSettingsService;

    /**
     * Extract user ID from the authenticated principal.
     * The principal name is always the database user ID (set during login/OAuth).
     * @param principal The authenticated principal
     * @return User ID as Long
     */
    private Long extractUserId(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("No authenticated user");
        }

        // The principal name is always the database user ID (not username)
        return Long.parseLong(principal.getName());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getUserSettings(Principal principal) {
        Long userId = extractUserId(principal);
        UserDto userDto = userSettingsService.getUserSettings(userId);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateUserSettings(Principal principal, @RequestBody UserSettingsDto userSettingsDto) {
        Long userId = extractUserId(principal);
        UserDto updatedUser = userSettingsService.updateUserSettings(userId, userSettingsDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteUser(Principal principal) {
        Long userId = extractUserId(principal);
        userSettingsService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
