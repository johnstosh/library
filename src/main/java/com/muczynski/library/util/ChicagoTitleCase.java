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
 * Chicago Manual of Style headline-style title case for a whole title,
 * including the subtitle after a colon, semicolon, or slash, and the sentence
 * after a period, question mark, or exclamation point.
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
            "concerning", "regarding", "unlike", "near", "off", "out", "up", "down",
            "de", "di", "da", "du", "van", "von"
    );

    /** English words that happen to match the roman-numeral pattern. */
    private static final Set<String> ROMAN_DENY = Set.of(
            "mix", "dix", "liv", "mid", "dim", "lid", "did", "vim", "mil",
            "civil", "mill", "dill", "livid", "civic", "mimic", "mild",
            "di"
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
            "fig", "figs", "al", "cf", "vs", "op", "cit", "ibid", "viz",
            "trans", "pt", "pts", "ser", "pl", "pls", "inc", "ltd", "co", "corp",
            "approx", "esp", "ca", "bp", "abp", "ven", "bl", "messrs", "mme", "mlle"
    );

    /** Short all-caps tokens that are names, not words to recase. */
    private static final Set<String> ACRONYM = Set.of(
            "abc", "youcat", "html", "xhtml", "css", "sql", "dvd", "cd", "tv", "pdf", "isbn"
    );

    private static final Pattern SEGMENT_SEP = Pattern.compile("\\s*([:;])\\s*|\\s+(/)\\s+");
    private static final Pattern NUMBER_SUFFIX = Pattern.compile("^(\\W*)(\\d+)([A-Za-z]+)(\\W*)$");
    private static final Pattern ROMAN_TOKEN = Pattern.compile("(?i)^(\\W*)([ivxlcdm]+)(['’]s)?(\\W*)$");
    private static final Pattern FRENCH_PARTICLE = Pattern.compile("(?i)^(\\W*)([dln])(['’])(\\p{L})(.*)$");
    private static final Pattern MC_NAME = Pattern.compile("(?i)^(\\W*)Mc(\\p{L})(.*)$");

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
     * Rewrites the whole title, including a subtitle after a colon, semicolon,
     * or slash, and each sentence after a period, question mark, or exclamation
     * point. A trailing catalog copy suffix ({@code ", c. N"}) is kept and
     * normalized to that spelling. Returns the original string when it is null
     * or blank, or when rewriting would leave nothing.
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

        List<String> parts = new ArrayList<>();
        List<String> marks = new ArrayList<>();
        Matcher segments = SEGMENT_SEP.matcher(main);
        int cursor = 0;
        while (segments.find()) {
            parts.add(main.substring(cursor, segments.start()));
            String mark = segments.group(1);
            marks.add(mark != null ? mark + " " : " / ");
            cursor = segments.end();
        }
        parts.add(main.substring(cursor));

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            String cased = titleCaseSegment(parts.get(i));
            if (cased.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(marks.get(i - 1));
            }
            out.append(cased);
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
            String next = i + 1 < words.length ? words[i + 1] : null;
            boolean edge = i == 0 || i == words.length - 1;
            boolean sentenceStart = i > 0 && endsSentence(words[i - 1], words[i]);
            boolean sentenceEnd = endsSentence(words[i], next);
            String previous = i > 0 ? words[i - 1] : "";
            String core = comparableWord(words[i]);
            boolean afterOr = previous.toLowerCase(Locale.ROOT).contains(",")
                    && "or".equals(comparableWord(previous));
            if (isVolumeNumber(words[i], next)) {
                out.append(lowercaseVolume(words[i]));
                continue;
            }
            if (!edge && isDeFamily(comparableWord(previous))
                    && ("la".equals(core) || "le".equals(core) || "les".equals(core))) {
                out.append(lowercaseLetters(words[i]));
                continue;
            }
            boolean forceWord = edge || sentenceStart || sentenceEnd || afterOr;
            out.append(titleCaseWord(words[i], edge, forceWord));
        }
        return out.toString();
    }

    private static boolean isDeFamily(String core) {
        return "de".equals(core) || "di".equals(core) || "da".equals(core) || "du".equals(core);
    }

    /** Manga and catalog {@code v. 3} / {@code v.1} stay lowercase. */
    private static boolean isVolumeNumber(String word, String next) {
        if (word.matches("(?i)v\\.\\d+")) {
            return true;
        }
        return word.matches("(?i)v\\.") && next != null && next.matches("\\d+.*");
    }

    private static String lowercaseVolume(String word) {
        return word.replaceFirst("(?i)v\\.", "v.");
    }

    private static String titleCaseWord(String word, boolean edge, boolean forceWord) {
        String[] parts = word.split("-", -1);
        if (parts.length == 1) {
            return titleCaseToken(word, forceWord);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                out.append('-');
            }
            boolean last = i == parts.length - 1;
            boolean force = i == 0 || (edge && last) || (forceWord && endsSentence(word, null) && last);
            out.append(titleCaseToken(parts[i], force));
        }
        return out.toString();
    }

    /**
     * True when this token ends a sentence, so the next word is capitalized
     * and a trailing short word on this token is capitalized too.
     */
    private static boolean endsSentence(String token, String next) {
        String stripped = token.replaceAll("[\"'”’)\\]}]+$", "");
        if (stripped.endsWith("?") || stripped.endsWith("!")
                || stripped.endsWith("…") || stripped.endsWith("...")) {
            return true;
        }
        if (!stripped.endsWith(".")) {
            return false;
        }
        String body = stripped.substring(0, stripped.length() - 1);
        if (body.isEmpty() || isInitialism(body)) {
            return false;
        }
        if (body.matches("[A-Za-z]")) {
            char letter = Character.toLowerCase(body.charAt(0));
            if ("ivxlcdm".indexOf(letter) < 0 || (next != null && isSingleInitial(next))) {
                return false;
            }
            return true;
        }
        String core = body.replaceAll("^[^\\p{L}\\p{N}]+", "")
                .replaceAll("[^\\p{L}\\p{N}]+$", "")
                .toLowerCase(Locale.ROOT);
        return !ABBREV.contains(core);
    }

    /** A one-letter initial such as {@code J.} or {@code A.} */
    private static boolean isSingleInitial(String token) {
        String core = comparableWord(token);
        return core.length() == 1 && token.indexOf('.') >= 0;
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
        if (hasInternalCap(token)) {
            return token;
        }
        String roman = romanToken(token);
        if (roman != null) {
            return roman;
        }
        String numbered = numberSuffix(token);
        if (numbered != null) {
            return numbered;
        }
        if (keepAcronym(token)) {
            return uppercaseLetters(token);
        }
        String dotted = formatWrappedDotted(token);
        if (dotted != null) {
            return dotted;
        }
        String comparable = comparableWord(token);
        if (!force && SMALL.contains(comparable)) {
            if (hasLeadingQuote(token) || hasLeadingParen(token)) {
                return fixMc(capitalizeLike(token));
            }
            return lowercaseLetters(token);
        }
        String french = frenchParticle(token);
        if (french != null) {
            return french;
        }
        return fixMc(capitalizeLike(token));
    }

    /**
     * A roman numeral, optionally possessive ({@code II's}) or punctuated
     * ({@code ii.}). {@code di} and other denied words are not numerals.
     * A token that contains a digit is not a numeral ({@code 2md}).
     */
    private static String romanToken(String token) {
        if (token.chars().anyMatch(Character::isDigit)) {
            return null;
        }
        Matcher match = ROMAN_TOKEN.matcher(token);
        if (!match.matches()) {
            return null;
        }
        String numeral = match.group(2);
        if (ROMAN_DENY.contains(numeral.toLowerCase(Locale.ROOT)) || !ROMAN.matcher(numeral).matches()) {
            return null;
        }
        String possessive = match.group(3) == null ? "" : "'s";
        return match.group(1) + numeral.toUpperCase(Locale.ROOT) + possessive + match.group(4);
    }

    private static boolean keepAcronym(String token) {
        if (token.indexOf('.') >= 0) {
            return false;
        }
        String letters = token.replaceAll("[^\\p{L}]", "");
        if (letters.length() < 2 || letters.length() > 10) {
            return false;
        }
        for (int i = 0; i < letters.length(); i++) {
            if (Character.isLowerCase(letters.charAt(i))) {
                return false;
            }
        }
        String lower = letters.toLowerCase(Locale.ROOT);
        if (ACRONYM.contains(lower)) {
            return true;
        }
        return letters.length() <= 6 && !lower.matches(".*[aeiou].*");
    }

    private static boolean hasLeadingQuote(String token) {
        return !token.isEmpty() && "\"'“‘".indexOf(token.charAt(0)) >= 0;
    }

    private static boolean hasLeadingParen(String token) {
        return !token.isEmpty() && token.charAt(0) == '(';
    }

    /** {@code 21st}, {@code 16th,}, {@code (4th}, and a lowercase tail such as {@code 122ff}. */
    private static String numberSuffix(String token) {
        Matcher match = NUMBER_SUFFIX.matcher(token);
        if (!match.matches()) {
            return null;
        }
        String letters = match.group(3);
        String suffix;
        if (letters.matches("(?i)st|nd|rd|th")) {
            suffix = letters.toLowerCase(Locale.ROOT);
        } else if (letters.equals(letters.toLowerCase(Locale.ROOT)) || letters.equals(letters.toUpperCase(Locale.ROOT))) {
            suffix = letters.equals(letters.toUpperCase(Locale.ROOT))
                    ? letters.toUpperCase(Locale.ROOT)
                    : letters;
        } else {
            return null;
        }
        return match.group(1) + match.group(2) + suffix + match.group(4);
    }

    /** {@code GIFTionary}, {@code McFadden}, {@code Ph.D.} already mark their own capitals. */
    private static boolean hasInternalCap(String token) {
        boolean seenLetter = false;
        boolean seenLower = false;
        boolean seenUpperAfter = false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!Character.isLetter(c)) {
                continue;
            }
            if (!seenLetter) {
                seenLetter = true;
                continue;
            }
            if (Character.isUpperCase(c)) {
                seenUpperAfter = true;
            } else if (Character.isLowerCase(c)) {
                seenLower = true;
            }
        }
        return seenUpperAfter && seenLower;
    }

    /** {@code d'Arc} and {@code l'Histoire}. The particle stays lower. */
    private static String frenchParticle(String token) {
        Matcher match = FRENCH_PARTICLE.matcher(token);
        if (!match.matches()) {
            return null;
        }
        String rest = match.group(5);
        return match.group(1) + match.group(2).toLowerCase(Locale.ROOT) + "'"
                + Character.toUpperCase(match.group(4).charAt(0))
                + rest.toLowerCase(Locale.ROOT);
    }

    private static String fixMc(String token) {
        Matcher match = MC_NAME.matcher(token);
        if (!match.matches()) {
            return token;
        }
        return match.group(1) + "Mc" + match.group(2).toUpperCase(Locale.ROOT) + match.group(3);
    }

    private static String uppercaseLetters(String token) {
        StringBuilder out = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            out.append(Character.isLetter(c) ? Character.toUpperCase(c) : c);
        }
        return out.toString();
    }

    /** {@code D.D.,} and {@code [i.e.} keep the abbreviation and the punctuation around it. */
    private static String formatWrappedDotted(String token) {
        Matcher match = Pattern.compile("^([^A-Za-z.]*)([A-Za-z][A-Za-z.]*)([^A-Za-z.]*)$").matcher(token);
        if (!match.matches() || !match.group(2).contains(".")) {
            return null;
        }
        if (!isDottedAbbreviation(match.group(2))) {
            return null;
        }
        return match.group(1) + formatDotted(match.group(2)) + match.group(3);
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
            if (part.length() == 2
                    && Character.toLowerCase(part.charAt(0)) == Character.toLowerCase(part.charAt(1))) {
                out.append(Character.toUpperCase(part.charAt(0)));
                out.append(Character.toUpperCase(part.charAt(1)));
            } else {
                out.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    out.append(part.substring(1).toLowerCase(Locale.ROOT));
                }
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
