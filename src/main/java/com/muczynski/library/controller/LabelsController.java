/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.BookLocStatusDto;
import com.muczynski.library.service.LabelsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for generating book labels
 */
@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
public class LabelsController {

    private final LabelsService labelsService;

    /**
     * Get books from most recent day with LOC numbers, sorted by date added
     */
    @GetMapping("/books")
    public ResponseEntity<List<BookLocStatusDto>> getBooksForLabels() {
        List<BookLocStatusDto> books = labelsService.getBooksForLabels();
        return ResponseEntity.ok(books);
    }

    /**
     * Get all books with LOC numbers, sorted by date added
     */
    @GetMapping("/books/all")
    public ResponseEntity<List<BookLocStatusDto>> getAllBooksForLabels() {
        List<BookLocStatusDto> books = labelsService.getAllBooksForLabels();
        return ResponseEntity.ok(books);
    }

    /**
     * Generate labels PDF for specified books
     */
    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateLabelsPdf(@RequestParam List<Long> bookIds) {
        byte[] pdfBytes = labelsService.generateLabelsPdf(bookIds);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "book-labels.pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
