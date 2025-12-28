/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.SearchResponseDto;
import com.muczynski.library.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private SearchService searchService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<SearchResponseDto> search(@RequestParam String query, @RequestParam int page, @RequestParam int size) {
        try {
            SearchResponseDto results = searchService.search(query, page, size);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.warn("Failed to perform search with query '{}', page {}, size {}: {}", query, page, size, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
