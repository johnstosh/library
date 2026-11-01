/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.AuthorNames;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookTitles;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Decides what a Duplicate Titles merge or fix will change, and writes the
 * sentence the librarian sees before and after the click.
 * <p>
 * A longer biography, affiliation, country, nationality, or Grokipedia URL is
 * kept. The author name is not chosen by length. A middle name that one author
 * has and the other lacks becomes the kept name, and the shorter form is stored
 * as an alternate name. Any other spelling stays as an alternate name on the
 * kept author. Two call numbers are never written into one field. Free-text
 * URLs are combined into the kept book's URL field.
 */
public final class DuplicateTitlePlans {

    public static final String MERGE_TITLES = "MERGE_TITLES";
    public static final String FIX_TITLES = "FIX_TITLES";
    public static final String MERGE_AUTHORS = "MERGE_AUTHORS";

    private DuplicateTitlePlans() {
    }

    public record AuthorSide(long id, String name, List<String> alternateNames) {
    }

    public record AuthorMergePlan(
            boolean available,
            Long keeperId,
            Long sourceId,
            String survivingName,
            List<String> alternateNames,
            String summary) {
    }

    public record TitleSide(
            long id,
            String title,
            List<String> alternateTitles,
            String locNumber,
            boolean electronic,
            String freeTextUrl) {
    }

    public record TitlePlan(
            String actionKind,
            boolean changeNeeded,
            Long keeperId,
            Long deletedId,
            String summary,
            long leftId,
            long rightId,
            String leftTitle,
            String rightTitle,
            List<String> leftAlternate,
            List<String> rightAlternate,
            String keeperFreeTextUrl,
            boolean keeperElectronic,
            List<String> keeperAlternate) {
    }

    public static AuthorMergePlan authorMerge(AuthorSide a, AuthorSide b) {
        if (a == null || b == null || a.id() <= 0 || b.id() <= 0) {
            return new AuthorMergePlan(false, null, null, null, List.of(),
                    "One book has no author, so Merge authors is not offered.");
        }
        if (a.id() == b.id()) {
            return new AuthorMergePlan(false, a.id(), null, a.name(), List.of(),
                    "Same author record, so Merge authors is not offered.");
        }
        AuthorSide keeper = a.id() <= b.id() ? a : b;
        AuthorSide source = keeper == a ? b : a;
        String surviving = blankToNull(keeper.name());
        MiddlePick middle = middlePick(a.name(), b.name());
        if (middle != null) {
            AuthorSide fuller = middle.fullerIsA() ? a : b;
            AuthorSide shorter = fuller == a ? b : a;
            keeper = fuller;
            source = shorter;
            surviving = blankToNull(fuller.name());
        }
        List<String> raw = new ArrayList<>();
        raw.add(a.name());
        raw.add(b.name());
        if (a.alternateNames() != null) {
            raw.addAll(a.alternateNames());
        }
        if (b.alternateNames() != null) {
            raw.addAll(b.alternateNames());
        }
        List<String> alternates = AuthorNames.normalize(surviving, raw);
        if (alternates == null) {
            alternates = List.of();
        }
        return new AuthorMergePlan(true, keeper.id(), source.id(), surviving, alternates,
                authorSummary(keeper, source, surviving, middle != null));
    }

    public static TitlePlan titles(TitleSide left, TitleSide right) {
        boolean bothCallNumbers = DuplicateCatalogLabels.hasCallNumber(left.locNumber())
                && DuplicateCatalogLabels.hasCallNumber(right.locNumber());
        if (bothCallNumbers) {
            return fix(left, right);
        }
        return merge(left, right);
    }

    public static String quote(String value) {
        return value == null ? "" : value;
    }

    private static TitlePlan merge(TitleSide left, TitleSide right) {
        TitleSide keeper;
        TitleSide dropped;
        if (DuplicateCatalogLabels.hasCallNumber(left.locNumber())
                && !DuplicateCatalogLabels.hasCallNumber(right.locNumber())) {
            keeper = left;
            dropped = right;
        } else if (DuplicateCatalogLabels.hasCallNumber(right.locNumber())
                && !DuplicateCatalogLabels.hasCallNumber(left.locNumber())) {
            keeper = right;
            dropped = left;
        } else if (left.id() <= right.id()) {
            keeper = left;
            dropped = right;
        } else {
            keeper = right;
            dropped = left;
        }
        String urls = combineUrls(keeper.freeTextUrl(), dropped.freeTextUrl());
        boolean electronic = keeper.electronic() || dropped.electronic();
        List<String> alternate = combinedAlternates(keeper, dropped);
        String alternateNote = alternateNote(keeper, alternate);
        String summary = mergeSummary(keeper, dropped, urls, electronic, alternateNote);
        return new TitlePlan(
                MERGE_TITLES,
                true,
                keeper.id(),
                dropped.id(),
                summary,
                left.id(),
                right.id(),
                left.title(),
                right.title(),
                left.id() == keeper.id() ? alternate : copyTitles(left.alternateTitles()),
                right.id() == keeper.id() ? alternate : copyTitles(right.alternateTitles()),
                urls,
                electronic,
                alternate);
    }

