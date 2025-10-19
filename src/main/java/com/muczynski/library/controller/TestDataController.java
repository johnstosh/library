package com.muczynski.library.controller;

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

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTestData(@RequestBody Map<String, Integer> payload) {
        int count = payload.getOrDefault("numBooks", 0);
        testDataService.generateTestData(count);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Test data generated successfully for " + count + " books");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<Void> deleteAll() {
        testDataService.deleteTestData();
        return ResponseEntity.ok().build();
    }
}
