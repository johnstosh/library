/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.BookPriceDto;
import com.muczynski.library.dto.BookPriceLookupResultDto;
import com.muczynski.library.service.BookPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Librarian endpoints for AbeBooks used-book prices.
 */
@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
@Slf4j
public class BookPriceController {

    private final BookPriceService bookPriceService;

    @GetMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<BookPriceDto>> listPrices() {
        return ResponseEntity.ok(bookPriceService.listAll());
    }

    @PostMapping("/lookup/{bookId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<BookPriceLookupResultDto> lookupBook(@PathVariable Long bookId) {
        log.info("Looking up AbeBooks prices for book ID: {}", bookId);
        return ResponseEntity.ok(bookPriceService.lookupAndUpdateBook(bookId));
    }
}
