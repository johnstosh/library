/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Canonical author name: given name(s), then family name(s).
 * <p>
 * Strips appended birth and death years (including {@code approximately}
 * and a year phrase that was moved in front of the name), turns
 * {@code Family, Given} around, and expands initials when a parenthetical
 * spells them out. A two-letter abbreviation such as {@code Wm.} or
 * {@code L.-Cl.} counts as an initial. A parenthetical that is only
 * initials ({@code C. C.}) is removed. The expanded form drops parentheses,
 * dashes, and periods. An initial with no parenthetical expansion is left
 * as written. A comma inside a phrase
 * ({@code Sisters of Charity of Our Lady, Mother of the Church},
 * {@code Ignatius, of Loyola}) is left in place. Editor and translator
 * credits are removed and are not stored anywhere else. {@code [from old catalog]}
 * is removed. UTF-8 letters that were read as Latin-1 are repaired.
 * A dotted abbreviation that is a prefix of the parenthetical name is
 * expanded ({@code Joh. Evang.} becomes {@code Johannes Evangelist}).
 * A shorter copy of the same name is dropped. {@code Thomas, à Kempis}
 * stays {@code Thomas à Kempis}. A {@code tr.} relator is removed as a
 * translator credit. A name whose letters are all capitals or all lowercase is
 * rewritten with initial capitals; particles such as {@code von} and
 * {@code de la} stay lower. A no-space run of two to six capital letters
 * is a postnominal suffix ({@code OCD}, {@code D.D.}). Spaced initials
 * such as {@code C. L} stay in the given-name slot.
 * <p>
 * The TypeScript twin is {@code frontend/src/utils/canonicalAuthorName.ts}.
 * This is not a place for pen names or Latin forms; those are alternate names.
 */
public final class CanonicalAuthorName {

    private static final Set<String> SUFFIXES = Set.of(
            "jr", "sr", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x",
            "esq", "phd", "md", "op", "sj", "osb", "ofm", "cssr", "osa", "slg", "cssp", "opraem",
            "fr", "rev", "dr", "inc", "llc"
    );

    private static final Set<String> ROMAN_DENY = Set.of(
            "mix", "dix", "liv", "mid", "dim", "lid", "did", "vim", "mil",
            "civil", "mill", "dill", "livid", "civic", "mimic", "mild"
    );

    /** Whole words that mark a phrase rather than a family or given name. */
    private static final Set<String> PHRASE_WORDS = Set.of("of", "the");

    /**
     * Titles and saint-abbreviations. {@code St.} is not a given-name initial,
     * so a parenthetical fuller name stays.
     */
    private static final Set<String> TITLE_ABBREV = Set.of(
            "st", "fr", "dr", "mr", "ms", "mrs", "sr", "jr", "bp", "br", "mt", "rt", "ss", "mm"
    );

    /**
     * Postnominals that stay capital when an all-caps or all-lower name is
     * recased. Word suffixes such as {@code Jr.} and {@code Rev.} are not here.
     */
    private static final Set<String> KEEP_UPPER = Set.of(
            "op", "sj", "osb", "ofm", "cssr", "osa", "slg", "cssp", "opraem",
            "llc", "phd", "md", "ocd", "cp", "mic", "bvm", "dd", "ss", "mm"
    );

    /** Particles and the few function words that stay lower inside a personal name. */
    private static final Set<String> NAME_SMALL = Set.of(
            "and", "or", "of", "the",
            "de", "di", "da", "du", "van", "von", "del", "della", "den", "der", "ten", "ter", "y"
    );

    /** Two-letter regnal numbers. {@code Cl.} is Claude, not 150. */
    private static final Set<String> REGNAL_TWO = Set.of(
            "ii", "iv", "vi", "ix", "xi", "xv", "xx"
    );

    private static final String YEAR_PREFIX =
            "(?:approximately|approx\\.?|circa|floruit|born|died|(?:b|d|c|ca|fl)\\.?)?";
    private static final String YEAR_QUALIFIER =
            "(?:approximately|approx\\.?|circa|floruit|born|died|(?:ca|fl|b|d|c)\\.)";
    private static final String YEAR_BODY =
            "\\d{3,4}\\??\\s*(?:[\\-\\u2013\\u2014]\\s*\\d{0,4}\\??)?";

