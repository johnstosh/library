// (c) Copyright 2025 by Muczynski
package com.muczynski.library.controller;

import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.service.TestDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    @PostMapping("/generate")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> generateTestData(@RequestBody Map<String, Integer> payload) {
        try {
            int count = payload.getOrDefault("numBooks", 0);
            testDataService.generateTestData(count);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data generated successfully for " + count + " books");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.debug("Failed to generate test data with payload {}: {}", payload, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/generate-loans")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> generateLoanData(@RequestBody Map<String, Integer> payload) {
        try {
            int count = payload.getOrDefault("numLoans", 0);
            testDataService.generateLoanData(count);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data generated successfully for " + count + " loans");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.debug("Failed to generate loan test data with payload {}: {}", payload, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
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
            logger.debug("Failed to delete all test data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/total-purge")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> totalPurge() {
        try {
            testDataService.totalPurge();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.debug("Failed to perform total purge: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Failed to purge database: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Long>> getStats() {
        try {
            Map<String, Long> stats = new HashMap<>();
            stats.put("books", bookRepository.count());
            stats.put("authors", authorRepository.count());
            stats.put("loans", loanRepository.count());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.debug("Failed to retrieve test data stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
