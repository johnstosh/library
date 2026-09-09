/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reading difficulty level for books. Used to help patrons select appropriate titles.
 * Patron-facing short labels are shown in UI; enum values are stored in DB/JSON.
 * unset is legacy default for existing books (migration default).
 * New books should be assigned a specific value.
 */
public enum ReadingDifficulty {
    CHILDREN("Children", "for kids / read-aloud"),
    ACCESSIBLE("Accessible", "clear for teens & adults"),
    MODERATE("Moderate", "takes patience; notes help"),
    DEMANDING("Demanding", "serious study; guide recommended"),
    ADVANCED("Advanced", "steep; best with direction"),
    UNSET("—", "not yet reviewed");

    private final String shortLabel;
    private final String description;

    ReadingDifficulty(String shortLabel, String description) {
        this.shortLabel = shortLabel;
        this.description = description;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public String getDescription() {
        return description;
    }

    /** Stable lowercase API and database key. */
    @JsonValue
    public String getKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Returns the enum for the given string value (case-insensitive), or UNSET if not found or null.
     */
    @JsonCreator
    public static ReadingDifficulty fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNSET;
        }
        for (ReadingDifficulty rd : values()) {
            if (rd.name().equalsIgnoreCase(value)) {
                return rd;
            }
        }
        return UNSET;
    }

    /**
     * Parse a comma-separated filter value into known enum constants.
     * Unknown tokens are ignored; blank or null returns an empty list.
     */
    public static List<ReadingDifficulty> parseFilterValues(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<ReadingDifficulty> selected = new ArrayList<>();
        Set<ReadingDifficulty> seen = EnumSet.noneOf(ReadingDifficulty.class);
        for (String part : raw.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            for (ReadingDifficulty value : values()) {
                if (value.getKey().equalsIgnoreCase(token) || value.name().equalsIgnoreCase(token)) {
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
