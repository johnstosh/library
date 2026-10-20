/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Explains why the two titles or the two authors on a duplicate-title pair differ.
 * A one- or two-word difference that is only an article, preposition, or conjunction
 * is named. A one-letter misspelling is named. A match that differs only by letter
 * case is named. A middle name that is present on only one author is named. When an
 * alternate title or alternate name is present, the sentence says which fields were compared.
 */
public final class DuplicateTitleDifference {

    private static final Set<String> ARTICLES = Set.of("a", "an", "the");

    private static final Set<String> PREPOSITIONS = Set.of(
            "of", "in", "on", "to", "for", "by", "at", "from", "with", "as", "into", "onto",
            "over", "after", "before", "between", "under", "about", "against", "among",
            "through", "during", "without", "within", "across", "around", "behind",
            "beyond", "despite", "except", "toward", "towards", "upon", "via", "per", "vs");

    private static final Set<String> CONJUNCTIONS = Set.of(
            "and", "or", "but", "nor", "yet", "so", "if");

    private DuplicateTitleDifference() {
    }

    public static String titles(String matchedA, String matchedB, String fieldA, String fieldB,
                                 String bookAAlternate, String bookBAlternate) {
        String body = compareTitles(matchedA, matchedB);
        String left = fieldA == null || fieldA.isBlank() ? "title" : fieldA;
        String right = fieldB == null || fieldB.isBlank() ? "title" : fieldB;
        boolean alternate = notBlank(bookAAlternate) || notBlank(bookBAlternate)
                || "alternate title".equals(left) || "alternate title".equals(right);
        if (!alternate) {
            return body;
        }
        return "Compared Book A " + left + " with Book B " + right + ". " + body;
    }

    public static String authors(Long authorAId, String nameA, List<String> alternatesA,
                                  Long authorBId, String nameB, List<String> alternatesB) {
        if (authorAId != null && authorAId.equals(authorBId)) {
            return "Same author record.";
        }
        List<String> altsA = clean(alternatesA);
        List<String> altsB = clean(alternatesB);
        if (!notBlank(nameA) && !notBlank(nameB) && altsA.isEmpty() && altsB.isEmpty()) {
            return "Neither book has an author.";
        }
        String primary = compareAuthorFields(
                nameA, nameB, "Book A author name", "Book B author name");
        if (altsA.isEmpty() && altsB.isEmpty()) {
            return primary;
        }
        String match = bestAlternateMatch(nameA, altsA, nameB, altsB);
        if (match != null) {
            return primary + " " + match;
        }
        return primary + " Compared Book A author name with Book B author name.";
    }

    static String compareTitles(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        if (a.equals(b)) {
            return a.isEmpty() ? "Titles are blank." : "Titles match exactly.";
        }
        if (a.equalsIgnoreCase(b)) {
            return "Titles match case-insensitively.";
        }
        String lettersA = lettersOnly(a);
        String lettersB = lettersOnly(b);
        if (lettersA.equals(lettersB)) {
            return "Titles match except for spacing or punctuation.";
        }
        String functionWords = functionWordDifference(a, b);
        if (functionWords != null) {
            return functionWords;
        }
        if (levenshtein(lettersA, lettersB) == 1) {
            return "Titles differ by a one-letter misspelling.";
        }
        return "Titles differ in more than a function word or a single letter.";
    }

    static String compareAuthorFields(String left, String right, String leftField, String rightField) {
        if (!notBlank(left) && !notBlank(right)) {
            return "Author names are blank.";
        }
        if (!notBlank(left) || !notBlank(right)) {
            String missing = notBlank(left) ? rightField : leftField;
            return missing + " is blank.";
        }
        String trimmedLeft = left.trim();
        String trimmedRight = right.trim();
        if (trimmedLeft.equals(trimmedRight)) {
            return "Authors match exactly.";
        }
        if (trimmedLeft.equalsIgnoreCase(trimmedRight)) {
            return "Authors match case-insensitively.";
        }
        if (DuplicateTitleService.normalizeAuthor(left).equals(DuplicateTitleService.normalizeAuthor(right))) {
            return "Authors match except for spacing or punctuation.";
        }
        Middle middle = middleName(left, right);
        if (middle != null) {
            String present = middle.presentOnLeft() ? leftField : rightField;
            String absent = middle.presentOnLeft() ? rightField : leftField;
            return "Authors differ by a middle name: \"" + middle.token()
                    + "\" is present on " + present + " and absent on " + absent + ".";
        }
        return "Authors differ.";
    }

    private static String bestAlternateMatch(String nameA, List<String> altsA,
                                              String nameB, List<String> altsB) {
        List<NamedField> left = fields("Book A", nameA, altsA);
        List<NamedField> right = fields("Book B", nameB, altsB);
        String middle = null;
        for (NamedField a : left) {
            for (NamedField b : right) {
                if (!a.alternate() && !b.alternate()) {
                    continue;
                }
                if (!notBlank(a.value()) || !notBlank(b.value())) {
                    continue;
                }
                String trimmedA = a.value().trim();
                String trimmedB = b.value().trim();
                if (trimmedA.equals(trimmedB)) {
                    return a.label() + " matches " + b.label() + " \"" + trimmedA + "\".";
                }
                if (trimmedA.equalsIgnoreCase(trimmedB)) {
                    return a.label() + " matches " + b.label() + " \"" + trimmedA + "\" case-insensitively.";
                }
                if (DuplicateTitleService.normalizeAuthor(trimmedA)
                        .equals(DuplicateTitleService.normalizeAuthor(trimmedB))) {
                    return a.label() + " matches " + b.label()
                            + " \"" + trimmedA + "\" except for spacing or punctuation.";
                }
                if (middle == null) {
                    Middle diff = middleName(a.value(), b.value());
                    if (diff != null) {
                        String present = diff.presentOnLeft() ? a.label() : b.label();
                        String absent = diff.presentOnLeft() ? b.label() : a.label();
                        middle = a.label() + " and " + b.label()
                                + " differ by a middle name: \"" + diff.token()
                                + "\" is present on " + present + " and absent on " + absent + ".";
                    }
                }
            }
        }
        return middle;
    }

