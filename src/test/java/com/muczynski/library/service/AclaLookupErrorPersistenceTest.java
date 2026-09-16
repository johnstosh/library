/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.AclaLookupResultDto;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.util.LookupErrorMessages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Verifies that a long ACLA HTTP error body is stored in varchar(255) without
 * failing the flush (the bulk carousel previously reported this as a duplicate).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AclaLookupErrorPersistenceTest {

    @Autowired
    private AclaLookupService aclaLookupService;

    @Autowired
    private BookRepository bookRepository;

    @MockitoBean(name = "aclaRestTemplate")
    private RestTemplate aclaRestTemplate;

    @Test
    void longHttpErrorBody_isTruncatedAndPersisted() {
        Book book = new Book();
        book.setTitle("ACLA Overflow " + UUID.randomUUID());
        book.setStatus(BookStatus.ACTIVE);
        book.setDateAddedToLibrary(LocalDateTime.now());
        book = bookRepository.saveAndFlush(book);

        String restTemplateMessage = "403 Forbidden from https://gateway.bibliocommons.com/v2/libraries/acl/bibs/search: ["
                + "x".repeat(400) + "]";
        byte[] body = ("<!DOCTYPE html>" + "x".repeat(4000)).getBytes(StandardCharsets.UTF_8);
        HttpClientErrorException httpError = HttpClientErrorException.create(
                restTemplateMessage, HttpStatus.FORBIDDEN, "Forbidden",
                HttpHeaders.EMPTY, body, StandardCharsets.UTF_8);
        assertTrue(httpError.getMessage().length() > LookupErrorMessages.MAX_LENGTH);

        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class))).thenThrow(httpError);

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(book.getId());

        assertFalse(result.isSuccess());
        assertEquals("Error: HTTP 403 Forbidden", result.getErrorMessage());
        assertDoesNotThrow(() -> bookRepository.flush());

        Book reloaded = bookRepository.findById(book.getId()).orElseThrow();
        assertEquals("Error: HTTP 403 Forbidden", reloaded.getAclaLookupError());
        assertTrue(reloaded.getAclaLookupError().length() <= LookupErrorMessages.MAX_LENGTH);
    }

    @Test
    void longRuntimeMessage_isTruncatedAndPersisted() {
        Book book = new Book();
        book.setTitle("ACLA Long Message " + UUID.randomUUID());
        book.setStatus(BookStatus.ACTIVE);
        book.setDateAddedToLibrary(LocalDateTime.now());
        book = bookRepository.saveAndFlush(book);

        when(aclaRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(new RuntimeException("z".repeat(1000)));

        AclaLookupResultDto result = aclaLookupService.lookupAndUpdateBook(book.getId());

        assertFalse(result.isSuccess());
        assertEquals(LookupErrorMessages.MAX_LENGTH, result.getErrorMessage().length());
        assertDoesNotThrow(() -> bookRepository.flush());

        Book reloaded = bookRepository.findById(book.getId()).orElseThrow();
        assertEquals(LookupErrorMessages.MAX_LENGTH, reloaded.getAclaLookupError().length());
        assertTrue(reloaded.getAclaLookupError().endsWith("..."));
    }
}
