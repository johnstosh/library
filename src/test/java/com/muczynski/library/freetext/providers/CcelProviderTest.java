/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.muczynski.library.freetext.FreeTextLookupResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CcelProviderTest {

    private static final Logger log = LoggerFactory.getLogger(CcelProviderTest.class);

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CcelProvider provider;

    @Test
    void getProviderName_returnsCorrectName() {
        assertEquals("Christian Classics Ethereal Library", provider.getProviderName());
    }

    @Test
    void getPriority_returns35() {
        assertEquals(35, provider.getPriority());
    }

    @Test
    void search_handlesEmptyResponse() {
        when(restTemplate.getForObject(anyString(), any())).thenReturn(null);

        FreeTextLookupResult result = provider.search("Some Book", "Some Author");

        assertFalse(result.isFound());
        assertEquals("Christian Classics Ethereal Library", result.getProviderName());
    }

    @Test
    void search_handlesApiException() {
        when(restTemplate.getForObject(anyString(), any()))
                .thenThrow(new RuntimeException("Connection failed"));

        FreeTextLookupResult result = provider.search("Test Book", null);

        assertFalse(result.isFound());
        assertTrue(result.getErrorMessage().contains("Search error"));
    }

    /**
     * Live integration test - actually calls CCEL API.
     * Run manually to debug connectivity/timeout issues.
     */
    @Test
    @Tag("manual")
    void search_liveApiTest() {
        // Create a real RestTemplate with timeouts and logging
        RestTemplate liveRestTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000); // 60 seconds
        factory.setReadTimeout(60000); // 60 seconds
        liveRestTemplate.setRequestFactory(factory);

        CcelProvider liveProvider = new CcelProvider();
        ReflectionTestUtils.setField(liveProvider, "restTemplate", liveRestTemplate);

        String testTitle = "City of God";
        String testAuthor = "Augustine";

        log.info("=== CCEL Provider Live Test ===");
        log.info("Searching for: '{}' by '{}'", testTitle, testAuthor);
        log.info("Connect timeout: 60s, Read timeout: 60s");

        long startTime = System.currentTimeMillis();
        log.info("Starting search at: {}", java.time.Instant.now());

        try {
            FreeTextLookupResult result = liveProvider.search(testTitle, testAuthor);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Search completed in {} ms", elapsed);
            log.info("Result found: {}", result.isFound());
            if (result.isFound()) {
                log.info("URL: {}", result.getUrl());
            } else {
                log.info("Error message: {}", result.getErrorMessage());
            }

            // Don't assert on result - this is for debugging
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Search failed after {} ms", elapsed);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());

            Throwable cause = e.getCause();
            while (cause != null) {
                log.error("Caused by: {} - {}", cause.getClass().getName(), cause.getMessage());
                cause = cause.getCause();
            }

            fail("Live API test failed: " + e.getMessage());
        }
    }

    /**
     * Test with direct URL fetch to isolate the issue.
     */
    @Test
    @Tag("manual")
    void directUrlTest() {
        RestTemplate liveRestTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(60000);
        liveRestTemplate.setRequestFactory(factory);

        String testUrl = "https://www.ccel.org/search?qu=city+god+augustine";

        log.info("=== Direct URL Test ===");
        log.info("Fetching: {}", testUrl);

        long startTime = System.currentTimeMillis();

        try {
            String response = liveRestTemplate.getForObject(testUrl, String.class);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Fetch completed in {} ms", elapsed);
            log.info("Response length: {} chars", response != null ? response.length() : 0);

            if (response != null && response.length() > 500) {
                log.info("Response preview (first 500 chars):\n{}", response.substring(0, 500));
            } else {
                log.info("Response:\n{}", response);
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Fetch failed after {} ms", elapsed);
            log.error("Exception: {} - {}", e.getClass().getName(), e.getMessage());

            Throwable cause = e.getCause();
            while (cause != null) {
                log.error("Caused by: {} - {}", cause.getClass().getName(), cause.getMessage());
                cause = cause.getCause();
            }

            fail("Direct URL test failed: " + e.getMessage());
        }
    }
}
