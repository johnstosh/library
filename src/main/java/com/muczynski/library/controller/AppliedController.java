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
        Applied applied = new Applied();
        applied.setName(registrationRequest.getUsername());
        applied.setPassword(registrationRequest.getPassword());
        appliedService.createApplied(applied);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/applied")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<Applied>> getAllApplied() {
        List<Applied> applied = appliedService.getAllApplied();
        return ResponseEntity.ok(applied);
    }

    @PostMapping("/applied")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Applied> createApplied(@RequestBody Applied applied) {
        Applied createdApplied = appliedService.createApplied(applied);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdApplied);
    }

    @PutMapping("/applied/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Applied> updateApplied(@PathVariable Long id, @RequestBody Applied applied) {
        Applied updatedApplied = appliedService.updateApplied(id, applied);
        return ResponseEntity.ok(updatedApplied);
    }

    @DeleteMapping("/applied/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> deleteApplied(@PathVariable Long id) {
        appliedService.deleteApplied(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/applied/{id}/approve")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> approveApplication(@PathVariable Long id) {
        appliedService.approveApplication(id);
        return ResponseEntity.ok().build();
    }
}