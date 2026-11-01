/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Primary and alternate titles of one work. An alternate title is another
 * wording of the same book, such as the original language or another English
 * title. It is not a different work.
 */
public final class BookTitles {

    private BookTitles() {
    }

    /**
     * Trim, split embedded newlines, drop blanks, drop a case-insensitive
     * repeat of the primary title, and drop later case-insensitive duplicates.
     * Comparison ignores a trailing {@code ", c. N"} suffix. A kept title is
     * stored as typed. Returns null when nothing remains.
     */
    public static List<String> normalize(String primaryTitle, List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String primaryKey = compareKey(primaryTitle);
        List<String> result = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (String value : raw) {
            if (value == null) {
                continue;
            }
            for (String line : value.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String key = compareKey(trimmed);
                if (key.isEmpty()) {
                    continue;
                }
                if (!primaryKey.isEmpty() && key.equals(primaryKey)) {
                    continue;
                }
                if (keys.contains(key)) {
                    continue;
                }
                keys.add(key);
                result.add(trimmed);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Folded form used to decide that two wordings are the same title.
     * A trailing copy-number suffix is ignored.
     */
    static String compareKey(String title) {
        String stripped = Book.stripCopySuffix(title);
        if (stripped == null || stripped.isBlank()) {
            return "";
        }
        return stripped.trim().toLowerCase(Locale.ROOT);
    }
}
