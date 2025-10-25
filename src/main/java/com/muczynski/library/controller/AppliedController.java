package com.muczynski.library.controller;

import com.muczynski.library.domain.Applied;
import com.muczynski.library.service.AppliedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.muczynski.library.controller.payload.RegistrationRequest;
import com.muczynski.library.domain.Applied;
import com.muczynski.library.service.AppliedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;


import java.util.List;

@RestController
@RequestMapping("/api")
public class AppliedController {

    @Autowired
    private AppliedService appliedService;

    @PostMapping("/public/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest registrationRequest) {
        try {
            Applied applied = new Applied();
            applied.setName(registrationRequest.getUsername());
            applied.setPassword(registrationRequest.getPassword());
            appliedService.createApplied(applied);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/applied")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> getAllApplied() {
        try {
            List<Applied> applied = appliedService.getAllApplied();
            return ResponseEntity.ok(applied);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/applied")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> createApplied(@RequestBody Applied applied) {
        try {
            Applied createdApplied = appliedService.createApplied(applied);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdApplied);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/applied/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateApplied(@PathVariable Long id, @RequestBody Applied applied) {
        try {
            Applied updatedApplied = appliedService.updateApplied(id, applied);
            return ResponseEntity.ok(updatedApplied);
        } catch (Exception e) {
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}