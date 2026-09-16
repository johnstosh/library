/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

import com.muczynski.library.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final WebRequest request = mock(WebRequest.class);

    @BeforeEach
    void setUp() {
        when(request.getDescription(false)).thenReturn("uri=/api/acla-lookup/lookup/1");
    }

    @Test
    void uniqueConstraint_isDuplicateConflict() {
        SQLException sql = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"uk_book_title\"", "23505");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("could not execute statement", sql);

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("DUPLICATE_ENTITY", response.getBody().getError());
        assertEquals("Duplicate entry violates constraint: uk_book_title", response.getBody().getMessage());
    }

    @Test
    void valueTooLong_isBadRequestNotDuplicate() {
        SQLException sql = new SQLException(
                "ERROR: value too long for type character varying(255)", "22001");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("could not execute statement", sql);

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALUE_TOO_LONG", response.getBody().getError());
        assertEquals("A value exceeds the maximum allowed length", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().toLowerCase().contains("duplicate"));
    }
}
