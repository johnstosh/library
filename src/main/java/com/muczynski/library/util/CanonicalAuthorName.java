/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Canonical author name: given name(s), then family name(s).
 * <p>
 * Strips appended birth and death years, turns {@code Family, Given} around,
 * and expands initials when a parenthetical spells them out. The expanded
 * form drops parentheses, dashes, and periods. An initial with no
 * parenthetical expansion is left as written. A comma inside a phrase
 * ({@code Sisters of Charity of Our Lady, Mother of the Church},
 * {@code Ignatius, of Loyola}) is left in place. Editor and translator
 * credits are removed and are not stored anywhere else.
 * <p>
 * The TypeScript twin is {@code frontend/src/utils/canonicalAuthorName.ts}.
 * This is not a place for pen names or Latin forms; those are alternate names.
 */
public final class CanonicalAuthorName {

    private static final Set<String> SUFFIXES = Set.of(
            "jr", "sr", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x",
            "esq", "phd", "md", "op", "sj", "osb", "ofm", "cssr", "osa", "slg", "fr", "rev", "dr"
    );

    private static final Set<String> ROMAN_DENY = Set.of(
            "mix", "dix", "liv", "mid", "dim", "lid", "did", "vim", "mil",
            "civil", "mill", "dill", "livid", "civic", "mimic", "mild"
    );

    /** Whole words that mark a phrase rather than a family or given name. */
    private static final Set<String> PHRASE_WORDS = Set.of("of", "the");

    private static final String YEAR_PREFIX =
            "(?:(?:b|d|c|ca|fl)\\.?|born|died|circa|floruit)?";
    private static final String YEAR_BODY =
            "\\d{3,4}\\??\\s*(?:[\\-\\u2013\\u2014]\\s*\\d{0,4}\\??)?";

    private static final Pattern PAREN_YEAR = Pattern.compile(
            "\\s*\\(\\s*" + YEAR_PREFIX + "\\s*" + YEAR_BODY + "\\s*\\)\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMA_YEAR = Pattern.compile(
            "\\s*,\\s*" + YEAR_PREFIX + "\\s*" + YEAR_BODY + "\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PAREN = Pattern.compile("\\(([^)]*)\\)");
    private static final Pattern ROMAN = Pattern.compile(
            "(?i)^(?=[ivxlcdm]+$)m{0,4}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$");

    /**
     * An editor or translator role. {@code ed} does not match {@code Edith}
     * or {@code edition}; the two-letter forms require the period.
     */
    private static final String CREDIT_ROLE =
            "edited\\s+by|editors|editor|translated\\s+by|translated|translators|translator|transl\\.|trans\\.|tr\\.|eds\\.|eds|ed\\.|ed";

    private static final Pattern CREDIT_PAREN = Pattern.compile(
            "\\s*\\(\\s*(?:" + CREDIT_ROLE + ")(?=[\\s).;])[^)]*\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CREDIT_CLAUSE = Pattern.compile(
            ";\\s*(?:" + CREDIT_ROLE + ")(?=[\\s).;])[^);]*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CREDIT_TRAIL = Pattern.compile(
            "(?i)(?:\\s*[.;])?\\s+(?:edited|translated)\\s+by\\b.*$|,\\s*(?:editors?|translators?|trans\\.?|transl\\.?|tr\\.)\\s*$");

    private CanonicalAuthorName() {
    }

    /**
     * True when a non-blank name is not already in canonical form.
     * Blank names are left alone.
     */
    public static boolean needsWork(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String canonical = canonical(name);
        return canonical != null && !canonical.equals(name);
    }

    /**
     * Returns the canonical form, or the original string when it is null,
     * blank, or would normalize to nothing.
     */
    public static String canonical(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String collapsed = raw.trim().replaceAll("\\s+", " ");
        String yearStripped = stripYears(collapsed);
        if (yearStripped.isBlank()) {
            return raw;
        }
        String prepared = stripCredits(yearStripped);
        if (prepared.isBlank()) {
            return raw;
        }

        Matcher paren = PAREN.matcher(prepared);
        String expansion = null;
        if (paren.find()) {
            String inside = paren.group(1).trim();
            if (!inside.isEmpty()) {
                expansion = inside;
            }
        }
        String outside = PAREN.matcher(prepared).replaceAll(" ");
        outside = outside.trim().replaceAll("\\s+", " ");
        outside = outside.replaceAll("\\s+,", ",").replaceAll(",\\s*", ", ");
        outside = outside.replaceAll("\\s+", " ").trim();
        while (outside.endsWith(",")) {
            outside = outside.substring(0, outside.length() - 1).trim();
        }

        String result;
        if (expansion != null && containsInitial(outside)) {
            result = finishExpanded(expansion, outside);
        } else {
            result = invertCommas(prepared);
        }
        if (result == null || result.isBlank()) {
            return raw;
        }
        return result;
    }

