/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogLookupRetryTest {

    @Test
    void call_tooManyRequests_waitsForRetryAfterThenSucceeds() {
        List<Long> pauses = new ArrayList<>();
        CatalogLookupRetry retry = new CatalogLookupRetry("YDL", 3, 2_000, 0, pauses::add);
        AtomicInteger calls = new AtomicInteger();

        String body = retry.call("https://catalog.example/search", () -> {
            if (calls.incrementAndGet() == 1) {
                throw tooMany("30");
            }
            return "ok";
        });

        assertEquals("ok", body);
        assertEquals(2, calls.get());
        assertEquals(1, pauses.size());
        assertClose(30_000, pauses.get(0));
    }

    @Test
    void call_exhaustedRetries_reserveTheNextCall() {
        List<Long> pauses = new ArrayList<>();
        CatalogLookupRetry retry = new CatalogLookupRetry("EMU", 2, 2_000, 0, pauses::add);

        assertThrows(HttpClientErrorException.class,
                () -> retry.call("search", () -> {
                    throw tooMany(null);
                }));
        assertEquals(2, pauses.size());
        assertClose(2_000, pauses.get(0));
        assertClose(4_000, pauses.get(1));

        pauses.clear();
        assertEquals("ok", retry.call("next", () -> "ok"));
        assertEquals(1, pauses.size());
        assertClose(8_000, pauses.get(0));
    }

    @Test
    void call_badRequest_doesNotRetry() {
        List<Long> pauses = new ArrayList<>();
        CatalogLookupRetry retry = new CatalogLookupRetry("WCC", 3, 2_000, 1_000, pauses::add);

        HttpClientErrorException badRequest = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        assertThrows(HttpClientErrorException.class, () -> retry.call("search", () -> {
            throw badRequest;
        }));
        assertTrue(pauses.isEmpty());
    }

    @Test
    void call_ioTimeout_backsOff() {
        List<Long> pauses = new ArrayList<>();
        CatalogLookupRetry retry = new CatalogLookupRetry("ACLA", 1, 2_000, 0, pauses::add);
        AtomicInteger calls = new AtomicInteger();

        assertEquals("ok", retry.call("search", () -> {
            if (calls.incrementAndGet() == 1) {
                throw new ResourceAccessException("Read timed out");
            }
            return "ok";
        }));
        assertEquals(1, pauses.size());
        assertClose(2_000, pauses.get(0));
    }

    @Test
    void call_spacesTheNextSearch() {
        List<Long> pauses = new ArrayList<>();
        CatalogLookupRetry retry = new CatalogLookupRetry("LOC", 0, 0, 1_000, pauses::add);

        retry.call("one", () -> "a");
        retry.call("two", () -> "b");

        assertEquals(1, pauses.size());
        assertClose(1_000, pauses.get(0));
    }

    @Test
    void backoffMs_growsAndHonorsRetryAfter() {
        assertEquals(0, CatalogLookupRetry.backoffMs(1, 0, null));
        assertEquals(2_000, CatalogLookupRetry.backoffMs(1, 2_000, null));
        assertEquals(4_000, CatalogLookupRetry.backoffMs(2, 2_000, null));
        assertEquals(8_000, CatalogLookupRetry.backoffMs(3, 2_000, null));
        assertEquals(16_000, CatalogLookupRetry.backoffMs(4, 2_000, null));
        assertEquals(32_000, CatalogLookupRetry.backoffMs(5, 2_000, null));
        assertEquals(60_000, CatalogLookupRetry.backoffMs(6, 2_000, null));
        assertEquals(30_000, CatalogLookupRetry.backoffMs(1, 2_000, tooMany("30")));
        assertEquals(60_000, CatalogLookupRetry.backoffMs(1, 2_000, tooMany("600")));
        assertEquals(2_000, CatalogLookupRetry.backoffMs(1, 2_000, tooMany("0")));
        assertEquals(2_000, CatalogLookupRetry.backoffMs(1, 2_000, tooMany("nope")));

        String httpDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(20));
        long dated = CatalogLookupRetry.backoffMs(1, 1_000, tooMany(httpDate));
        assertTrue(dated >= 18_000 && dated <= 20_000, "Retry-After date wait was " + dated);

        assertTrue(CatalogLookupRetry.isRetryableStatus(429));
        assertTrue(CatalogLookupRetry.isRetryableStatus(503));
        assertFalse(CatalogLookupRetry.isRetryableStatus(400));
        assertFalse(CatalogLookupRetry.isRetryableStatus(404));
    }

    private static void assertClose(long expectedMs, long actualMs) {
        assertTrue(Math.abs(expectedMs - actualMs) <= 250,
                "expected pause near " + expectedMs + " ms but was " + actualMs);
    }

    private static HttpClientErrorException tooMany(String retryAfter) {
        HttpHeaders headers = new HttpHeaders();
        if (retryAfter != null) {
            headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
        }
        return HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers,
                new byte[0], StandardCharsets.UTF_8);
    }
}
