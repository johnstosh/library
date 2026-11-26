/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Whether the book's main character is male or female.
 * Null means unknown, mixed, or more than one story.
 */
public enum MainCharacterSex {
    MALE,
    FEMALE;

    public String displayName() {
        return this == MALE ? "Male" : "Female";
    }

    /**
     * Blank is null. Unknown text is rejected.
     */
    @JsonCreator
    public static MainCharacterSex fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String token = value.trim();
        for (MainCharacterSex sex : values()) {
            if (sex.name().equalsIgnoreCase(token) || sex.displayName().equalsIgnoreCase(token)) {
                return sex;
            }
        }
        throw new IllegalArgumentException(
                "Main character must be male, female, or blank.");
    }
}
