/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Book status filter chips. Selected values OR together, then AND with other filters.
 * Active is split into in-library (physical collection), electronic-resource,
 * and without-loc (no call number, excluding electronic resources).
 */
public enum BookStatusFilter {
    IN_LIBRARY("in-library"),
    ELECTRONIC_RESOURCE("electronic-resource"),
    WITHOUT_LOC("without-loc"),
    LOST("lost"),
    WITHDRAWN("withdrawn"),
    ON_ORDER("on-order"),
    REQUESTED("requested");

    private final String key;

    BookStatusFilter(String key) {
        this.key = key;
    }

    /** Stable lowercase API and URL key. */
    public String getKey() {
        return key;
    }

    /**
     * Parse a comma-separated filter value into known status-filter constants.
     * Unknown tokens are ignored; blank or null returns an empty list.
     */
    public static List<BookStatusFilter> parseFilterValues(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<BookStatusFilter> selected = new ArrayList<>();
        Set<BookStatusFilter> seen = EnumSet.noneOf(BookStatusFilter.class);
        for (String part : raw.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            for (BookStatusFilter value : values()) {
                if (value.key.equalsIgnoreCase(token) || value.name().equalsIgnoreCase(token)) {
                    if (seen.add(value)) {
                        selected.add(value);
                    }
                    break;
                }
            }
        }
        return selected;
    }
}
