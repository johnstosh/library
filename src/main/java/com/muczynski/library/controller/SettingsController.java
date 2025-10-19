package com.muczynski.library.controller;

import com.muczynski.library.dto.SettingsDto;
import com.muczynski.library.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('USER')")
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    @Autowired
    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<SettingsDto> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<SettingsDto> updateSettings(@RequestBody SettingsDto settingsDto) {
        return ResponseEntity.ok(settingsService.updateSettings(settingsDto));
    }
}