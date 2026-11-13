/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Username of the person making the current request. Null when the call has
 * no logged-in user, which is the case in unit tests that call services directly.
 */
public final class CurrentActor {

    private CurrentActor() {
    }

    public static String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails details) {
            String username = details.getUsername();
            return username == null || username.isBlank() ? null : username.trim();
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name.trim();
    }
}
