package com.muczynski.library.controller;

import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.service.TestDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test-data")
public class TestDataController {

    @Autowired
    private TestDataService testDataService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private LoanRepository loanRepository;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTestData(@RequestBody Map<String, Integer> payload) {
        int count = payload.getOrDefault("numBooks", 0);
        testDataService.generateTestData(count);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Test data generated successfully for " + count + " books");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-loans")
    public ResponseEntity<Map<String, Object>> generateLoanData(@RequestBody Map<String, Integer> payload) {
        int count = payload.getOrDefault("numLoans", 0);
        testDataService.generateLoanData(count);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Test data generated successfully for " + count + " loans");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<Void> deleteAll() {
        testDataService.deleteTestData();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("books", bookRepository.count());
        stats.put("authors", authorRepository.count());
        stats.put("loans", loanRepository.count());
        return ResponseEntity.ok(stats);
    }
}
