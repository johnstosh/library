/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.Book;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Normalizes book titles for duplicate detection (Issue #351).
 * Strips copy suffixes and subtitles, drops stop words / light morphological
 * suffixes, and produces a keyword set plus a joined normalized string for
 * Jaro–Winkler comparison.
 */
public final class TitleNormalizer {

    private static final Pattern SUBTITLE_CUT =
            Pattern.compile("[:;—]|\\s+-\\s+|\\(");

    private static final Pattern NON_LETTER_DIGIT =
            Pattern.compile("[^a-z0-9]+");

    private static final Pattern WHITESPACE =
            Pattern.compile("\\s+");

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "of", "and", "or", "if", "in", "on", "to", "for",
            "by", "at", "from", "with", "as", "into", "onto", "vs", "etc",
            "fr", "mr", "mrs", "ms", "dr", "st", "vol", "volume", "book",
            "pt", "part"
    );

    private TitleNormalizer() {
    }

    /**
     * Result of normalizing one title string.
     */
    public record NormalizedTitle(String normalizedString, Set<String> keywords) {
        public boolean isEmpty() {
            return keywords == null || keywords.isEmpty();
        }
    }

    /**
     * Full normalization pipeline for a raw catalog title (or alternate title).
     */
    public static NormalizedTitle normalize(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return new NormalizedTitle("", Collections.emptySet());
        }

        String stripped = Book.stripCopySuffix(rawTitle);
        if (stripped == null || stripped.isBlank()) {
            return new NormalizedTitle("", Collections.emptySet());
        }

        String truncated = truncateSubtitle(stripped);
        String lower = truncated.toLowerCase(Locale.ROOT);
        String spaced = NON_LETTER_DIGIT.matcher(lower).replaceAll(" ").trim();
        spaced = WHITESPACE.matcher(spaced).replaceAll(" ").trim();
        if (spaced.isEmpty()) {
            return new NormalizedTitle("", Collections.emptySet());
        }

        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (String token : spaced.split(" ")) {
            if (token.isEmpty() || STOP_WORDS.contains(token)) {
                continue;
            }
            String stemmed = lightStem(token);
            if (!stemmed.isEmpty() && !STOP_WORDS.contains(stemmed)) {
                keywords.add(stemmed);
            }
        }

        String joined = String.join(" ", keywords);
        return new NormalizedTitle(joined, Collections.unmodifiableSet(keywords));
    }

    /**
     * Truncates at the first subtitle marker: colon, semicolon, em dash,
     * spaced hyphen ({@code " - "}), or opening parenthesis.
     * Deliberately does not treat bare mid-word hyphens as subtitle cuts.
     */
    static String truncateSubtitle(String title) {
        if (title == null || title.isBlank()) {
            return title;
        }
        java.util.regex.Matcher m = SUBTITLE_CUT.matcher(title);
        if (m.find()) {
            return title.substring(0, m.start()).trim();
        }
        return title.trim();
    }

    /**
     * Very light suffix stripping for keyword matching only.
     * Order matters: longer suffixes first. Keeps short tokens intact.
     */
    static String lightStem(String token) {
        if (token == null || token.length() < 4) {
            return token == null ? "" : token;
        }
        if (token.endsWith("'s") && token.length() > 3) {
            return token.substring(0, token.length() - 2);
        }
        if (token.endsWith("ing") && token.length() > 5) {
            return token.substring(0, token.length() - 3);
        }
        if (token.endsWith("ed") && token.length() > 4) {
            return token.substring(0, token.length() - 2);
        }
        if (token.endsWith("es") && token.length() > 4) {
            return token.substring(0, token.length() - 2);
        }
        if (token.endsWith("s") && token.length() > 3 && !token.endsWith("ss")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }

    /** Exposed for tests: whether a token is treated as a stop word. */
    static boolean isStopWord(String token) {
        return token != null && STOP_WORDS.contains(token.toLowerCase(Locale.ROOT));
    }
}
