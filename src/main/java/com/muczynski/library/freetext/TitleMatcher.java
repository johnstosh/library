/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for matching book titles with various normalization strategies.
 * Handles case differences, subtitles, articles, punctuation, and common abbreviations
 * such as Fr./Father and St./Saint.
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

    /**
     * Title abbreviations expanded to their full forms so "Fr. Brown" matches "Father Brown"
     * and "St. Augustine" matches "Saint Augustine". Applied after punctuation is stripped.
     */
    private static final Map<String, String> ABBREVIATIONS = Map.of(
            "fr", "father",
            "st", "saint"
    );

    private static final Pattern ABBREVIATION_PATTERN = Pattern.compile("\\b(fr|st)\\b");

    /**
     * Words that introduce a continuation of the same work ("and other stories",
     * "and selected writings"), not a second independently titled work.
     */
    private static final Set<String> CONTINUATION_WORDS = Set.of(
            "other", "others", "selected", "selections", "complete", "including",
            "volume", "vol", "part", "parts", "excerpt", "excerpts", "abridged",
            "unabridged", "revised", "edition", "supplement", "appendix"
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

        // Remove punctuation, convert to lowercase, expand Fr./St., split into words
        String[] words = expandAbbreviations(stripAccents(title).toLowerCase()
                .replace("&", " ")
                .replaceAll("[^a-z0-9\\s]", "")
                .trim())
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
     * Normalize a title for prefix (starts-with) API searches such as LibriVox.
     * <p>
     * LibriVox stores leading articles in a separate {@code title_prefix} column, so the
     * API {@code title} field is typically "Wisdom of Father Brown" not "The Wisdom of
     * Father Brown". Prefix match ({@code title=^...}) is SQL {@code LIKE 'term%'} with
     * literal spaces, so middle words like "of" and "and" must be kept. Keyword-style
     * stop-word stripping ({@link #normalizeForSearch}) would make "wisdom father brown"
     * which does not prefix-match "Wisdom of Father Brown".
     * <p>
     * Example: "The Wisdom of Father Brown" -> "wisdom of father brown"
     * Example: "A Tale of Two Cities" -> "tale of two cities"
     * Example: "War and Peace" -> "war and peace"
     *
     * @param title the original title
     * @return normalized title suitable for prefix API searches
     */
    public static String normalizeForPrefixSearch(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        String main = title;
        int colonIndex = main.indexOf(':');
        if (colonIndex > 0) {
            main = main.substring(0, colonIndex);
        }

        String normalized = expandAbbreviations(main.toLowerCase()
                .replaceAll("\\s*\\([^)]*\\)\\s*$", "")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim());

        return normalized.replaceFirst("^(the|a|an)(\\s+|$)", "");
    }

    /**
     * Check if two titles match, accounting for:
     * - Case differences
     * - Subtitle variations (text after colon)
     * - Articles (The, A, An)
     * - Punctuation differences
     * - Fr./Father and St./Saint abbreviations
     *
     * @param candidate the title found from the provider
     * @param target    the title we're searching for
     * @return true if the titles match
     */
    public static boolean titleMatches(String candidate, String target) {
        if (candidate == null || target == null) {
            return false;
        }

        String normalizedCandidate = normalizeForComparison(candidate);
        String normalizedTarget = normalizeForComparison(target);
        if (normalizedCandidate.isEmpty() || normalizedTarget.isEmpty()) {
            return false;
        }

        // Exact match after normalization
        if (normalizedCandidate.equals(normalizedTarget)) {
            return true;
        }

        // Split on the subtitle separator of the ORIGINAL text. Normalization removes
        // colons, so the main title has to be taken before normalizing.
        String candidateMain = normalizeForComparison(mainTitle(candidate));
        String targetMain = normalizeForComparison(mainTitle(target));
        String candidateSub = normalizeForComparison(subtitle(candidate));
        String targetSub = normalizeForComparison(subtitle(target));

        // Two different subtitles on the same main title are usually different books,
        // e.g. "Jesus of Nazareth: The Infancy Narratives" and
        // "Jesus of Nazareth: From the Baptism in the Jordan to the Transfiguration".
        if (!candidateSub.isEmpty() && !targetSub.isEmpty()
                && !subtitlesCompatible(candidateSub, targetSub)) {
            return false;
        }

        // Main title match when at most one side has a subtitle, or the subtitles agree
        if (!candidateMain.isEmpty() && candidateMain.equals(targetMain)) {
            return true;
        }

        // "Treatise on the Love of God & Treatise on the Priesthood" is two works.
        // A prefix, a buried phrase ("on the priesthood"), or a 70% word overlap
        // must not attach one of those works to the other, or to a different author
        // who used a similar title.
        if (isJoinedWorks(candidate) || isJoinedWorks(target)) {
            return false;
        }

        // Prefix match: if one title starts with the other, it's likely the same book
        // with different subtitle (e.g., "War and Peace" vs "War and Peace: Complete Edition").
        // An interior substring is not a match: "On the Priesthood" sits inside
        // "Treatise on the Love of God & Treatise on the Priesthood" and is a different book.
        if (normalizedCandidate.startsWith(normalizedTarget) || normalizedTarget.startsWith(normalizedCandidate)) {
            // Only accept if the shorter one has at least 2 significant words
            // to prevent single words from matching everything
            String shorter = normalizedCandidate.length() <= normalizedTarget.length() ? normalizedCandidate : normalizedTarget;
            if (countSignificantWords(shorter.split("\\s+")) >= 2) {
                return true;
            }
        }

        // Word-based matching with bidirectional verification on the whole titles.
        // Not on main titles alone: "Paradise of the Desert Fathers" would then pass
        // for "The Sayings of the Desert Fathers: The Alphabetical Collection".
        return wordsMatch(normalizedCandidate, normalizedTarget);
    }

    /**
     * Same work with no fuzzy overlap: normalized titles are equal, or the main
     * titles are equal and the subtitles do not name two different books.
     * Used when a provider cannot see the author of a hit.
     */
    public static boolean sameTitle(String candidate, String target) {
        if (candidate == null || target == null) {
            return false;
        }
        String normalizedCandidate = normalizeForComparison(candidate);
        String normalizedTarget = normalizeForComparison(target);
        if (normalizedCandidate.isEmpty() || normalizedTarget.isEmpty()) {
            return false;
        }
        if (normalizedCandidate.equals(normalizedTarget)) {
            return true;
        }
        String candidateSub = normalizeForComparison(subtitle(candidate));
        String targetSub = normalizeForComparison(subtitle(target));
        if (!candidateSub.isEmpty() && !targetSub.isEmpty()
                && !subtitlesCompatible(candidateSub, targetSub)) {
            return false;
        }
        String candidateMain = normalizeForComparison(mainTitle(candidate));
        String targetMain = normalizeForComparison(mainTitle(target));
        return !candidateMain.isEmpty() && candidateMain.equals(targetMain);
    }

    /**
     * A hit may be kept when the titles are the same work and, if both sides name
     * an author, the authors are the same person. A provider that cannot see the
     * hit's author may keep only a {@link #sameTitle} match.
     */
    public static boolean accepts(String candidateTitle, String candidateAuthor,
                                  String targetTitle, String targetAuthor) {
        if (!titleMatches(candidateTitle, targetTitle)) {
            return false;
        }
        boolean wantAuthor = targetAuthor != null && !targetAuthor.isBlank();
        boolean haveAuthor = candidateAuthor != null && !candidateAuthor.isBlank();
        if (wantAuthor && haveAuthor) {
            return authorMatches(candidateAuthor, targetAuthor);
        }
        if (wantAuthor) {
            return sameTitle(candidateTitle, targetTitle);
        }
        return true;
    }

    /**
     * Author slug from a URL path, such as CCEL {@code ccel/schaff/npnf109}.
     * Accepts a last name contained in the slug ({@code chrysostom} in
     * {@code johnchrysostom}) as well as a normal author match.
     */
    public static boolean authorMatchesSlug(String slug, String authorName) {
        if (slug == null || authorName == null || slug.isBlank() || authorName.isBlank()) {
            return false;
        }
        if (authorMatches(slug, authorName)) {
            return true;
        }
        String slugNorm = stripAccents(slug).toLowerCase().replaceAll("[^a-z]", "");
        String authorNorm = normalizeAuthor(authorName).replace(" ", "");
        if (slugNorm.isEmpty() || authorNorm.isEmpty()) {
            return false;
        }
        if (slugNorm.equals(authorNorm)) {
            return true;
        }
        String lastName = getLastName(normalizeAuthor(authorName));
        return lastName.length() > 2 && slugNorm.contains(lastName);
    }

    /**
     * True when a title joins two independently named works, as in
     * "Treatise on the Love of God &amp; Treatise on the Priesthood".
     * "War and Peace" is one title. "The Brothers Karamazov and Other Stories"
     * is a continuation, not a second work.
     */
    static boolean isJoinedWorks(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        String clean = title.replaceAll("\\s+/\\s+.*$", "");
        int colon = clean.indexOf(':');
        if (colon > 0) {
            clean = clean.substring(0, colon);
        }
        return joinedOnAmpersand(clean) || joinedOnWord(clean, "and");
    }

    private static boolean joinedOnAmpersand(String title) {
        String[] parts = title.split("\\s*&\\s*");
        if (parts.length < 2) {
            return false;
        }
        for (int i = 0; i < parts.length - 1; i++) {
            String right = parts[i + 1];
            if (bothSubstantial(parts[i], right) && !continuation(right)) {
                return true;
            }
        }
        return false;
    }

    private static boolean joinedOnWord(String title, String word) {
        String[] parts = title.split("(?i)\\s+" + word + "\\s+");
        if (parts.length < 2) {
            return false;
        }
        for (int i = 0; i < parts.length - 1; i++) {
            String left = parts[i];
            String right = String.join(" " + word + " ", Arrays.copyOfRange(parts, i + 1, parts.length));
            if (bothSubstantial(left, right) && !continuation(right)) {
                return true;
            }
        }
        return false;
    }

    private static boolean bothSubstantial(String left, String right) {
        return significantCount(left) >= 2 && significantCount(right) >= 2;
    }

    private static int significantCount(String text) {
        String normalized = normalizeForComparison(text);
        if (normalized.isEmpty()) {
            return 0;
        }
        return countSignificantWords(normalized.split("\\s+"));
    }

    private static boolean continuation(String right) {
        String normalized = normalizeForComparison(right);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String word : normalized.split("\\s+")) {
            if (isSignificantWord(word)) {
                return CONTINUATION_WORDS.contains(word);
            }
        }
        return false;
    }

    /**
     * True when two normalized subtitles describe the same book: one contains the
     * other, or their significant words agree in both directions.
     */
    private static boolean subtitlesCompatible(String a, String b) {
        return a.contains(b) || b.contains(a) || wordsMatch(a, b);
    }

    /**
     * Normalize a title for comparison:
     * - Convert to lowercase and strip accents ("Brontë" becomes "bronte")
     * - Drop a trailing statement of responsibility ("The seven virtues / by Fulton J. Sheen")
     * - Remove trailing parenthetical content (dates, editions, etc.)
     * - Remove leading articles (the, a, an)
     * - Turn "&" into "and", then remove punctuation except spaces
     * - Expand Fr./St. abbreviations
     * - Collapse multiple spaces
     */
    public static String normalizeForComparison(String title) {
        if (title == null) {
            return "";
        }
        return expandAbbreviations(stripAccents(title).toLowerCase()
                .replaceAll("\\s+/\\s+.*$", "")           // Remove " / by Author" catalog suffix
                .replaceAll("[\\s.,;:]+$", "")
                .replaceAll("\\s*\\([^)]*\\)\\s*$", "") // Remove trailing (date), (edition), etc.
                .replaceAll("^(the|a|an)\\s+", "")
                .replace("&", " and ")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim());
    }

    /**
     * Remove diacritical marks so "François Mauriac" compares equal to "Francois Mauriac".
     */
    public static String stripAccents(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /**
     * Expand standalone title abbreviations to their full forms.
     * "fr" becomes "father" and "st" becomes "saint". Safe to call after punctuation
     * has already been stripped, so "Fr." has already become "fr".
     */
    static String expandAbbreviations(String title) {
        if (title == null || title.isBlank()) {
            return title == null ? "" : title;
        }
        Matcher matcher = ABBREVIATION_PATTERN.matcher(title);
        StringBuilder expanded = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(expanded,
                    Matcher.quoteReplacement(ABBREVIATIONS.get(matcher.group(1))));
        }
        matcher.appendTail(expanded);
        return expanded.toString();
    }

    /**
     * The main title of an un-normalized title: the text before the first colon,
     * or the whole title when there is no colon. A " / by ..." statement of
     * responsibility is dropped first.
     */
    public static String mainTitle(String title) {
        if (title == null) {
            return "";
        }
        String clean = title.replaceAll("\\s+/\\s+.*$", "");
        int colonIndex = clean.indexOf(':');
        return colonIndex > 0 ? clean.substring(0, colonIndex).trim() : clean.trim();
    }

    /**
     * The subtitle of an un-normalized title: the text after the first colon, or "".
     */
    static String subtitle(String title) {
        if (title == null) {
            return "";
        }
        String clean = title.replaceAll("\\s+/\\s+.*$", "");
        int colonIndex = clean.indexOf(':');
        return colonIndex > 0 ? clean.substring(colonIndex + 1).trim() : "";
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
        if (normalizedCandidate.isEmpty() || normalizedTarget.isEmpty()) {
            return false;
        }

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
     * Normalize an author name for comparison. Catalog forms are put into
     * "first last" order before punctuation is removed, so
     * "Sheen, Fulton J. (Fulton John), 1895-1979" becomes "fulton j sheen" and
     * "Leslie, Shane, 1885-1971" becomes "shane leslie". Accents are stripped.
     */
    static String normalizeAuthor(String author) {
        String name = stripAccents(author)
                .replaceAll("\\([^)]*\\)", " ")                 // (Fulton John)
                .replaceAll("\\b(ca|fl|b|d)\\.?\\s*\\d.*$", " ")  // ca. 1217-1274
                .replaceAll("\\d{3,4}\\??\\s*-.*$", " ")          // 1895-1979, 1865-
                .replaceAll("\\d{3,4}.*$", " ");
        String[] parts = name.split(",");
        if (parts.length >= 2 && !parts[0].isBlank() && !parts[1].isBlank()
                && parts[0].trim().split("\\s+").length <= 3) {
            name = parts[1].trim() + " " + parts[0].trim();
        }
        return name.toLowerCase()
                .replaceAll("[^a-z\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Extract the last name from an author string.
     * Handles both "First Last" and "Last, First" formats.
     */
    private static String getLastName(String author) {
        // normalizeAuthor already put "Last, First" into "first last" order
        String[] parts = author.split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : author;
    }
}
