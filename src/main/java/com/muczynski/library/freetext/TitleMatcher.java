/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

/**
 * Utility class for matching book titles with various normalization strategies.
 * Handles case differences, subtitles, articles, and punctuation variations.
 */
public final class TitleMatcher {

    private TitleMatcher() {
        // Utility class
    }

    /**
     * Check if two titles match, accounting for:
     * - Case differences
     * - Subtitle variations (text after colon)
     * - Articles (The, A, An)
     * - Punctuation differences
     *
     * @param candidate the title found from the provider
     * @param target    the title we're searching for
     * @return true if the titles match
     */
    public static boolean titleMatches(String candidate, String target) {
        if (candidate == null || target == null) {
            return false;
        }

        String normalizedCandidate = normalize(candidate);
        String normalizedTarget = normalize(target);

        // Exact match after normalization
        if (normalizedCandidate.equals(normalizedTarget)) {
            return true;
        }

        // Main title match (before colon)
        String candidateMain = getMainTitle(normalizedCandidate);
        String targetMain = getMainTitle(normalizedTarget);

        if (candidateMain.equals(targetMain)) {
            return true;
        }

        // Contains match for longer titles (to handle slight variations)
        if (candidateMain.length() > 15 && targetMain.length() > 15) {
            return candidateMain.contains(targetMain) || targetMain.contains(candidateMain);
        }

        // Word-based matching for shorter titles
        return wordsMatch(candidateMain, targetMain);
    }

    /**
     * Normalize a title for comparison:
     * - Convert to lowercase
     * - Remove leading articles (the, a, an)
     * - Remove punctuation except spaces
     * - Collapse multiple spaces
     */
    private static String normalize(String title) {
        return title.toLowerCase()
                .replaceAll("^(the|a|an)\\s+", "")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Get the main title (text before colon or the full title if no colon).
     */
    private static String getMainTitle(String title) {
        int colonIndex = title.indexOf(':');
        return colonIndex > 0 ? title.substring(0, colonIndex).trim() : title;
    }

    /**
     * Check if the significant words in two titles match.
     * Ignores common filler words.
     */
    private static boolean wordsMatch(String title1, String title2) {
        String[] words1 = title1.split("\\s+");
        String[] words2 = title2.split("\\s+");

        // Count matching significant words
        int matchCount = 0;
        int significantWords = 0;

        for (String word : words1) {
            if (word.length() > 2) { // Skip very short words
                significantWords++;
                for (String word2 : words2) {
                    if (word.equals(word2)) {
                        matchCount++;
                        break;
                    }
                }
            }
        }

        // Consider a match if at least 70% of significant words match
        return significantWords > 0 && (double) matchCount / significantWords >= 0.7;
    }

    /**
     * Check if an author name matches, accounting for:
     * - Last name only matches
     * - First Last vs Last, First formats
     * - Case differences
     * - Multi-part names where 2nd-to-last name may be the surname
     *   (e.g., "Gabriel García Márquez" where "García" is the paternal surname)
     *
     * @param candidate the author name found from the provider
     * @param target    the author name we're searching for
     * @return true if the authors match
     */
    public static boolean authorMatches(String candidate, String target) {
        if (candidate == null || target == null) {
            return false;
        }

        String normalizedCandidate = normalizeAuthor(candidate);
        String normalizedTarget = normalizeAuthor(target);

        // Exact match
        if (normalizedCandidate.equals(normalizedTarget)) {
            return true;
        }

        // Check if last names match
        String candidateLastName = getLastName(normalizedCandidate);
        String targetLastName = getLastName(normalizedTarget);

        if (candidateLastName.equals(targetLastName)) {
            return true;
        }

        // Check if target last name matches any of the last few names in candidate
        // This handles cases like "Gabriel García Márquez" vs "García"
        if (lastNameInFinalNames(normalizedCandidate, targetLastName)) {
            return true;
        }

        // Check the reverse: candidate last name in target's final names
        // This handles cases like "García" vs "Gabriel García Márquez"
        return lastNameInFinalNames(normalizedTarget, candidateLastName);
    }

    /**
     * Check if a last name appears in the final 2-3 names of a full name.
     * Handles multi-part surnames common in Spanish, Portuguese, etc.
     *
     * @param fullName the full author name (normalized)
     * @param lastName the last name to search for
     * @return true if lastName is found in the final names
     */
    private static boolean lastNameInFinalNames(String fullName, String lastName) {
        String[] parts = fullName.split("\\s+");
        if (parts.length <= 1) {
            return false;
        }

        // Check the last 3 names (or fewer if the name is shorter)
        int startIndex = Math.max(0, parts.length - 3);
        for (int i = startIndex; i < parts.length; i++) {
            if (parts[i].equals(lastName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalize an author name for comparison.
     */
    private static String normalizeAuthor(String author) {
        return author.toLowerCase()
                .replaceAll("[^a-z\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Extract the last name from an author string.
     * Handles both "First Last" and "Last, First" formats.
     */
    private static String getLastName(String author) {
        // Handle "Last, First" format
        if (author.contains(",")) {
            return author.split(",")[0].trim();
        }
        // Handle "First Last" format
        String[] parts = author.split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : author;
    }
}
