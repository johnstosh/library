package com.muczynski.library.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test-data")
public class TestDataController {

    @PostMapping("/generate")
    public ResponseEntity<Void> generateTestData(@RequestBody Map<String, Integer> payload) {
        // Later, we will implement the logic to generate test data.
        return ResponseEntity.ok().build();
    }
}