// (c) Copyright 2025 by Muczynski
package com.muczynski.library.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to handle React Router client-side routing.
 * All non-API routes should serve the React index.html to allow
 * client-side routing to work properly.
 */
@Controller
public class SpaController {

    /**
     * Forward all non-API routes to index.html for React Router.
     * This allows React to handle client-side routing for paths like:
     * - /books
     * - /authors
     * - /loans
     * - etc.
     *
     * API routes (/api/**) are not affected by this mapping.
     */
    @GetMapping(value = {
            "/",
            "/books",
            "/books/**",
            "/authors",
            "/authors/**",
            "/libraries",
            "/libraries/**",
            "/branches",
            "/branches/**",
            "/data-management",
            "/loans",
            "/loans/**",
            "/users",
            "/users/**",
            "/search",
            "/search/**",
            "/my-card",
            "/applications",
            "/applications/**",
            "/labels",
            "/labels/**",
            "/books-from-feed",
            "/books-from-feed/**",
            "/settings",
            "/settings/**",
            "/global-settings",
            "/global-settings/**",
            "/test-data",
            "/test-data/**",
            "/login",
            "/apply",
            "/photos",
            "/photos/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
