/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneNumbersTest {

    @Test
    void acceptsCommonFormats() {
        assertTrue(PhoneNumbers.isValid("5551234567"));
        assertTrue(PhoneNumbers.isValid("(555) 123-4567"));
        assertTrue(PhoneNumbers.isValid("+1 555 123 4567"));
    }

    @Test
    void rejectsBlankLettersAndTooFewDigits() {
        assertFalse(PhoneNumbers.isValid(null));
        assertFalse(PhoneNumbers.isValid(""));
        assertFalse(PhoneNumbers.isValid("123"));
        assertFalse(PhoneNumbers.isValid("call-me"));
    }
}
