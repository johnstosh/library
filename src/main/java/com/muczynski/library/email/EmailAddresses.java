/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Shared email-address parsing and light validation used by settings and senders.
 */
public final class EmailAddresses {

    private static final Pattern LOOSE_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private EmailAddresses() {
    }

    public static boolean isValid(String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        return !trimmed.isEmpty() && LOOSE_EMAIL.matcher(trimmed).matches();
    }

    /**
     * Split a librarian-typed recipient list on commas, semicolons, or whitespace
     * and keep unique valid addresses in order.
     */
    public static List<String> parseRecipientList(String raw) {
        Set<String> unique = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        for (String part : raw.split("[,;\\s]+")) {
            String candidate = part.trim();
            if (isValid(candidate)) {
                unique.add(candidate);
            }
        }
        return new ArrayList<>(unique);
    }

    public static List<String> mergeUnique(List<String> first, List<String> second) {
        Set<String> unique = new LinkedHashSet<>();
        addNormalized(unique, first);
        addNormalized(unique, second);
        return new ArrayList<>(unique);
    }

    private static void addNormalized(Set<String> unique, List<String> emails) {
        if (emails == null) {
            return;
        }
        Set<String> seenLower = new LinkedHashSet<>();
        for (String existing : unique) {
            seenLower.add(existing.toLowerCase(Locale.ROOT));
        }
        for (String email : emails) {
            if (!isValid(email)) {
                continue;
            }
            String trimmed = email.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (seenLower.add(lower)) {
                unique.add(trimmed);
            }
        }
    }
}
