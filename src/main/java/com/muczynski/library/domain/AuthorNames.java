/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Canonical and alternate author names used when searching the catalog
 * and when looking a book up in a library or free-text source.
 * An alternate name is another identity or catalog form of the same person,
 * such as a pen name, a saint's name, or a Latin name.
 */
public final class AuthorNames {

    private AuthorNames() {
    }

    /**
     * Trim, drop blanks, drop a case-insensitive repeat of the canonical name,
     * and drop case-insensitive duplicates. A single list entry may contain
     * several names separated by newlines. Returns null when nothing remains.
     */
    public static List<String> normalize(String canonicalName, List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String canonical = canonicalName == null ? "" : canonicalName.trim();
        List<String> result = new ArrayList<>();
        for (String value : raw) {
            if (value == null) {
                continue;
            }
            for (String line : value.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (!canonical.isEmpty() && trimmed.equalsIgnoreCase(canonical)) {
                    continue;
                }
                if (containsIgnoreCase(result, trimmed)) {
                    continue;
                }
                result.add(trimmed);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Names to send to a lookup, canonical first. Always returns at least one
     * entry; that entry is null when the book has no usable author name.
     */
    public static List<String> lookupNames(Author author) {
        if (author == null) {
            return Collections.singletonList(null);
        }
        List<String> names = new ArrayList<>();
        if (author.getName() != null && !author.getName().isBlank()) {
            names.add(author.getName().trim());
        }
        if (author.getAlternateNames() != null) {
            for (String alternate : author.getAlternateNames()) {
                if (alternate == null || alternate.isBlank()) {
                    continue;
                }
                String trimmed = alternate.trim();
                if (!containsIgnoreCase(names, trimmed)) {
                    names.add(trimmed);
                }
            }
        }
        if (names.isEmpty()) {
            return Collections.singletonList(null);
        }
        return names;
    }

    /**
     * Distinct final tokens of {@link #lookupNames(Author)}, in the same order.
     * Library catalogs match on that token. A null entry means there is no
     * author name to require.
     */
    public static List<String> lookupLastNames(Author author) {
        List<String> lastNames = new ArrayList<>();
        for (String name : lookupNames(author)) {
            String token = lastToken(name);
            if (token == null || containsIgnoreCase(lastNames, token)) {
                continue;
            }
            lastNames.add(token);
        }
        if (lastNames.isEmpty()) {
            return Collections.singletonList(null);
        }
        return lastNames;
    }

    public static String lastToken(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String[] parts = name.trim().split("\\s+");
        return parts.length == 0 ? null : parts[parts.length - 1];
    }

    private static boolean containsIgnoreCase(List<String> names, String candidate) {
        for (String name : names) {
            if (name != null && name.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }
}