    private static TitlePlan fix(TitleSide left, TitleSide right) {
        TitleSide first = left.id() <= right.id() ? left : right;
        TitleSide second = first == left ? right : left;
        String wordFirst = Book.stripCopySuffix(nullToEmpty(first.title()));
        String wordSecond = Book.stripCopySuffix(nullToEmpty(second.title()));
        String shared = sharedWording(wordFirst, wordSecond);
        if (shared != null) {
            wordFirst = shared;
            wordSecond = shared;
        }
        Integer firstNumber = copyNumber(first.title());
        Integer secondNumber = copyNumber(second.title());
        String newFirst = firstNumber == null ? wordFirst : withCopy(wordFirst, firstNumber);
        String newSecond;
        if (secondNumber != null) {
            newSecond = withCopy(wordSecond, secondNumber);
        } else if (firstNumber == null) {
            newSecond = withCopy(wordSecond, 2);
        } else {
            int used = Math.max(firstNumber, 1);
            newSecond = withCopy(wordSecond, used + 1);
        }
        List<String> altFirst = alternateAfterTitleChange(first.alternateTitles(), first.title(), newFirst);
        List<String> altSecond = alternateAfterTitleChange(second.alternateTitles(), second.title(), newSecond);
        String leftTitle = left.id() == first.id() ? newFirst : newSecond;
        String rightTitle = right.id() == first.id() ? newFirst : newSecond;
        List<String> leftAlt = left.id() == first.id() ? altFirst : altSecond;
        List<String> rightAlt = right.id() == first.id() ? altFirst : altSecond;
        boolean changed = !same(left.title(), leftTitle) || !same(right.title(), rightTitle)
                || !sameTitles(left.alternateTitles(), leftAlt) || !sameTitles(right.alternateTitles(), rightAlt);
        String summary = fixSummary(left, right, leftTitle, rightTitle, leftAlt, rightAlt);
        return new TitlePlan(
                FIX_TITLES,
                changed,
                null,
                null,
                summary,
                left.id(),
                right.id(),
                leftTitle,
                rightTitle,
                leftAlt,
                rightAlt,
                null,
                false,
                null);
    }

    public static String fixSummary(TitleSide left, TitleSide right,
                                     String leftTitle, String rightTitle,
                                     List<String> leftAlternate, List<String> rightAlternate) {
        StringBuilder text = new StringBuilder();
        text.append("Fix titles does not delete either book. ");
        text.append(titleChange(left.id(), left.title(), leftTitle));
        text.append(titleChange(right.id(), right.title(), rightTitle));
        text.append(alternateChange(left.id(), left.alternateTitles(), leftAlternate));
        text.append(alternateChange(right.id(), right.alternateTitles(), rightAlternate));
        text.append("Call number \"").append(quote(left.locNumber())).append("\" stays on book #")
                .append(left.id()).append(". Call number \"").append(quote(right.locNumber()))
                .append("\" stays on book #").append(right.id())
                .append(". Free-text URLs stay. No call number is combined into one field.");
        return text.toString();
    }

    private static String authorSummary(AuthorSide keeper, AuthorSide source, String surviving, boolean middleName) {
        StringBuilder text = new StringBuilder();
        text.append("Merge authors keeps author #").append(keeper.id())
                .append(" \"").append(quote(surviving))
                .append("\" and deletes author #").append(source.id())
                .append(" \"").append(quote(source.name())).append("\". ");
        if (middleName && surviving != null && !surviving.equals(keeper.name())) {
            text.append("Author #").append(keeper.id())
                    .append(" name changes from \"").append(quote(keeper.name()))
                    .append("\" to \"").append(surviving)
                    .append("\" because that form includes the middle name. ");
        }
        if (middleName) {
            String shorter = keeper.id() == source.id() ? "" : quote(source.name());
            text.append("\"").append(shorter).append("\" is stored as an alternate name. ");
        } else if (source.name() != null && surviving != null
                && source.name().trim().equalsIgnoreCase(surviving)
                && !source.name().trim().equals(surviving)) {
            text.append("Author names match case-insensitively. The kept capitalization is \"")
                    .append(surviving)
                    .append("\". The other capitalization is not stored as an alternate name. ");
        } else if (source.name() != null && surviving != null
                && !DuplicateTitleService.normalizeAuthor(source.name())
                .equals(DuplicateTitleService.normalizeAuthor(surviving))) {
            text.append("The kept author name stays \"").append(surviving)
                    .append("\". \"").append(source.name().trim())
                    .append("\" is stored as an alternate name. The longer spelling is not chosen. ");
        } else {
            text.append("Author names already match. Alternate names are combined. ");
        }
        text.append("The longer biography, religious affiliation, birth country, nationality, and Grokipedia URL are kept. ");
        text.append("A missing birth or death date is filled from the deleted author. ");
        text.append("Books, portraits, and favorites move to author #").append(keeper.id()).append(".");
        return text.toString();
    }

