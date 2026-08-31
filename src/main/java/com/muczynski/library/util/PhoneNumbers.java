/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import java.util.regex.Pattern;

/**
 * Light validation for optional patron phone numbers.
 */
public final class PhoneNumbers {

    public static final int MAX_LENGTH = 32;

    private static final Pattern ALLOWED = Pattern.compile("^[+0-9().\\-\\s]+$");

    private PhoneNumbers() {
    }

    public static boolean isValid(String phone) {
        if (phone == null) {
            return false;
        }
        String trimmed = phone.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            return false;
        }
        if (!ALLOWED.matcher(trimmed).matches()) {
            return false;
        }
        long digits = trimmed.chars().filter(Character::isDigit).count();
        return digits >= 7 && digits <= 15;
    }
}
