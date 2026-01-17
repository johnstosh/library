/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.BranchDto;
import com.muczynski.library.dto.BranchStatisticsDto;
import com.muczynski.library.service.BranchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private static final Logger logger = LoggerFactory.getLogger(BranchController.class);

    @Autowired
    private BranchService branchService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAllBranches() {
        try {
            List<BranchDto> branches = branchService.getAllBranches();
            return ResponseEntity.ok(branches);
        } catch (Exception e) {
            logger.warn("Failed to retrieve all branches: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getBranchById(@PathVariable Long id) {
        try {
            BranchDto branch = branchService.getBranchById(id);
            return branch != null ? ResponseEntity.ok(branch) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.warn("Failed to retrieve branch by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> createBranch(@RequestBody BranchDto branchDto) {
        try {
            BranchDto created = branchService.createBranch(branchDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.warn("Failed to create branch with DTO {}: {}", branchDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateBranch(@PathVariable Long id, @RequestBody BranchDto branchDto) {
        try {
            BranchDto updated = branchService.updateBranch(id, branchDto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.warn("Failed to update branch ID {} with DTO {}: {}", id, branchDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> deleteBranch(@PathVariable Long id) {
        try {
            branchService.deleteBranch(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.warn("Failed to delete branch ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> getBranchStatistics() {
        try {
            List<BranchStatisticsDto> statistics = branchService.getBranchStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.warn("Failed to retrieve branch statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
