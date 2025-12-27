/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.controller.payload.RegistrationRequest;
import com.muczynski.library.domain.Applied;
import com.muczynski.library.dto.AppliedDto;
import com.muczynski.library.mapper.AppliedMapper;
import com.muczynski.library.service.AppliedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AppliedController {

    private static final Logger logger = LoggerFactory.getLogger(AppliedController.class);

    @Autowired
    private AppliedService appliedService;

    @Autowired
    private AppliedMapper appliedMapper;

    @PostMapping("/application/public/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest registrationRequest) {
        logger.info("=== APPLICATION PUBLIC REGISTER ENDPOINT CALLED ===");
        logger.info("Received application registration request - username: {}, authority: {}",
                    registrationRequest.getUsername(), registrationRequest.getAuthority());
        try {
            Applied applied = new Applied();
            applied.setName(registrationRequest.getUsername());
            applied.setPassword(registrationRequest.getPassword());
            Applied createdApplied = appliedService.createApplied(applied);
            logger.info("Successfully created application with ID: {}", createdApplied.getId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Failed to register application with request {}: {}", registrationRequest, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/applied")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> getAllApplied() {
        try {
            List<Applied> applied = appliedService.getAllApplied();
            List<AppliedDto> dtos = applied.stream()
                    .map(appliedMapper::appliedToAppliedDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.warn("Failed to retrieve all applied applications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/applied")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> createApplied(@RequestBody Applied applied) {
        try {
            Applied createdApplied = appliedService.createApplied(applied);
            AppliedDto dto = appliedMapper.appliedToAppliedDto(createdApplied);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            logger.warn("Failed to create applied with entity {}: {}", applied, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/applied/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateApplied(@PathVariable Long id, @RequestBody Applied applied) {
        try {
            Applied updatedApplied = appliedService.updateApplied(id, applied);
            AppliedDto dto = appliedMapper.appliedToAppliedDto(updatedApplied);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.warn("Failed to update applied ID {} with entity {}: {}", id, applied, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/applied/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> deleteApplied(@PathVariable Long id) {
        try {
            appliedService.deleteApplied(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.warn("Failed to delete applied ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/applied/{id}/approve")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> approveApplication(@PathVariable Long id) {
        try {
            appliedService.approveApplication(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to approve application ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
