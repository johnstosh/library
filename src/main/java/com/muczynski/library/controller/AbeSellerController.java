/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.AbeSellerDto;
import com.muczynski.library.dto.AdditionalShippingUpdateDto;
import com.muczynski.library.dto.BookListingDto;
import com.muczynski.library.service.AbeSellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Librarian views of AbeBooks sellers captured during price lookup.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AbeSellerController {

    private final AbeSellerService sellerService;

    @GetMapping("/sellers")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<AbeSellerDto>> list() {
        return ResponseEntity.ok(sellerService.list());
    }

    @GetMapping("/sellers/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<AbeSellerDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(sellerService.get(id));
    }

    @PutMapping("/sellers/{id}/additional-shipping")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<AbeSellerDto> updateAdditionalShipping(@PathVariable Long id,
                                                                @RequestBody AdditionalShippingUpdateDto update) {
        return ResponseEntity.ok(sellerService.updateAdditionalShipping(id, update));
    }

    @PostMapping("/listings/by-book-ids")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<BookListingDto>> listingsForBooks(@RequestBody List<Long> bookIds) {
        return ResponseEntity.ok(sellerService.listingsForBooks(bookIds));
    }
}
