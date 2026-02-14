/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.service.LabelsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
     * Generate labels PDF for specified books (librarian only)
     */
    @GetMapping("/generate")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<byte[]> generateLabelsPdf(@RequestParam List<Long> bookIds) {
        byte[] pdfBytes = labelsService.generateLabelsPdf(bookIds);

        String datePrefix = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String filename = String.format("%s-book-labels-%d-books.pdf", datePrefix, bookIds.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
