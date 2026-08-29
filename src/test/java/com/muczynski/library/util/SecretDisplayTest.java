/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretDisplayTest {

    @Test
    void partial_masksLastFour() {
        assertEquals("...1234", SecretDisplay.partial("secret1234"));
        assertEquals("(not configured)", SecretDisplay.partial(""));
        assertEquals("(not configured)", SecretDisplay.partial(null));
        assertEquals("(too short)", SecretDisplay.partial("ab"));
    }

    @Test
    void isConfigured_requiresNonBlank() {
        assertTrue(SecretDisplay.isConfigured("abc"));
        assertFalse(SecretDisplay.isConfigured("  "));
        assertFalse(SecretDisplay.isConfigured(null));
    }
}