    private static List<NamedField> fields(String book, String name, List<String> alternates) {
        List<NamedField> fields = new ArrayList<>();
        fields.add(new NamedField(book + " author name", name, false));
        for (String alternate : alternates) {
            fields.add(new NamedField(book + " alternate name", alternate, true));
        }
        return fields;
    }

    private static String functionWordDifference(String left, String right) {
        List<String> tokensA = wordTokens(left);
        List<String> tokensB = wordTokens(right);
        if (!frequencies(contentTokens(tokensA)).equals(frequencies(contentTokens(tokensB)))) {
            return null;
        }
        List<String> extras = symmetricExtras(tokensA, tokensB);
        if (extras.isEmpty() || extras.size() > 2) {
            return null;
        }
        for (String extra : extras) {
            if (wordClass(extra) == null) {
                return null;
            }
        }
        return describeFunctionWords(extras);
    }

    private static String describeFunctionWords(List<String> extras) {
        String first = extras.get(0);
        String firstClass = wordClass(first);
        if (extras.size() == 1) {
            return "Titles differ by the " + firstClass + " \"" + first + "\".";
        }
        String second = extras.get(1);
        String secondClass = wordClass(second);
        if (firstClass.equals(secondClass)) {
            return "Titles differ by two " + plural(firstClass) + ": \"" + first + "\" and \"" + second + "\".";
        }
        return "Titles differ by the " + firstClass + " \"" + first
                + "\" and the " + secondClass + " \"" + second + "\".";
    }

    private static String plural(String wordClass) {
        return switch (wordClass) {
            case "article" -> "articles";
            case "preposition" -> "prepositions";
            case "conjunction" -> "conjunctions";
            default -> wordClass;
        };
    }

    private static String wordClass(String token) {
        if (ARTICLES.contains(token)) {
            return "article";
        }
        if (PREPOSITIONS.contains(token)) {
            return "preposition";
        }
        if (CONJUNCTIONS.contains(token)) {
            return "conjunction";
        }
        return null;
    }

    private static List<String> contentTokens(List<String> tokens) {
        List<String> content = new ArrayList<>();
        for (String token : tokens) {
            if (wordClass(token) == null) {
                content.add(token);
            }
        }
        return content;
    }

    private static List<String> symmetricExtras(List<String> left, List<String> right) {
        Map<String, Integer> a = frequencies(left);
        Map<String, Integer> b = frequencies(right);
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(a.keySet());
        keys.addAll(b.keySet());
        List<String> extras = new ArrayList<>();
        for (String key : keys) {
            int count = Math.abs(a.getOrDefault(key, 0) - b.getOrDefault(key, 0));
            for (int i = 0; i < count; i++) {
                extras.add(key);
            }
        }
        return extras;
    }

    private static Map<String, Integer> frequencies(List<String> tokens) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String token : tokens) {
            counts.merge(token, 1, Integer::sum);
        }
        return counts;
    }

    private static List<String> wordTokens(String value) {
        String spaced = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        if (spaced.isEmpty()) {
            return List.of();
        }
        return List.of(spaced.split("\\s+"));
    }

    private static Middle middleName(String left, String right) {
        List<String> a = wordTokens(left);
        List<String> b = wordTokens(right);
        if (a.size() == b.size() + 1) {
            int skip = extraIndex(b, a);
            if (isMiddle(skip, a.size())) {
                return new Middle(displayToken(left, skip, a.get(skip)), true);
            }
        } else if (b.size() == a.size() + 1) {
            int skip = extraIndex(a, b);
            if (isMiddle(skip, b.size())) {
                return new Middle(displayToken(right, skip, b.get(skip)), false);
            }
        }
        return null;
    }

    /** Index in {@code longer} of the single token that {@code shorter} does not have, or -1. */
    private static int extraIndex(List<String> shorter, List<String> longer) {
        int i = 0;
        int skip = -1;
        for (int j = 0; j < longer.size(); j++) {
            if (i < shorter.size() && shorter.get(i).equals(longer.get(j))) {
                i++;
            } else if (skip == -1) {
                skip = j;
            } else {
                return -1;
            }
        }
        if (i != shorter.size()) {
            return -1;
        }
        return skip == -1 ? longer.size() - 1 : skip;
    }

    private static boolean isMiddle(int index, int longerSize) {
        return index > 0 && index < longerSize - 1;
    }

    private static String displayToken(String original, int index, String fallback) {
        if (original == null) {
            return fallback;
        }
        String[] parts = original.trim().split("\\s+");
        if (index >= 0 && index < parts.length) {
            return parts[index];
        }
        return fallback;
    }

    private static String lettersOnly(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    static int levenshtein(String s1, String s2) {
        if (s1.equals(s2)) {
            return 0;
        }
        int len1 = s1.length();
        int len2 = s2.length();
        int[] prev = new int[len2 + 1];
        int[] curr = new int[len2 + 1];
        for (int j = 0; j <= len2; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= len1; i++) {
            curr[0] = i;
            char c1 = s1.charAt(i - 1);
            for (int j = 1; j <= len2; j++) {
                int cost = c1 == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[len2];
    }

    private static List<String> clean(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            if (notBlank(value)) {
                cleaned.add(value.trim());
            }
        }
        return cleaned;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private record Middle(String token, boolean presentOnLeft) {
    }

    private record NamedField(String label, String value, boolean alternate) {
    }
}
