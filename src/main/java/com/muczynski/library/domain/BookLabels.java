/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import java.util.List;

/**
 * Canonical list of valid book labels used for categorization.
 * Labels are lowercase with only letters, numbers, and dashes.
 * Mirrors the label list used in AskGrok genre lookup prompts.
 */
public final class BookLabels {

    private BookLabels() {
        // Utility class — not instantiable
    }

    public static final List<String> ALL_LABELS = List.of(
            "fiction",
            "slice-of-life",
            "hagiography",
            "saint",
            "fantasy",
            "family",
            "childrens",
            "adult",
            "philosophy",
            "theology",
            "discernment",
            "talking-animals",
            "biography",
            "history",
            "prayer",
            "classic",
            "poetry",
            "science",
            "music",
            "mystery",
            "adventure",
            "romance",
            "humor"
    );
}
