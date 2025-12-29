/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.TestDataResponseDto;
import com.muczynski.library.dto.TestDataStatsDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.TestDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test-data")
public class TestDataController {

    private static final Logger logger = LoggerFactory.getLogger(TestDataController.class);

    @Autowired
    private TestDataService testDataService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/generate")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TestDataResponseDto> generateTestData(@RequestBody Map<String, Integer> payload) {
        try {
            int count = payload.getOrDefault("numBooks", 0);
            testDataService.generateTestData(count);
            TestDataResponseDto response = new TestDataResponseDto(true,
                "Test data generated successfully for " + count + " books");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.warn("Failed to generate test data with payload {}: {}", payload, e.getMessage(), e);
            TestDataResponseDto errorResponse = new TestDataResponseDto(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/generate-loans")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TestDataResponseDto> generateLoanData(@RequestBody Map<String, Integer> payload) {
        try {
            int count = payload.getOrDefault("numLoans", 0);
            testDataService.generateLoanData(count);
            TestDataResponseDto response = new TestDataResponseDto(true,
                "Test data generated successfully for " + count + " loans");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.warn("Failed to generate loan test data with payload {}: {}", payload, e.getMessage(), e);
            TestDataResponseDto errorResponse = new TestDataResponseDto(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/generate-users")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TestDataResponseDto> generateUserData(@RequestBody Map<String, Integer> payload) {
        try {
            int count = payload.getOrDefault("numUsers", 0);
            testDataService.generateUserData(count);
            TestDataResponseDto response = new TestDataResponseDto(true,
                "Test data generated successfully for " + count + " users");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.warn("Failed to generate user test data with payload {}: {}", payload, e.getMessage(), e);
            TestDataResponseDto errorResponse = new TestDataResponseDto(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/delete-all")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> deleteAll() {
        try {
            testDataService.deleteTestData();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to delete all test data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/total-purge")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> totalPurge() {
        try {
            testDataService.totalPurge();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to perform total purge: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TestDataStatsDto> getStats() {
        try {
            TestDataStatsDto stats = new TestDataStatsDto(
                bookRepository.count(),
                authorRepository.count(),
                loanRepository.count(),
                userRepository.count()
            );
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.warn("Failed to retrieve test data stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
