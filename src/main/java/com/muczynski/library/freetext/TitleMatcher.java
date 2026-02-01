/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import java.util.Set;

/**
 * Utility class for matching book titles with various normalization strategies.
 * Handles case differences, subtitles, articles, and punctuation variations.
 */
public final class TitleMatcher {

    /**
     * Common words to remove from search queries.
     * Includes articles and short prepositions that often differ between title variations.
     */
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an",           // Articles
            "in", "at", "to", "of",     // Common prepositions
            "on", "by", "for", "and",   // More prepositions/conjunctions
            "or", "with", "from"        // Additional common words
    );

    private TitleMatcher() {
        // Utility class
    }

    /**
     * Normalize a title for use in API search queries.
     * Removes articles, common short words, pure numbers, and punctuation to improve search matches.
     * <p>
     * Example: "The History of the World" -> "History World"
     * Example: "A Tale of Two Cities" -> "Tale Two Cities"
     * Example: "War and Peace 1952 Edition" -> "War Peace Edition"
     *
     * @param title the original title
     * @return normalized title suitable for API searches
     */
    public static String normalizeForSearch(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        // Remove punctuation, convert to lowercase, split into words
        String[] words = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .split("\\s+");

        // Keep only significant words (not in stop words list and not pure numbers)
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty() && !STOP_WORDS.contains(word) && !word.matches("\\d+")) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(word);
            }
        }

        return result.toString();
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
            if (candidateMain.contains(targetMain) || targetMain.contains(candidateMain)) {
                return true;
            }
            // If no contains match, continue to word-based matching below
        }

        // Prefix match: if one title starts with the other, it's likely the same book
        // with different subtitle (e.g., "War and Peace" vs "War and Peace: Complete Edition")
        // Note: The colon is removed during normalization, so we check startsWith instead
        if (normalizedCandidate.startsWith(normalizedTarget) || normalizedTarget.startsWith(normalizedCandidate)) {
            // Only accept if the shorter one has at least 2 significant words
            // to prevent single words from matching everything
            String shorter = normalizedCandidate.length() <= normalizedTarget.length() ? normalizedCandidate : normalizedTarget;
            if (countSignificantWords(shorter.split("\\s+")) >= 2) {
                return true;
            }
        }

        // Word-based matching for shorter titles with bidirectional verification
        return wordsMatch(candidateMain, targetMain);
    }

    /**
     * Normalize a title for comparison:
     * - Convert to lowercase
     * - Remove trailing parenthetical content (dates, editions, etc.)
     * - Remove leading articles (the, a, an)
     * - Remove punctuation except spaces
     * - Collapse multiple spaces
     */
    private static String normalize(String title) {
        return title.toLowerCase()
                .replaceAll("\\s*\\([^)]*\\)\\s*$", "") // Remove trailing (date), (edition), etc.
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
     * Requires bidirectional matching to prevent false positives where
     * a short title (like "Christmas") matches a long title containing that word.
     */
    private static boolean wordsMatch(String title1, String title2) {
        String[] words1 = title1.split("\\s+");
        String[] words2 = title2.split("\\s+");

        // Count significant words in each title
        int significant1 = countSignificantWords(words1);
        int significant2 = countSignificantWords(words2);

        // If either has no significant words, no match
        if (significant1 == 0 || significant2 == 0) {
            return false;
        }

        // Count how many significant words from title1 appear in title2
        int matchCount1to2 = countMatchingWords(words1, words2);

        // Count how many significant words from title2 appear in title1
        int matchCount2to1 = countMatchingWords(words2, words1);

        // Calculate match percentages in both directions
        double ratio1to2 = (double) matchCount1to2 / significant1;
        double ratio2to1 = (double) matchCount2to1 / significant2;

        // BOTH directions must meet the threshold to prevent false positives
        // This prevents "Christmas" from matching "How the Grinch Stole Christmas"
        // because while 100% of "Christmas" is in the target, only 25% of target is in "Christmas"
        return ratio1to2 >= 0.7 && ratio2to1 >= 0.7;
    }

    /**
     * Check if a word is significant for matching purposes.
     * A word is significant if:
     * - It has more than 2 characters
     * - It is NOT a pure number (like publication years 1952, 1999)
     */
    private static boolean isSignificantWord(String word) {
        if (word.length() <= 2) {
            return false;
        }
        // Treat pure numbers as insignificant (publication years, edition numbers, etc.)
        return !word.matches("\\d+");
    }

    /**
     * Count significant words in a word array.
     * Significant words have length > 2 and are not pure numbers.
     */
    private static int countSignificantWords(String[] words) {
        int count = 0;
        for (String word : words) {
            if (isSignificantWord(word)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Count how many significant words from source appear in target.
     */
    private static int countMatchingWords(String[] source, String[] target) {
        int matchCount = 0;
        for (String word : source) {
            if (isSignificantWord(word)) { // Only count significant words
                for (String targetWord : target) {
                    if (word.equals(targetWord)) {
                        matchCount++;
                        break;
                    }
                }
            }
        }
        return matchCount;
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
