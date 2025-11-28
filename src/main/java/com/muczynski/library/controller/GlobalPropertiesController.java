/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for exposing application properties to the frontend.
 * These are read-only properties from application.properties files.
 */
@RestController
@RequestMapping("/api/global-properties")
public class GlobalPropertiesController {

    private static final Logger logger = LoggerFactory.getLogger(GlobalPropertiesController.class);

    @Value("${app.show-test-data-page:true}")
    private boolean showTestDataPage;

    /**
     * Check if test data page should be shown
     * Public endpoint - no authentication required
     */
    @GetMapping("/test-data-page-visibility")
    public ResponseEntity<TestDataPageVisibilityDto> getTestDataPageVisibility() {
        logger.info("Checking test data page visibility property");
        TestDataPageVisibilityDto visibility = new TestDataPageVisibilityDto();
        visibility.setShowTestDataPage(showTestDataPage);
        return ResponseEntity.ok(visibility);
    }

    /**
     * DTO for test data page visibility response
     */
    public static class TestDataPageVisibilityDto {
        private boolean showTestDataPage;

        public boolean isShowTestDataPage() {
            return showTestDataPage;
        }

        public void setShowTestDataPage(boolean showTestDataPage) {
            this.showTestDataPage = showTestDataPage;
        }
    }
}
