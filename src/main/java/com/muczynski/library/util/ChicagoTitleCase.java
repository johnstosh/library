/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chicago Manual of Style headline-style title case for a whole title,
 * including the subtitle after a colon and the sentence after a period,
 * question mark, or exclamation point.
 * <p>
 * The TypeScript twin is {@code frontend/src/utils/chicagoTitleCase.ts}.
 * Keep the two in step: major words are capitalized, the first and last word
 * of the title, of each subtitle, and of each sentence are capitalized, and
 * short function words stay lower elsewhere. A period after an initial
 * ({@code J.}) or an abbreviation ({@code St.}, {@code U.S.}) does not start
 * a new sentence.
 */
public final class ChicagoTitleCase {

    /**
     * Articles, coordinating conjunctions, {@code to}, {@code as}, and
     * prepositions. Chicago lowercases these unless they are the first or
     * last word of the title or subtitle.
     */
    private static final Set<String> SMALL = Set.of(
            "a", "an", "the",
            "and", "but", "or", "nor", "for",
            "to", "as",
            "of", "in", "on", "at", "by", "from", "with", "into", "onto", "upon",
            "over", "under", "about", "after", "before", "between", "through",
            "during", "without", "within", "against", "among", "around", "across",
            "behind", "beyond", "despite", "except", "toward", "towards", "until",
            "via", "per", "vs", "versus", "amid", "amongst", "beside", "besides",
            "concerning", "regarding", "unlike", "near", "off", "out", "up", "down"
    );

    /** English words that happen to match the roman-numeral pattern. */
    private static final Set<String> ROMAN_DENY = Set.of(
            "mix", "dix", "liv", "mid", "dim", "lid", "did", "vim", "mil",
            "civil", "mill", "dill", "livid", "civic", "mimic", "mild"
    );

    /**
     * One-word abbreviations. Their period does not start a new sentence.
     * Single-letter initials and dotted initialisms ({@code U.S.}, {@code Ph.D.})
     * are recognized separately.
     */
    private static final Set<String> ABBREV = Set.of(
            "st", "ste", "mr", "mrs", "ms", "mss", "dr", "jr", "sr",
            "fr", "br", "mt", "ft", "gen", "col", "capt", "sgt", "lt",
            "prof", "rev", "hon", "pres", "sen", "gov", "rep",
            "vol", "vols", "ed", "eds", "no", "nos", "pp", "ch", "chap", "chaps",
            "fig", "figs", "etc", "al", "cf", "vs", "op", "cit", "ibid", "viz",
            "trans", "pt", "pts", "ser", "pl", "pls", "inc", "ltd", "co", "corp",
            "approx", "esp", "ca", "bp", "abp", "ven", "bl", "messrs", "mme", "mlle"
    );

    private static final Pattern COPY_SUFFIX = Pattern.compile("(?i),\\s*c\\.?\\s*(\\d+)\\s*$");
    private static final Pattern ROMAN = Pattern.compile(
            "(?i)^(?=[ivxlcdm]+$)m{0,4}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$");

    private ChicagoTitleCase() {
    }

    /**
     * True when a non-blank title is not already in Chicago title case.
     * Blank titles are left alone.
     */
    public static boolean needsWork(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        String chicago = toChicago(title);
        return chicago != null && !chicago.equals(title);
    }

    /**
     * Rewrites the whole title, including a subtitle after a colon and each
     * sentence after a period, question mark, or exclamation point.
     * A trailing catalog copy suffix ({@code ", c. N"}) is kept and normalized
     * to that spelling. Returns the original string when it is null or blank,
     * or when rewriting would leave nothing.
     */
    public static String toChicago(String title) {
        if (title == null || title.isBlank()) {
            return title;
        }
        String trimmed = title.trim().replaceAll("\\s+", " ");
        Matcher suffix = COPY_SUFFIX.matcher(trimmed);
        String main = trimmed;
        String copy = "";
        if (suffix.find() && suffix.start() > 0) {
            main = trimmed.substring(0, suffix.start()).trim();
            copy = ", c. " + suffix.group(1);
            if (main.isEmpty()) {
                return title;
            }
        }

        String[] segments = main.split("\\s*:\\s*", -1);
        StringBuilder out = new StringBuilder();
        boolean wroteSegment = false;
        for (String segment : segments) {
            String cased = titleCaseSegment(segment);
            if (cased.isEmpty()) {
                continue;
            }
            if (wroteSegment) {
                out.append(": ");
            }
            out.append(cased);
            wroteSegment = true;
        }
        if (!copy.isEmpty()) {
            out.append(copy);
        }
        String result = out.toString().trim();
        return result.isEmpty() ? title : result;
    }

