/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

/**
 * Masks secrets for API responses so librarians can confirm a value is stored
 * without seeing the full secret.
 */
public final class SecretDisplay {

    private SecretDisplay() {
    }

    public static boolean isConfigured(String secret) {
        return secret != null && !secret.trim().isEmpty();
    }

    /**
     * Last four characters prefixed with "...", matching the Google secret display.
     */
    public static String partial(String secret) {
        if (!isConfigured(secret)) {
            return "(not configured)";
        }
        String trimmed = secret.trim();
        if (trimmed.length() < 4) {
            return "(too short)";
        }
        return "..." + trimmed.substring(trimmed.length() - 4);
    }
}
