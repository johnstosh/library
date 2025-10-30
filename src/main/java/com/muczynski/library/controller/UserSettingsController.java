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
        UserDto userDto = userSettingsService.getUserSettings(userDetails.getUsername());
        return ResponseEntity.ok(userDto);
    }

    @PutMapping
    public ResponseEntity<UserDto> updateUserSettings(@AuthenticationPrincipal UserDetails userDetails, @RequestBody UserSettingsDto userSettingsDto) {
        UserDto updatedUser = userSettingsService.updateUserSettings(userDetails.getUsername(), userSettingsDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserDetails userDetails) {
        userSettingsService.deleteUser(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