    private static String mergeSummary(TitleSide keeper, TitleSide dropped, String urls,
                                        boolean electronic, String alternateNote) {
        StringBuilder text = new StringBuilder();
        text.append("Merge titles keeps book #").append(keeper.id())
                .append(" \"").append(quote(keeper.title()))
                .append("\" and deletes book #").append(dropped.id())
                .append(" \"").append(quote(dropped.title())).append("\". ");
        if (DuplicateCatalogLabels.hasCallNumber(keeper.locNumber())) {
            text.append("Call number \"").append(keeper.locNumber().trim())
                    .append("\" stays on book #").append(keeper.id()).append(". ");
        } else {
            text.append("Neither book has a call number. ");
        }
        text.append("No second call number is written. ");
        if (urls == null || urls.isBlank()) {
            text.append("Neither book has a free-text URL. ");
        } else {
            text.append("Free-text URL on book #").append(keeper.id())
                    .append(" becomes \"").append(urls).append("\". ");
        }
        if (electronic && !keeper.electronic()) {
            text.append("Book #").append(keeper.id()).append(" is marked as an electronic resource. ");
        }
        text.append(alternateNote);
        if (alternateNote.length() > 0 && !alternateNote.endsWith(" ")) {
            text.append(' ');
        }
        text.append("Loans, photos, prices, and favorites move to book #").append(keeper.id())
                .append(" when that book does not already have the same one. A colliding price, loan, or favorite is removed with the deleted book and put back on Undo.");
        return text.toString().replaceAll(" +", " ").trim();
    }

    private static List<String> combinedAlternates(TitleSide keeper, TitleSide dropped) {
        List<String> raw = new ArrayList<>();
        if (keeper.alternateTitles() != null) {
            raw.addAll(keeper.alternateTitles());
        }
        if (dropped.alternateTitles() != null) {
            raw.addAll(dropped.alternateTitles());
        }
        if (notBlank(dropped.title())) {
            raw.add(dropped.title().trim());
        }
        return BookTitles.normalize(keeper.title(), raw);
    }

    private static String alternateNote(TitleSide keeper, List<String> combined) {
        if (sameTitles(keeper.alternateTitles(), combined)) {
            if (combined == null || combined.isEmpty()) {
                return "Alternate titles are unchanged.";
            }
            return "Alternate titles stay " + quoted(combined) + ".";
        }
        return "Alternate titles on book #" + keeper.id() + " are combined as " + quoted(combined) + ".";
    }

