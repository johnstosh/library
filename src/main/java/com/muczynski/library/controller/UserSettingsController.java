package com.muczynski.library.controller;

import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.service.UserSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-settings")
public class UserSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsController.class);

    @Autowired
    private UserSettingsService userSettingsService;

    @GetMapping
    public ResponseEntity<UserDto> getUserSettings(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            UserDto userDto = userSettingsService.getUserSettings(userDetails.getUsername());
            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            logger.debug("Failed to get user settings for {}: {}", userDetails.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping
    public ResponseEntity<UserDto> updateUserSettings(@AuthenticationPrincipal UserDetails userDetails, @RequestBody UserSettingsDto userSettingsDto) {
        try {
            UserDto updatedUser = userSettingsService.updateUserSettings(userDetails.getUsername(), userSettingsDto);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            logger.debug("Failed to update user settings for {} with DTO {}: {}", userDetails.getUsername(), userSettingsDto, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            userSettingsService.deleteUser(userDetails.getUsername());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.debug("Failed to delete user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
