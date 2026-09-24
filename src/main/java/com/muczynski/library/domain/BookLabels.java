/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * Maps common incorrect plural forms, spelling variants, case variants,
     * whitespace/underscore variants to canonical labels.
     * Used by maintenance cleanup to normalize tags.
     */
    private static final Map<String, String> PLURAL_AND_VARIANT_MAP = Map.ofEntries(
            // Plural mismatches
            Map.entry("histories", "history"),
            Map.entry("biographies", "biography"),
            Map.entry("fantasies", "fantasy"),
            Map.entry("mysteries", "mystery"),
            Map.entry("romances", "romance"),
            Map.entry("adventures", "adventure"),
            Map.entry("classics", "classic"),
            Map.entry("prayers", "prayer"),
            Map.entry("sciences", "science"),
            Map.entry("philosophies", "philosophy"),
            Map.entry("theologies", "theology"),
            Map.entry("fictions", "fiction"),
            Map.entry("saints", "saint"),
            Map.entry("adults", "adult"),
            Map.entry("families", "family"),
            Map.entry("humors", "humor"),
            Map.entry("humour", "humor"),
            Map.entry("poems", "poetry"),
            Map.entry("poetries", "poetry"),
            Map.entry("musics", "music"),
            // Childrens variants
            Map.entry("children", "childrens"),
            Map.entry("childrens'", "childrens"),
            Map.entry("children's", "childrens"),
            // Slice of life variants
            Map.entry("slice of life", "slice-of-life"),
            Map.entry("slice_of_life", "slice-of-life"),
            // Talking animals
            Map.entry("talking animals", "talking-animals"),
            // Case and separator variants of canonicals (normalized in method)
            Map.entry("sliceoflife", "slice-of-life"),
            Map.entry("talkinganimals", "talking-animals")
    );

    /**
     * Set of all canonical labels for fast lookup.
     */
    private static final Set<String> CANONICAL_SET = Set.copyOf(ALL_LABELS);

    /**
     * Normalizes a tag: trims, lowercases, and applies known plural/variant mappings.
     * Returns the canonical form if it maps to one, or the normalized input.
     */
    public static String normalizeTag(String tag) {
        if (tag == null) return null;
        String normalized = tag.trim().toLowerCase();
        // Replace separators and remove apostrophes for variant matching
        normalized = normalized.replace('_', '-')
                .replace(' ', '-')
                .replace("'", "");
        return PLURAL_AND_VARIANT_MAP.getOrDefault(normalized, normalized);
    }

    /**
     * Returns true if the normalized tag is a valid canonical label.
     */
    public static boolean isValidLabel(String tag) {
        if (tag == null) return false;
        String normalized = normalizeTag(tag);
        return CANONICAL_SET.contains(normalized);
    }

    /**
     * Returns the canonical label for a tag, or null if it should be removed.
     */
    public static String getCanonicalLabel(String tag) {
        if (tag == null) return null;
        String normalized = normalizeTag(tag);
        return CANONICAL_SET.contains(normalized) ? normalized : null;
    }

    /**
     * Returns a list of canonical labels from a book's tagsList, removing
     * duplicates while preserving first-occurrence order.
     */
    public static List<String> cleanupTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String> cleaned = new ArrayList<>();

        for (String tag : tags) {
            String canonical = getCanonicalLabel(tag);
            if (canonical != null && seen.add(canonical)) {
                cleaned.add(canonical);
            }
        }
        return cleaned;
    }
}