    private static String quoted(List<String> titles) {
        if (titles == null || titles.isEmpty()) {
            return "none";
        }
        StringBuilder text = new StringBuilder();
        for (String title : titles) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append('"').append(title).append('"');
        }
        return text.toString();
    }

    private static List<String> copyTitles(List<String> titles) {
        if (titles == null || titles.isEmpty()) {
            return null;
        }
        return new ArrayList<>(titles);
    }

    private static String sharedWording(String baseA, String baseB) {
        if (same(baseA, baseB) || sameIgnoreCase(baseA, baseB)) {
            return baseA == null ? "" : baseA.trim();
        }
        List<String> tokensA = wordTokens(baseA);
        List<String> tokensB = wordTokens(baseB);
        if (isFunctionWordExtension(tokensB, tokensA)) {
            return baseA == null ? "" : baseA.trim();
        }
        if (isFunctionWordExtension(tokensA, tokensB)) {
            return baseB == null ? "" : baseB.trim();
        }
        return null;
    }

    /** True when {@code longer} is {@code shorter} plus only function words. */
    private static boolean isFunctionWordExtension(List<String> shorter, List<String> longer) {
        if (longer.size() <= shorter.size()) {
            return false;
        }
        int extra = 0;
        int i = 0;
        for (String token : longer) {
            if (i < shorter.size() && shorter.get(i).equals(token)) {
                i++;
            } else if (wordClass(token) != null) {
                extra++;
            } else {
                return false;
            }
        }
        return i == shorter.size() && extra >= 1 && extra <= 2;
    }

    private static String titleChange(long id, String before, String after) {
        if (same(before, after)) {
            return "Book #" + id + " title stays \"" + quote(before) + "\". ";
        }
        return "Book #" + id + " title changes from \"" + quote(before) + "\" to \"" + quote(after) + "\". ";
    }

    private static String alternateChange(long id, List<String> before, List<String> after) {
        if (sameTitles(before, after)) {
            return "";
        }
        if ((before == null || before.isEmpty()) && after != null && !after.isEmpty()) {
            return "Book #" + id + " alternate titles are set to " + quoted(after)
                    + " so the previous title is kept. ";
        }
        return "Book #" + id + " alternate titles change from " + quoted(before)
                + " to " + quoted(after) + ". ";
    }

    /**
     * Keeps the existing alternate titles and, when the primary title changes,
     * adds the previous wording when {@link com.muczynski.library.domain.BookTitles#normalize}
     * does not treat it as the same title as the new primary.
     */
    public static List<String> alternateAfterTitleChange(List<String> alternate, String oldTitle, String newTitle) {
        List<String> raw = new ArrayList<>();
        if (alternate != null) {
            raw.addAll(alternate);
        }
        if (!same(oldTitle, newTitle) && oldTitle != null && !oldTitle.isBlank()) {
            raw.add(oldTitle.trim());
        }
        return BookTitles.normalize(newTitle, raw);
    }

    static boolean sameTitles(List<String> left, List<String> right) {
        List<String> a = left == null ? List.of() : left;
        List<String> b = right == null ? List.of() : right;
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!same(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static Integer copyNumber(String title) {
        if (title == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i),\\s*c\\.?\\s*(\\d+)\\s*$")
                .matcher(title.trim());
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    public static String withCopy(String base, int number) {
        String stripped = Book.stripCopySuffix(base == null ? "" : base);
        if (stripped == null) {
            stripped = "";
        }
        if (number <= 1) {
            return stripped;
        }
        return stripped + ", c. " + number;
    }

    public static String combineUrls(String keeper, String source) {
        Set<String> urls = new LinkedHashSet<>();
        addUrls(urls, keeper);
        addUrls(urls, source);
        if (urls.isEmpty()) {
            return null;
        }
        return String.join(" ", urls);
    }

    private static void addUrls(Set<String> urls, String field) {
        if (field == null || field.isBlank()) {
            return;
        }
        for (String part : field.trim().split("\\s+")) {
            if (!part.isBlank()) {
                urls.add(part);
            }
        }
    }

    private static MiddlePick middlePick(String left, String right) {
        List<String> a = wordTokens(left);
        List<String> b = wordTokens(right);
        if (isMiddleExtension(b, a)) {
            return new MiddlePick(true);
        }
        if (isMiddleExtension(a, b)) {
            return new MiddlePick(false);
        }
        return null;
    }

    private static boolean isMiddleExtension(List<String> shorter, List<String> longer) {
        if (shorter.size() < 2 || longer.size() != shorter.size() + 1) {
            return false;
        }
        int i = 0;
        int skip = -1;
        for (int j = 0; j < longer.size(); j++) {
            if (i < shorter.size() && shorter.get(i).equals(longer.get(j))) {
                i++;
            } else if (skip == -1) {
                skip = j;
            } else {
                return false;
            }
        }
        if (i != shorter.size()) {
            return false;
        }
        if (skip == -1) {
            skip = longer.size() - 1;
        }
        return skip > 0 && skip < longer.size() - 1;
    }

    private static String wordClass(String token) {
        if (token == null) {
            return null;
        }
        if (Set.of("a", "an", "the").contains(token)) {
            return "article";
        }
        if (Set.of("of", "in", "on", "to", "for", "by", "at", "from", "with", "as", "into", "onto",
                "over", "after", "before", "between", "under", "about", "against", "among",
                "through", "during", "without", "within", "across", "around", "behind",
                "beyond", "despite", "except", "toward", "towards", "upon", "via", "per", "vs")
                .contains(token)) {
            return "preposition";
        }
        if (Set.of("and", "or", "but", "nor", "yet", "so", "if").contains(token)) {
            return "conjunction";
        }
        return null;
    }

    private static List<String> wordTokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String spaced = Book.stripCopySuffix(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
        if (spaced.isEmpty()) {
            return List.of();
        }
        return List.of(spaced.split("\\s+"));
    }

    private static boolean same(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        return a.equals(b);
    }

    private static boolean sameIgnoreCase(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        return a.equalsIgnoreCase(b);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record MiddlePick(boolean fullerIsA) {
    }
}