    /**
     * Drops editor and translator credits. A parenthetical that only names
     * the person more fully, and an edition note such as {@code (Benziger ed.)},
     * stay. The removed credit is not saved.
     */
    private static String stripCredits(String value) {
        String current = value;
        boolean changed = true;
        while (changed) {
            changed = false;
            String next = CREDIT_PAREN.matcher(current).replaceAll("");
            next = CREDIT_CLAUSE.matcher(next).replaceAll("");
            next = CREDIT_TRAIL.matcher(next).replaceAll("");
            next = next.replaceAll("\\(\\s*\\)", "");
            next = next.replaceAll("\\s+", " ").trim();
            next = next.replaceAll("\\s+([,;])", "$1");
            next = next.replaceAll("([,;])(?!\\s)", "$1 ");
            while (next.endsWith(",") || next.endsWith(";")) {
                next = next.substring(0, next.length() - 1).trim();
            }
            if (!next.equals(current)) {
                current = next;
                changed = true;
            }
        }
        return current;
    }

    private static String stripYears(String value) {
        String current = value;
        boolean changed = true;
        while (changed) {
            changed = false;
            String next = PAREN_YEAR.matcher(current).replaceAll(" ");
            next = COMMA_YEAR.matcher(next).replaceAll("");
            next = next.replaceAll("\\s+", " ").trim();
            while (next.endsWith(",")) {
                next = next.substring(0, next.length() - 1).trim();
            }
            if (!next.equals(current)) {
                current = next;
                changed = true;
            }
        }
        return current;
    }

    private static String finishExpanded(String expansion, String outside) {
        String family = cleanExpanded(familyName(outside));
        String cleaned = cleanExpanded(expansion);
        if (!family.isEmpty() && !containsPhrase(cleaned, family)) {
            cleaned = (cleaned + " " + family).trim();
        }
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private static String familyName(String outside) {
        int comma = outside.indexOf(',');
        if (comma >= 0) {
            return outside.substring(0, comma).trim();
        }
        String[] tokens = outside.split("\\s+");
        for (int i = tokens.length - 1; i >= 0; i--) {
            if (!tokens[i].isEmpty() && !isInitialToken(tokens[i])) {
                return tokens[i];
            }
        }
        return "";
    }

    private static String cleanExpanded(String value) {
        String cleaned = value
                .replace('\u2013', ' ')
                .replace('\u2014', ' ')
                .replace('-', ' ')
                .replace(".", "")
                .replace("(", "")
                .replace(")", "");
        return cleaned.trim().replaceAll("\\s+", " ");
    }

    private static boolean containsPhrase(String name, String phrase) {
        String haystack = " " + name.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ") + " ";
        String needle = " " + phrase.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim() + " ";
        return haystack.contains(needle);
    }

    private static boolean containsInitial(String outside) {
        if (outside == null || outside.isBlank()) {
            return false;
        }
        for (String token : outside.split("\\s+")) {
            String bare = token.replaceAll("^,+|,+$", "");
            if (isInitialToken(bare)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A token of single letters separated by periods or hyphens, such as
     * {@code B.}, {@code J.-P.}, or {@code B.J.}. A spelled name is not an initial.
     */
    private static boolean isInitialToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] pieces = token.split("[.\\-]+");
        boolean any = false;
        for (String piece : pieces) {
            if (piece.isEmpty()) {
                continue;
            }
            if (!piece.matches("[A-Za-z]")) {
                return false;
            }
            any = true;
        }
        return any;
    }

    private static String invertCommas(String value) {
        String collapsed = value.trim().replaceAll("\\s+", " ");
        collapsed = collapsed.replaceAll("\\s*,\\s*", ", ");
        collapsed = collapsed.trim();
        while (collapsed.endsWith(",")) {
            collapsed = collapsed.substring(0, collapsed.length() - 1).trim();
        }
        if (!collapsed.contains(",")) {
            return collapsed;
        }
        String[] rawParts = collapsed.split(",");
        List<String> parts = new ArrayList<>();
        for (String part : rawParts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        if (parts.size() < 2) {
            return collapsed;
        }
        if (parts.size() == 2 && isSuffix(parts.get(1))) {
            return parts.get(0) + " " + parts.get(1);
        }
        for (int i = 2; i < parts.size(); i++) {
            if (!isSuffix(parts.get(i))) {
                return collapsed;
            }
        }
        if (isPhrase(parts.get(0)) || isPhrase(parts.get(1))) {
            return collapsed;
        }
        StringBuilder out = new StringBuilder();
        out.append(parts.get(1)).append(' ').append(parts.get(0));
        for (int i = 2; i < parts.size(); i++) {
            out.append(' ').append(parts.get(i));
        }
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * True when {@code part} is a phrase joined by a comma, such as
     * {@code Mother of the Church} or {@code of Loyola}. Those parts stay
     * in the order they were written.
     */
    private static boolean isPhrase(String part) {
        for (String token : part.split("\\s+")) {
            String key = token.replaceAll("^\\P{L}+|\\P{L}+$", "").toLowerCase(Locale.ROOT);
            if (PHRASE_WORDS.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSuffix(String part) {
        String key = part.replace(".", "").replace(" ", "").toLowerCase(Locale.ROOT);
        if (SUFFIXES.contains(key)) {
            return true;
        }
        if (ROMAN_DENY.contains(key) || key.length() < 2 || key.length() > 6) {
            return false;
        }
        return ROMAN.matcher(key).matches();
    }
}