    private static String titleCaseSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return "";
        }
        String[] words = segment.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            boolean edge = i == 0 || i == words.length - 1;
            boolean sentenceStart = i > 0 && endsSentence(words[i - 1]);
            boolean sentenceEnd = endsSentence(words[i]);
            out.append(titleCaseWord(words[i], edge, sentenceStart || sentenceEnd));
        }
        return out.toString();
    }

    private static String titleCaseWord(String word, boolean edge, boolean sentenceEdge) {
        String[] parts = word.split("-", -1);
        if (parts.length == 1) {
            return titleCaseToken(word, edge || sentenceEdge);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                out.append('-');
            }
            boolean last = i == parts.length - 1;
            boolean force = i == 0 || (edge && last) || (sentenceEdge && endsSentence(word) && last);
            out.append(titleCaseToken(parts[i], force));
        }
        return out.toString();
    }

    /**
     * True when this token ends a sentence, so the next word is capitalized
     * and a trailing short word on this token is capitalized too.
     */
    private static boolean endsSentence(String token) {
        String stripped = token.replaceAll("[\"'”’)\\]}]+$", "");
        if (stripped.endsWith("?") || stripped.endsWith("!")
                || stripped.endsWith("…") || stripped.endsWith("...")) {
            return true;
        }
        if (!stripped.endsWith(".")) {
            return false;
        }
        String body = stripped.substring(0, stripped.length() - 1);
        if (body.isEmpty() || isInitialism(body) || body.matches("[A-Za-z]")) {
            return false;
        }
        String core = body.replaceAll("^[^\\p{L}\\p{N}]+", "")
                .replaceAll("[^\\p{L}\\p{N}]+$", "")
                .toLowerCase(Locale.ROOT);
        return !ABBREV.contains(core);
    }

    /** {@code U.S} or {@code Ph.D}: two or more short letter groups. */
    private static boolean isInitialism(String body) {
        if (!body.contains(".")) {
            return false;
        }
        String[] parts = body.split("\\.", -1);
        int groups = 0;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!part.matches("[A-Za-z]{1,2}")) {
                return false;
            }
            groups++;
        }
        return groups >= 2;
    }

    private static String titleCaseToken(String token, boolean force) {
        if (token.isEmpty()) {
            return token;
        }
        if (isRomanNumeral(token)) {
            return uppercaseLetters(token);
        }
        String comparable = comparableWord(token);
        if (!force && SMALL.contains(comparable)) {
            return lowercaseLetters(token);
        }
        if (isDottedAbbreviation(token)) {
            return formatDotted(token);
        }
        return capitalizeLike(token);
    }

    private static boolean isRomanNumeral(String token) {
        String core = token.replaceAll("^[^\\p{L}]+", "").replaceAll("[^\\p{L}]+$", "");
        if (core.isEmpty() || !core.matches("(?i)[ivxlcdm]+")) {
            return false;
        }
        if (ROMAN_DENY.contains(core.toLowerCase(Locale.ROOT))) {
            return false;
        }
        return ROMAN.matcher(core).matches();
    }

    private static String uppercaseLetters(String token) {
        StringBuilder out = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            out.append(Character.isLetter(c) ? Character.toUpperCase(c) : c);
        }
        return out.toString();
    }

    private static boolean isDottedAbbreviation(String token) {
        if (!token.contains(".")) {
            return false;
        }
        String[] parts = token.split("\\.", -1);
        boolean any = false;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!part.matches("[A-Za-z]{1,2}")) {
                return false;
            }
            any = true;
        }
        return any;
    }

    private static String formatDotted(String token) {
        boolean trailingDot = token.endsWith(".");
        String[] parts = token.split("\\.", -1);
        StringBuilder out = new StringBuilder();
        boolean wrote = false;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (wrote) {
                out.append('.');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
            wrote = true;
        }
        if (trailingDot) {
            out.append('.');
        }
        return out.toString();
    }

    private static String comparableWord(String token) {
        return token.replaceAll("^[^\\p{L}\\p{N}]+", "")
                .replaceAll("[^\\p{L}\\p{N}]+$", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String lowercaseLetters(String token) {
        StringBuilder out = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            out.append(Character.isLetter(c) ? Character.toLowerCase(c) : c);
        }
        return out.toString();
    }

    /**
     * Capitalizes the first letter and the letter after a one-letter apostrophe
     * ({@code O'Brien}). Other letters are lower. Punctuation stays put.
     */
    private static String capitalizeLike(String token) {
        StringBuilder out = new StringBuilder(token.length());
        boolean capNext = true;
        boolean capAfterApostrophe = false;
        int lettersSeen = 0;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isLetter(c)) {
                if (capNext || capAfterApostrophe) {
                    out.append(Character.toUpperCase(c));
                    capNext = false;
                    capAfterApostrophe = false;
                } else {
                    out.append(Character.toLowerCase(c));
                }
                lettersSeen++;
            } else {
                out.append(c);
                if (c == '\'' && lettersSeen == 1) {
                    capAfterApostrophe = true;
                }
            }
        }
        return out.toString();
    }
}
