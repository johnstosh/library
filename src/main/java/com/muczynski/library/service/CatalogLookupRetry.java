/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.exception.LibraryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

/**
 * Pause and retry for one public catalog. A bulk carousel looks titles up
 * back-to-back, and one title can search more than once. HTTP 403/429/502/503/504
 * and I/O timeouts wait, then try again. The wait is exponential from the base
 * and at least {@code Retry-After}, capped at {@link #MAX_BACKOFF_MS}. The last
 * failed try reserves that wait so the next title does not go out immediately.
 */
@Slf4j
public final class CatalogLookupRetry {

    /** One retry waits at most this long, even when Retry-After is longer. */
    public static final long MAX_BACKOFF_MS = 60_000L;

    private static final LongConsumer THREAD_SLEEP = millis -> {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LibraryException("Catalog lookup interrupted", ex);
        }
    };

    private final String catalog;
    private final int retries;
    private final long backoffMs;
    private final long requestDelayMs;
    private final LongConsumer pauser;
    private final Object paceLock = new Object();
    /** Earliest time the next outbound search may start. */
    private long nextAllowedAtMs;

    public CatalogLookupRetry(String catalog, int retries, long backoffMs, long requestDelayMs) {
        this(catalog, retries, backoffMs, requestDelayMs, THREAD_SLEEP);
    }

    public CatalogLookupRetry(String catalog, int retries, long backoffMs, long requestDelayMs,
                               LongConsumer pauser) {
        this.catalog = catalog == null || catalog.isBlank() ? "Catalog" : catalog;
        this.retries = Math.max(0, retries);
        this.backoffMs = Math.max(0, backoffMs);
        this.requestDelayMs = Math.max(0, requestDelayMs);
        this.pauser = pauser == null ? THREAD_SLEEP : pauser;
    }

    /**
     * Run one outbound call. {@code target} is only used in the backoff log.
     */
    public <T> T call(String target, Supplier<T> request) {
        int attempts = retries + 1;
        RuntimeException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            pace();
            try {
                return request.get();
            } catch (RestClientResponseException e) {
                last = e;
                if (!isRetryableStatus(e.getStatusCode().value()) || attempt == attempts) {
                    if (isRetryableStatus(e.getStatusCode().value())) {
                        delayNextRequest(backoffMs(attempt, backoffMs, e));
                    }
                    throw e;
                }
                long wait = backoffMs(attempt, backoffMs, e);
                log.warn("{} HTTP {} for {} (attempt {}/{}); backing off {} ms",
                        catalog, e.getStatusCode().value(), target, attempt, attempts, wait);
                delayNextRequest(wait);
            } catch (ResourceAccessException e) {
                last = e;
                if (attempt == attempts) {
                    delayNextRequest(backoffMs(attempt, backoffMs, null));
                    throw e;
                }
                long wait = backoffMs(attempt, backoffMs, null);
                log.warn("{} I/O for {} (attempt {}/{}): {}; backing off {} ms",
                        catalog, target, attempt, attempts, e.getMessage(), wait);
                delayNextRequest(wait);
            }
        }
        throw last != null ? last : new LibraryException(catalog + " lookup failed");
    }

    public static boolean isRetryableStatus(int statusCode) {
        return statusCode == 403 || statusCode == 429
                || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    /**
     * Wait before another try. Exponential from {@code baseMs} on the first
     * failure, doubled each further failure, and never shorter than Retry-After.
     * A zero base and a missing Retry-After wait 0, which stores the 429 and
     * sends the next title at once.
     */
    public static long backoffMs(int failedAttempt, long baseMs, RestClientResponseException exception) {
        long exponential = 0;
        if (baseMs > 0 && failedAttempt > 0) {
            exponential = baseMs;
            for (int i = 1; i < failedAttempt && exponential < MAX_BACKOFF_MS; i++) {
                exponential = Math.min(MAX_BACKOFF_MS, exponential * 2);
            }
        }
        return Math.max(exponential, retryAfterMs(exception));
    }

    public static long retryAfterMs(RestClientResponseException exception) {
        if (exception == null || exception.getResponseHeaders() == null) {
            return 0;
        }
        String raw = exception.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        raw = raw.trim();
        try {
            long seconds = Long.parseLong(raw);
            if (seconds <= 0) {
                return 0;
            }
            return Math.min(seconds * 1000L, MAX_BACKOFF_MS);
        } catch (NumberFormatException ignored) {
            try {
                ZonedDateTime when = ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME);
                long ms = Duration.between(Instant.now(), when.toInstant()).toMillis();
                if (ms <= 0) {
                    return 0;
                }
                return Math.min(ms, MAX_BACKOFF_MS);
            } catch (DateTimeParseException ex) {
                return 0;
            }
        }
    }

    private void pace() {
        long wait;
        synchronized (paceLock) {
            long now = System.currentTimeMillis();
            wait = Math.max(0, nextAllowedAtMs - now);
            long start = now + wait;
            nextAllowedAtMs = start + requestDelayMs;
        }
        sleep(wait);
    }

    private void delayNextRequest(long waitMs) {
        if (waitMs <= 0) {
            return;
        }
        synchronized (paceLock) {
            long until = System.currentTimeMillis() + waitMs;
            if (until > nextAllowedAtMs) {
                nextAllowedAtMs = until;
            }
        }
    }

    private void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        pauser.accept(ms);
    }
}
