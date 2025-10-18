package com.muczynski.library.controller;

import com.muczynski.library.service.TestDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test-data")
public class TestDataController {

    @Autowired
    private TestDataService testDataService;

    @PostMapping("/generate")
    public ResponseEntity<Void> generateTestData(@RequestBody Map<String, Integer> payload) {
        int count = payload.getOrDefault("count", 0);
        testDataService.generateTestData(count);
        return ResponseEntity.ok().build();
    }
}