    private static final Pattern PAREN_YEAR = Pattern.compile(
            "\\s*\\(\\s*" + YEAR_PREFIX + "\\s*" + YEAR_BODY + "\\s*\\)\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMA_YEAR = Pattern.compile(
            "\\s*,\\s*" + YEAR_PREFIX + "\\s*" + YEAR_BODY + "\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_YEAR = Pattern.compile(
            "^" + YEAR_QUALIFIER + "\\s+" + YEAR_BODY + "\\s+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_YEAR = Pattern.compile(
            "\\s+" + YEAR_QUALIFIER + "\\s+" + YEAR_BODY + "\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CATALOG_NOTE = Pattern.compile(
            "(?i)\\s*\\[\\s*from\\s+old\\s+catalog\\s*\\]");
    private static final Pattern ROMAN_TOKEN = Pattern.compile(
            "(?i)^(\\W*)([ivxlcdm]+)(\\W*)$");
    private static final Pattern FRENCH_PARTICLE = Pattern.compile(
            "(?i)^(\\W*)([dln])(['’])(\\p{L})(.*)$");
    private static final Pattern MC_NAME = Pattern.compile("(?i)^(\\W*)Mc(\\p{L})(.*)$");
    private static final Pattern PAREN = Pattern.compile("\\(([^)]*)\\)");
    private static final Pattern ROMAN = Pattern.compile(
            "(?i)^(?=[ivxlcdm]+$)m{0,4}(cm|cd|d?c{0,3})(xc|xl|l?x{0,3})(ix|iv|v?i{0,3})$");
    /** Single-letter initials with a space, such as {@code S. J.} after a given name. */
    private static final Pattern SPACED_INITIALS = Pattern.compile("^(?:[A-Za-z]\\.\\s+)+[A-Za-z]\\.?$");

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
            "(?i)(?:\\s*[.;])?\\s+(?:edited|translated)\\s+by\\b.*$|,\\s*(?:editors?|translators?|trans\\.?|transl\\.?|tr\\.)\\s*$|\\btr\\.(?=\\s|$)");

    /** A second comma-part that begins with one of these is another person, not a given name. */
    private static final Set<String> CLERICAL_TITLES = Set.of(
            "rev", "reverend", "rt", "fr", "father", "cardinal", "archbishop", "bishop",
            "abbot", "pope", "sister", "brother", "dom", "monsignor", "msgr"
    );

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
        collapsed = Normalizer.normalize(collapsed, Normalizer.Form.NFC);
        collapsed = repairMojibake(collapsed);
        collapsed = Normalizer.normalize(collapsed, Normalizer.Form.NFC);
        collapsed = stripCatalogNote(collapsed);
        collapsed = stripStrayBrackets(collapsed);
        if (collapsed.isBlank()) {
            return raw;
        }
        String yearStripped = stripYears(collapsed);
        if (yearStripped.isBlank()) {
            return raw;
        }
        String kempis = thomasAKempis(yearStripped);
        if (kempis != null) {
            return kempis;
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
        if (expansion != null && isRedundantInitials(expansion)) {
            result = invertCommas(outside);
        } else if (expansion != null && (containsInitial(outside) || abbreviatesParenthetical(outside, expansion))) {
            result = finishExpanded(expansion, outside);
        } else {
            result = invertCommas(prepared);
        }
        if (result == null || result.isBlank()) {
            return raw;
        }
        return normalizeUniformCase(result);
    }

    /** Drops a Library of Congress {@code [from old catalog]} note. */
    private static String stripCatalogNote(String value) {
        String next = CATALOG_NOTE.matcher(value).replaceAll("");
        next = next.replaceAll("\\s+", " ").trim();
        while (next.endsWith(",")) {
            next = next.substring(0, next.length() - 1).trim();
        }
        return next;
    }

    /** Drops a square bracket left over from a catalog heading, such as {@code Joachim]}. */
    private static String stripStrayBrackets(String value) {
        if (value.indexOf('[') < 0 && value.indexOf(']') < 0) {
            return value;
        }
        String next = value.replace("[", "").replace("]", "");
        return next.replaceAll("\\s+", " ").trim();
    }

    /**
     * Repairs UTF-8 text that was read as Latin-1. {@code é} stored as
     * U+00C3 U+00A9 becomes {@code é} again. A name that is already Unicode
     * is left alone.
     */
    private static String repairMojibake(String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean changed = false;
        int i = 0;
        while (i < value.length()) {
            int cp = value.charAt(i);
            if ((cp == 0xC2 || cp == 0xC3) && i + 1 < value.length()) {
                int next = value.charAt(i + 1);
                if (next >= 0x80 && next <= 0xBF) {
                    out.append(decodeUtf8((byte) cp, (byte) next));
                    i += 2;
                    changed = true;
                    continue;
                }
            }
            if (cp >= 0xE0 && cp <= 0xEF && i + 2 < value.length()) {
                int n1 = value.charAt(i + 1);
                int n2 = value.charAt(i + 2);
                if (n1 >= 0x80 && n1 <= 0xBF && n2 >= 0x80 && n2 <= 0xBF) {
                    String decoded = new String(new byte[] {(byte) cp, (byte) n1, (byte) n2}, StandardCharsets.UTF_8);
                    if (decoded.length() == 1 && decoded.charAt(0) != '\uFFFD') {
                        out.append(decoded);
                        i += 3;
                        changed = true;
                        continue;
                    }
                }
            }
            out.append((char) cp);
            i++;
        }
        return changed ? out.toString() : value;
    }

    private static String decodeUtf8(byte first, byte second) {
        String decoded = new String(new byte[] {first, second}, StandardCharsets.UTF_8);
        if (decoded.length() == 1 && decoded.charAt(0) != '\uFFFD') {
            return decoded;
        }
        return new String(new char[] {(char) (first & 0xFF), (char) (second & 0xFF)});
    }

    /** {@code Thomas, à Kempis} and the inverted {@code à Kempis Thomas} are the same name. */
    private static String thomasAKempis(String value) {
        String key = value.toLowerCase(Locale.ROOT).replace(',', ' ').replaceAll("\\s+", " ").trim();
        if (key.equals("thomas à kempis") || key.equals("à kempis thomas")
                || key.equals("thomas a kempis") || key.equals("a kempis thomas")) {
            return "Thomas à Kempis";
        }
        return null;
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
            next = TRAILING_YEAR.matcher(next).replaceAll("");
            next = LEADING_YEAR.matcher(next).replaceAll("");
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
     * A token of one- or two-letter pieces separated by periods or hyphens,
     * such as {@code B.}, {@code J.-P.}, {@code Wm.}, or {@code L.-Cl.}.
     * A spelled name is not an initial. {@code St.} and {@code Ph.D.} are not
     * given-name initials. A two-letter piece needs a period, and a degree
     * such as {@code LL.D.} is not one.
     */
    private static boolean isInitialToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] pieces = token.split("[.\\-]+");
        boolean any = false;
        boolean two = false;
        for (String piece : pieces) {
            if (piece.isEmpty()) {
                continue;
            }
            if (!piece.matches("[A-Za-z]{1,2}")) {
                return false;
            }
            if (piece.length() == 2) {
                two = true;
            }
            any = true;
        }
        if (!any) {
            return false;
        }
        if (!two) {
            return true;
        }
        if (token.indexOf('.') < 0 || isSuffix(token)) {
            return false;
        }
        String key = token.replaceAll("[.\\-]", "").toLowerCase(Locale.ROOT);
        if (TITLE_ABBREV.contains(key)) {
            return false;
        }
        return token.indexOf('-') >= 0 || key.length() == 2;
    }

    /**
     * Parenthetical given-name initials, such as {@code C. C.} or {@code Wm.}.
     * A degree or religious postnominal ({@code D.D.}, {@code S.J.}, {@code OP})
     * stays.
     */
    private static boolean isRedundantInitials(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        String key = trimmed.replaceAll("[.\\-\\s]", "").toLowerCase(Locale.ROOT);
        if (key.isEmpty() || SUFFIXES.contains(key) || KEEP_UPPER.contains(key)) {
            return false;
        }
        for (String token : trimmed.split("\\s+")) {
            if (!isInitialToken(token)) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@code Joh. Evang.} abbreviates {@code Johannes Evangelist}: every
     * dotted token is a shorter prefix of the matching parenthetical word.
     */
    private static boolean abbreviatesParenthetical(String outside, String expansion) {
        int comma = outside.indexOf(',');
        if (comma < 0) {
            return false;
        }
        String given = outside.substring(comma + 1).trim();
        if (given.isEmpty()) {
            return false;
        }
        String[] givenTokens = given.split("\\s+");
        String[] expanded = expansion.trim().split("\\s+");
        if (givenTokens.length == 0 || givenTokens.length != expanded.length) {
            return false;
        }
        for (int i = 0; i < givenTokens.length; i++) {
            String token = givenTokens[i].replaceAll("^,+|,+$", "");
            if (!token.endsWith(".")) {
                return false;
            }
            String prefix = token.replaceAll("\\P{L}", "");
            String full = expanded[i].replaceAll("\\P{L}", "");
            if (prefix.isEmpty() || prefix.length() >= full.length()) {
                return false;
            }
            if (!full.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
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
        if (parts.size() == 2) {
            String longer = longerSameName(parts.get(0), parts.get(1));
            if (longer != null) {
                return longer;
            }
            if (isAnotherPerson(parts.get(0), parts.get(1))) {
                return collapsed;
            }
        }
        for (int i = 2; i < parts.size(); i++) {
            if (!isSuffix(parts.get(i)) && !isSpacedInitials(parts.get(i))) {
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
     * {@code Joseph Hergenröther, Joseph Adam Gustav Hergenröther} is one
     * person written twice. Keep the longer form when one side is an ordered
     * subset of the other and both end with the same surname.
     */
    private static String longerSameName(String left, String right) {
        List<String> a = nameWords(left);
        List<String> b = nameWords(right);
        if (a.size() < 2 || b.size() < 2) {
            return null;
        }
        if (!wordKey(a.get(a.size() - 1)).equals(wordKey(b.get(b.size() - 1)))) {
            return null;
        }
        if (a.size() < b.size() && isOrderedSubsequence(a, b)) {
            return right;
        }
        if (b.size() < a.size() && isOrderedSubsequence(b, a)) {
            return left;
        }
        return null;
    }

    private static boolean isOrderedSubsequence(List<String> shorter, List<String> longer) {
        int found = 0;
        for (String word : longer) {
            if (found < shorter.size() && wordKey(word).equals(wordKey(shorter.get(found)))) {
                found++;
            }
        }
        return found == shorter.size();
    }

    private static List<String> nameWords(String part) {
        List<String> words = new ArrayList<>();
        for (String token : part.trim().split("\\s+")) {
            if (!token.isEmpty()) {
                words.add(token);
            }
        }
        return words;
    }

    /**
     * {@code John Baptist Scaramelli, Rev. Cardinal Archbishop Manning} names
     * two people. A clerical title on the second side is not a given name.
     */
    private static boolean isAnotherPerson(String left, String right) {
        List<String> a = nameWords(left);
        List<String> b = nameWords(right);
        if (a.size() < 2 || b.size() < 2) {
            return false;
        }
        String title = wordKey(b.get(0));
        if (!CLERICAL_TITLES.contains(title)) {
            return false;
        }
        return !wordKey(a.get(a.size() - 1)).equals(wordKey(b.get(b.size() - 1)));
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

    /**
     * A postnominal with no spaces whose letters are all capitals, two to six
     * of them, periods allowed: {@code SJ}, {@code S.J.}, {@code OCD}, {@code D.D.}
     * A word with a lowercase letter ({@code Joseph}) is not one. {@code Inc} is listed separately.
     */
    private static boolean isPostnominalInitialism(String part) {
        int letters = 0;
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (Character.isLetter(c)) {
                if (!Character.isUpperCase(c)) {
                    return false;
                }
                letters++;
            } else if (c != '.') {
                return false;
            }
        }
        return letters >= 2 && letters <= 6;
    }

    /** {@code S. J.} after a given name. Spaced initials in the given-name slot are not suffixes. */
    private static boolean isSpacedInitials(String part) {
        return SPACED_INITIALS.matcher(part).matches();
    }

    private static boolean isSuffix(String part) {
        if (part.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        String key = part.replace(".", "").toLowerCase(Locale.ROOT);
        if (SUFFIXES.contains(key)) {
            return true;
        }
        if (isPostnominalInitialism(part)) {
            return true;
        }
        if (ROMAN_DENY.contains(key) || key.length() < 2 || key.length() > 6) {
            return false;
        }
        return ROMAN.matcher(key).matches();
    }

    /**
     * Initial capitals for a name whose letters are all upper or all lower.
     * A mixed-case name is returned unchanged, so {@code McFadden} and
     * {@code de la} stay as stored.
     */
    private static String normalizeUniformCase(String value) {
        if (!isUniformLetterCase(value)) {
            return value;
        }
        String[] tokens = value.split(" ");
        StringBuilder out = new StringBuilder();
        String previous = "";
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(caseToken(tokens[i], i == 0, previous));
            previous = wordKey(tokens[i]);
        }
        return out.toString();
    }

    private static boolean isUniformLetterCase(String value) {
        Boolean upper = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isLetter(c)) {
                continue;
            }
            boolean isUpper = Character.isUpperCase(c);
            boolean isLower = Character.isLowerCase(c);
            if (!isUpper && !isLower) {
                continue;
            }
            if (upper == null) {
                upper = isUpper;
            } else if (upper != isUpper) {
                return false;
            }
        }
        return upper != null;
    }

    private static String caseToken(String token, boolean first, String previous) {
        if (token.indexOf('-') < 0) {
            return caseTokenPart(token, first, previous);
        }
        String[] parts = token.split("-", -1);
        StringBuilder out = new StringBuilder();
        String prev = previous;
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                out.append('-');
            }
            out.append(caseTokenPart(parts[i], first && i == 0, prev));
            prev = wordKey(parts[i]);
        }
        return out.toString();
    }

    private static String caseTokenPart(String token, boolean first, String previous) {
        if (token.isEmpty()) {
            return token;
        }
        String roman = romanNameToken(token);
        if (roman != null) {
            return roman;
        }
        String dotted = formatWrappedDotted(token);
        if (dotted != null) {
            return dotted;
        }
        String french = frenchParticle(token);
        if (french != null) {
            return french;
        }
        String key = wordKey(token);
        if (KEEP_UPPER.contains(key)) {
            return uppercaseLetters(token);
        }
        if (!first && isNameSmall(key, previous)) {
            return lowercaseLetters(token);
        }
        return fixMc(capitalizeLike(token));
    }

    /** {@code XIV} and {@code IV.} stay numerals. {@code Cl.} does not. */
    private static String romanNameToken(String token) {
        Matcher match = ROMAN_TOKEN.matcher(token);
        if (!match.matches()) {
            return null;
        }
        String key = match.group(2).toLowerCase(Locale.ROOT);
        if (key.length() < 2 || ROMAN_DENY.contains(key) || !ROMAN.matcher(key).matches()) {
            return null;
        }
        if (key.length() == 2 && match.group(3).indexOf('.') >= 0 && !REGNAL_TWO.contains(key)) {
            return null;
        }
        return match.group(1) + key.toUpperCase(Locale.ROOT) + match.group(3);
    }

    private static boolean isNameSmall(String key, String previous) {
        if (NAME_SMALL.contains(key)) {
            return true;
        }
        return ("la".equals(key) || "le".equals(key)) && "de".equals(previous);
    }

    /** {@code D.D.,} keeps the abbreviation and the punctuation around it. */
    private static String formatWrappedDotted(String token) {
        Matcher match = Pattern.compile("^([^A-Za-z.]*)([A-Za-z][A-Za-z.]*)([^A-Za-z.]*)$").matcher(token);
        if (!match.matches() || match.group(2).indexOf('.') < 0 || !isDottedAbbreviation(match.group(2))) {
            return null;
        }
        return match.group(1) + formatDotted(match.group(2)) + match.group(3);
    }

    private static boolean isDottedAbbreviation(String token) {
        if (token.indexOf('.') < 0) {
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
                out.append(part.toUpperCase(Locale.ROOT));
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

    /** {@code d'Arc}. The particle stays lower. */
    private static String frenchParticle(String token) {
        Matcher match = FRENCH_PARTICLE.matcher(token);
        if (!match.matches()) {
            return null;
        }
        return match.group(1) + match.group(2).toLowerCase(Locale.ROOT) + match.group(3)
                + Character.toUpperCase(match.group(4).charAt(0))
                + match.group(5).toLowerCase(Locale.ROOT);
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

    private static String lowercaseLetters(String token) {
        StringBuilder out = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            out.append(Character.isLetter(c) ? Character.toLowerCase(c) : c);
        }
        return out.toString();
    }

    /** Capitalizes the first letter and the letter after a one-letter apostrophe ({@code O'Brien}). */
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

    private static String wordKey(String token) {
        return token.replaceAll("^[^\\p{L}\\p{N}]+", "")
                .replaceAll("[^\\p{L}\\p{N}]+$", "")
                .toLowerCase(Locale.ROOT);
    }
}
