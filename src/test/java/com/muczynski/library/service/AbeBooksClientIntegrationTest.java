/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.exception.AbeBooksRateLimitedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hits the real AbeBooks SearchResults page for a well-known title so rate-limit
 * detection and parsing can be seen in the logs (GitHub issue 334).
 */
@SpringBootTest(properties = {
        "abebooks.request-delay-ms=1000",
        "abebooks.tenth-request-delay-ms=2000",
        "abebooks.rate-limit-retries=1",
        "abebooks.rate-limit-backoff-ms=2000"
})
@ActiveProfiles("test")
class AbeBooksClientIntegrationTest {

    @Autowired
    private AbeBooksClient abeBooksClient;

    @Test
    void realAbeBooksSearch_prideAndPrejudice() {
        try {
            AbeBooksCoverListings found = abeBooksClient.findCheapestGoodOrBetter(
                    "Pride and Prejudice", "Jane Austen");
            System.out.println("AbeBooks hardcover=" + found.getHardcover());
            System.out.println("AbeBooks softcover=" + found.getSoftcover());
            assertTrue(found.getHardcover() != null || found.getSoftcover() != null,
                    "expected at least one listing for Pride and Prejudice");
        } catch (AbeBooksRateLimitedException ex) {
            System.out.println("AbeBooks rate limited (detected correctly): " + ex.getMessage());
            assertEquals(AbeBooksRateLimitedException.MESSAGE, ex.getMessage());
        }
    }
}
