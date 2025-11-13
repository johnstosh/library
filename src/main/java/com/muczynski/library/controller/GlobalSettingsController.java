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
     * Available to all authenticated users (read-only for non-librarians)
     */
    @GetMapping
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
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<GlobalSettingsDto> updateGlobalSettings(@RequestBody GlobalSettingsDto dto) {
        logger.info("Update global settings requested (librarian-only)");
        GlobalSettingsDto updated = globalSettingsService.updateGlobalSettings(dto);
        return ResponseEntity.ok(updated);
    }
}
