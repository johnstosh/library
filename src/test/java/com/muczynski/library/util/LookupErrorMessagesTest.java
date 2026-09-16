/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LookupErrorMessagesTest {

    @Test
    void httpError_usesStatusWithoutResponseBody() {
        String restTemplateMessage = "403 Forbidden from https://gateway.bibliocommons.com/v2/libraries/acl/bibs/search: ["
                + "x".repeat(400) + "]";
        byte[] body = ("<!DOCTYPE html>" + "x".repeat(4000)).getBytes(StandardCharsets.UTF_8);
        HttpClientErrorException exception = HttpClientErrorException.create(
                restTemplateMessage, HttpStatus.FORBIDDEN, "Forbidden",
                HttpHeaders.EMPTY, body, StandardCharsets.UTF_8);

        assertTrue(exception.getMessage().length() > LookupErrorMessages.MAX_LENGTH);
        assertEquals("Error: HTTP 403 Forbidden", LookupErrorMessages.fromException(exception));
    }

    @Test
    void longRuntimeMessage_isTruncatedTo255() {
        String result = LookupErrorMessages.fromException(new RuntimeException("y".repeat(1000)));

        assertEquals(LookupErrorMessages.MAX_LENGTH, result.length());
        assertTrue(result.startsWith("Error: "));
        assertTrue(result.endsWith("..."));
    }

    @Test
    void shortRuntimeMessage_isPrefixed() {
        assertEquals("Error: connection timed out",
                LookupErrorMessages.fromException(new RuntimeException("connection timed out")));
    }

    @Test
    void nullMessage_usesExceptionClassName() {
        assertEquals("Error: RuntimeException",
                LookupErrorMessages.fromException(new RuntimeException()));
    }
}
