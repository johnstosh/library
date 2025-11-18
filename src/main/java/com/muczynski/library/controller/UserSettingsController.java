package com.muczynski.library.controller;

import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.service.UserSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    private String extractUsername(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("No authenticated user");
        }

        // Handle different authentication types
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            Object principalObj = ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
            if (principalObj instanceof UserDetails) {
                return ((UserDetails) principalObj).getUsername();
            }
        } else if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            Object principalObj = ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) principal).getPrincipal();
            if (principalObj instanceof OidcUser) {
                // For SSO users, get the username from their database record (which uses their real name)
                OidcUser oidcUser = (OidcUser) principalObj;
                String name = oidcUser.getFullName();
                return name != null && !name.trim().isEmpty() ? name : oidcUser.getEmail();
            } else if (principalObj instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principalObj;
                String name = oauth2User.getAttribute("name");
                return name != null ? name : oauth2User.getAttribute("email");
            }
        }

        // Fallback to principal name
        return principal.getName();
    }

    @GetMapping
    public ResponseEntity<UserDto> getUserSettings(Principal principal) {
        String username = extractUsername(principal);
        UserDto userDto = userSettingsService.getUserSettings(username);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping
    public ResponseEntity<UserDto> updateUserSettings(Principal principal, @RequestBody UserSettingsDto userSettingsDto) {
        String username = extractUsername(principal);
        UserDto updatedUser = userSettingsService.updateUserSettings(username, userSettingsDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteUser(Principal principal) {
        String username = extractUsername(principal);
        userSettingsService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }
}
