/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.GlobalSettingsDto;
import com.muczynski.library.service.GlobalSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing global application settings.
 * Only librarians have access to these endpoints.
 */
@RestController
@RequestMapping("/api/global-settings")
public class GlobalSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(GlobalSettingsController.class);

    @Autowired
    private GlobalSettingsService globalSettingsService;

    /**
     * Get global settings
     * Only librarians can view global settings
     */
    @GetMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<GlobalSettingsDto> getGlobalSettings() {
        logger.info("Fetching global settings");
        GlobalSettingsDto settings = globalSettingsService.getGlobalSettingsDto();
        return ResponseEntity.ok(settings);
    }

    /**
     * Update global settings
     * Only librarians can update
     */
    @PutMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<GlobalSettingsDto> updateGlobalSettings(@RequestBody GlobalSettingsDto dto) {
        logger.info("Update global settings requested (librarian-only)");
        GlobalSettingsDto updated = globalSettingsService.updateGlobalSettings(dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Check if SSO is configured
     * Public endpoint - no authentication required
     */
    @GetMapping("/sso-status")
    public ResponseEntity<SsoStatusDto> getSsoStatus() {
        logger.info("Checking SSO configuration status");
        GlobalSettingsDto settings = globalSettingsService.getGlobalSettingsDto();
        SsoStatusDto status = new SsoStatusDto();
        status.setSsoConfigured(settings.isGoogleSsoClientIdConfigured() && settings.isGoogleSsoClientSecretConfigured());
        return ResponseEntity.ok(status);
    }

    /**
     * DTO for SSO status response
     */
    public static class SsoStatusDto {
        private boolean ssoConfigured;

        public boolean isSsoConfigured() {
            return ssoConfigured;
        }

        public void setSsoConfigured(boolean ssoConfigured) {
            this.ssoConfigured = ssoConfigured;
        }
    }
}
