/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.DuplicateTitleDismissal;
import com.muczynski.library.dto.DuplicateTitlePairDto;
import com.muczynski.library.dto.DuplicateTitlesResultDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.DuplicateTitleDismissalRepository;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.duplicate.DuplicateTitlePlans.AuthorMergePlan;
import com.muczynski.library.service.duplicate.DuplicateTitlePlans.AuthorSide;
import com.muczynski.library.service.duplicate.DuplicateTitlePlans.TitlePlan;
import com.muczynski.library.service.duplicate.DuplicateTitlePlans.TitleSide;
import com.muczynski.library.service.duplicate.TitleNormalizer.NormalizedTitle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Finds near-duplicate catalog titles using keyword indexing + Jaro–Winkler
 * (Issue #351). Memory-safe: uses lightweight projections only.
 *
 * <p>WITHDRAWN books are omitted before representative collapse and pairing.
 * A withdrawn copy is never a representative and never appears in a pair.
 * Other statuses, including a null status, are included.
 *
 * <p>A librarian can mark a pair as not a duplicate. The mark is the folded
 * primary titles and normalized authors, so every extra copy of that pair is
 * left out of later scans. Marks are removed before the top-100 cap.

 *
 * <h2>Score formula</h2>
 * <p>Primary signal: Jaro–Winkler on folded/normalized titles (and on
 * {@code title | author} when both authors are present), with an author JW
 * gate at {@link #AUTHOR_SIMILARITY_THRESHOLD}. A pair is included only when
 * primary JW ≥ {@link #SIMILARITY_THRESHOLD} (keeps junk out).
 *
 * <p>Secondary (tie-break / finer ranking): character-level Levenshtein
 * similarity {@code 1 - distance/maxLen} on the same strings used for the
 * winning primary comparison — folded title alone when no author, or
 * {@code title | author} when both authors are present — so identical folded
 * titles with author prefix differences (e.g. "Joseph Ratzinger" vs
 * "Joseph Aloisius Ratzinger") score slightly below 1.0.
 *
 * <p>Reported score (display + sort):
 * {@code 0.85 * primaryJw + 0.15 * levSim}, clamped to [0, 1], rounded to
 * 6 decimal places. Exact identical folded title+author yields 1.0.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateTitleService {

    /** Minimum primary Jaro–Winkler score to report a pair. Tuned so slight
     *  misspellings match (e.g. Confessions/Confession) while unrelated titles
     *  do not flood. Gate uses primary JW only; the blended score is for
     *  ranking/display. */
    public static final double SIMILARITY_THRESHOLD = 0.85;

    /**
     * When both books have a non-blank author, author Jaro–Winkler must meet this
     * gate or the pair is rejected (same-title different-author must not score high).
     */
    public static final double AUTHOR_SIMILARITY_THRESHOLD = 0.85;

    /** Weight of primary Jaro–Winkler in the reported blend. */
    public static final double JW_WEIGHT = 0.85;

    /** Weight of secondary Levenshtein similarity in the reported blend. */
    public static final double LEV_WEIGHT = 0.15;

    public static final int MAX_RESULTS = 100;

    private static final Pattern AUTHOR_NON_LETTER_DIGIT =
            Pattern.compile("[^a-z0-9]+");

    private final BookRepository bookRepository;
    private final DuplicateTitleDismissalRepository dismissalRepository;
    private final AuthorRepository authorRepository;
    private final BookService bookService;

    @Transactional(readOnly = true)
    public DuplicateTitlesResultDto findDuplicates() {
        List<BookRepository.DuplicateTitleProjection> projections =
                bookRepository.findAllForDuplicateTitleScan().stream()
                        .filter(p -> p.getStatus() != BookStatus.WITHDRAWN)
                        .toList();
        int booksScanned = projections.size();

        List<BookEntry> representatives = buildRepresentatives(projections);
        List<DuplicateTitlePairDto> pairs = excludeDismissed(findPairs(representatives));

        pairs.sort(Comparator.comparingDouble(DuplicateTitlePairDto::getScore).reversed()
                .thenComparing(DuplicateTitlePairDto::getBookAId)
                .thenComparing(DuplicateTitlePairDto::getBookBId));

        if (pairs.size() > MAX_RESULTS) {
            pairs = new ArrayList<>(pairs.subList(0, MAX_RESULTS));
        }
        enrich(pairs);

        // Neutral count only — no score/threshold language for the UI.
        String message = pairs.isEmpty()
                ? "No pairs found."
                : pairs.size() + " pairs found.";

        log.info("Duplicate title scan: scanned={}, representatives={}, pairs={}",
                booksScanned, representatives.size(), pairs.size());

        return DuplicateTitlesResultDto.builder()
                .pairs(pairs)
                .booksScanned(booksScanned)
                .representativesCompared(representatives.size())
                .message(message)
                .build();
    }

    /**
     * Remember that these two works are not duplicates. Copy suffixes and
     * argument order are folded away. Marking an already stored pair does nothing.
     */
    @Transactional
    public void dismiss(String titleA, String authorA, String titleB, String authorB) {
        DuplicateTitlePairKey key = DuplicateTitlePairKey.fromRaw(titleA, authorA, titleB, authorB);
        if (dismissalRepository.findByPairKey(key.pairKey()).isPresent()) {
            return;
        }
        DuplicateTitleDismissal row = new DuplicateTitleDismissal();
        row.setPairKey(key.pairKey());
        row.setTitleA(key.titleA());
        row.setAuthorA(key.authorA());
        row.setTitleB(key.titleB());
        row.setAuthorB(key.authorB());
        dismissalRepository.save(row);
        log.info("Marked title pair as not a duplicate: '{}' by '{}' / '{}' by '{}'",
                key.titleA(), key.authorA(), key.titleB(), key.authorB());
    }

    /**
     * Forget a not-duplicate mark. Missing marks are ignored so Undo can be
     * repeated safely.
     */
    @Transactional
    public void restore(String titleA, String authorA, String titleB, String authorB) {
        DuplicateTitlePairKey key = DuplicateTitlePairKey.fromRaw(titleA, authorA, titleB, authorB);
        dismissalRepository.findByPairKey(key.pairKey()).ifPresent(row -> {
            dismissalRepository.delete(row);
            log.info("Restored title pair to the duplicate scan: '{}' by '{}' / '{}' by '{}'",
                    key.titleA(), key.authorA(), key.titleB(), key.authorB());
        });
    }

    private List<DuplicateTitlePairDto> excludeDismissed(List<DuplicateTitlePairDto> pairs) {
        List<DuplicateTitleDismissal> dismissed = dismissalRepository.findAll();
        if (dismissed.isEmpty() || pairs.isEmpty()) {
            return pairs;
        }
        Set<String> keys = new HashSet<>();
        for (DuplicateTitleDismissal row : dismissed) {
            if (row.getPairKey() != null && !row.getPairKey().isBlank()) {
                keys.add(row.getPairKey());
            }
        }
        List<DuplicateTitlePairDto> kept = new ArrayList<>();
        for (DuplicateTitlePairDto pair : pairs) {
            if (!keys.contains(dismissalKey(pair))) {
                kept.add(pair);
            }
        }
        return kept;
    }

    private static String dismissalKey(DuplicateTitlePairDto pair) {
        try {
            return DuplicateTitlePairKey.fromRaw(
                    pair.getBookATitle(),
                    pair.getBookAAuthorName(),
                    pair.getBookBTitle(),
                    pair.getBookBAuthorName()).pairKey();
        } catch (LibraryException ex) {
            return "";
        }
    }

    /**
     * Collapse copy-suffix and volume-suffix twins that share the same folded
     * primary title (lowercased) and normalized author: keep only the lowest id.
     */
    List<BookEntry> buildRepresentatives(List<BookRepository.DuplicateTitleProjection> projections) {
        Map<String, BookEntry> byFoldedKey = new HashMap<>();
        List<BookEntry> noPrimary = new ArrayList<>();

        for (BookRepository.DuplicateTitleProjection p : projections) {
            BookEntry entry = BookEntry.from(p);
            String key = entry.representativeKey();
            if (key == null) {
                noPrimary.add(entry);
                continue;
            }
            BookEntry existing = byFoldedKey.get(key);
            if (existing == null || entry.id < existing.id) {
                byFoldedKey.put(key, entry);
            }
        }

        List<BookEntry> result = new ArrayList<>(byFoldedKey.values());
        result.addAll(noPrimary);
        result.sort(Comparator.comparingLong(e -> e.id));
        return result;
    }

    List<DuplicateTitlePairDto> findPairs(List<BookEntry> books) {
        // keyword → indices into books
        Map<String, List<Integer>> keywordIndex = new HashMap<>();
        for (int i = 0; i < books.size(); i++) {
            for (String kw : books.get(i).allKeywords()) {
                keywordIndex.computeIfAbsent(kw, k -> new ArrayList<>()).add(i);
            }
        }

        // unique unordered candidate pairs that share ≥1 keyword
        Set<String> seenPairKeys = new HashSet<>();
        List<DuplicateTitlePairDto> pairs = new ArrayList<>();

        for (List<Integer> indices : keywordIndex.values()) {
            if (indices.size() < 2) {
                continue;
            }
            for (int a = 0; a < indices.size(); a++) {
                for (int b = a + 1; b < indices.size(); b++) {
                    int i = indices.get(a);
                    int j = indices.get(b);
                    if (i == j) {
                        continue;
                    }
                    BookEntry left = books.get(Math.min(i, j));
                    BookEntry right = books.get(Math.max(i, j));
                    if (Objects.equals(left.id, right.id)) {
                        continue;
                    }
                    String pairKey = pairKey(left.id, right.id);
                    if (!seenPairKeys.add(pairKey)) {
                        continue;
                    }

                    BestMatch best = bestSimilarity(left, right);
                    if (best.primaryJw >= SIMILARITY_THRESHOLD) {
                        pairs.add(DuplicateTitlePairDto.builder()
                                .score(round6(best.score))
                                .bookAId(left.id)
                                .bookATitle(left.title)
                                .bookAAlternateTitle(left.alternateTitle)
                                .bookAAuthorName(left.authorName)
                                .bookAStatus(left.statusName())
                                .bookBId(right.id)
                                .bookBTitle(right.title)
                                .bookBAlternateTitle(right.alternateTitle)
                                .bookBAuthorName(right.authorName)
                                .bookBStatus(right.statusName())
                                .matchedTitleA(best.matchedA)
                                .matchedTitleB(best.matchedB)
                                .matchedFieldA(best.fieldA)
                                .matchedFieldB(best.fieldB)
                                .bookAAuthorId(left.authorId)
                                .bookBAuthorId(right.authorId)
                                .bookALocNumber(left.locNumber)
                                .bookBLocNumber(right.locNumber)
                                .bookAElectronicResource(left.electronicResource)
                                .bookBElectronicResource(right.electronicResource)
                                .bookACatalogLabels(DuplicateCatalogLabels.of(
                                        left.status, left.locNumber, left.electronicResource))
                                .bookBCatalogLabels(DuplicateCatalogLabels.of(
                                        right.status, right.locNumber, right.electronicResource))
                                .build());
                    }
                }
            }
        }
        return pairs;
    }

    private static BestMatch bestSimilarity(BookEntry a, BookEntry b) {
        String authorA = normalizeAuthor(a.authorName);
        String authorB = normalizeAuthor(b.authorName);
        boolean bothAuthorsPresent = !authorA.isEmpty() && !authorB.isEmpty();
        if (bothAuthorsPresent) {
            double authorJw = JaroWinklerSimilarity.similarity(authorA, authorB);
            if (authorJw < AUTHOR_SIMILARITY_THRESHOLD) {
                return new BestMatch(0.0, 0.0, null, null, null, null);
            }
        }

        BestMatch best = new BestMatch(0.0, 0.0, null, null, null, null);
        for (TitleVariant va : a.variants) {
            for (TitleVariant vb : b.variants) {
                if (va.normalized.isEmpty() || vb.normalized.isEmpty()) {
                    continue;
                }
                String normA = va.normalized.normalizedString();
                String normB = vb.normalized.normalizedString();

                double titleJw = JaroWinklerSimilarity.similarity(normA, normB);
                double rawJw = JaroWinklerSimilarity.similarity(
                        va.strippedForCompare,
                        vb.strippedForCompare);
                double jw = Math.max(titleJw, rawJw);

                String levLeft;
                String levRight;
                if (bothAuthorsPresent) {
                    String combinedA = normA + " | " + authorA;
                    String combinedB = normB + " | " + authorB;
                    double combined = JaroWinklerSimilarity.similarity(combinedA, combinedB);
                    jw = Math.max(jw, combined);
                    // Secondary always uses title|author when authors present so
                    // author prefix differences break flat JW=1.0 ties.
                    levLeft = combinedA;
                    levRight = combinedB;
                } else if (rawJw >= titleJw) {
                    levLeft = va.strippedForCompare;
                    levRight = vb.strippedForCompare;
                } else {
                    levLeft = normA;
                    levRight = normB;
                }

                if (jw < SIMILARITY_THRESHOLD) {
                    continue;
                }

                double lev = levenshteinSimilarity(levLeft, levRight);
                double blended = blendScore(jw, lev);

                if (blended > best.score
                        || (blended == best.score && jw > best.primaryJw)) {
                    best = new BestMatch(blended, jw, va.originalDisplay, vb.originalDisplay,
                            va.fieldLabel, vb.fieldLabel);
                }
            }
        }
        return best;
    }

    /**
     * Reported score: {@code JW_WEIGHT * primaryJw + LEV_WEIGHT * levSim} in [0, 1].
     */
    static double blendScore(double primaryJw, double levSim) {
        double blended = JW_WEIGHT * primaryJw + LEV_WEIGHT * levSim;
        if (blended < 0.0) {
            return 0.0;
        }
        if (blended > 1.0) {
            return 1.0;
        }
        return blended;
    }

    /**
     * Character-level Levenshtein similarity: {@code 1 - distance / maxLen}.
     */
    static double levenshteinSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }
        int dist = levenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) dist / (double) maxLen);
    }

    static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        // Two-row DP to keep memory small for long titles
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
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[len2];
    }

    static String normalizeAuthor(String authorName) {
        if (authorName == null || authorName.isBlank()) {
            return "";
        }
        String lower = authorName.toLowerCase(Locale.ROOT).trim();
        String spaced = AUTHOR_NON_LETTER_DIGIT.matcher(lower).replaceAll(" ").trim();
        return spaced.replaceAll("\\s+", " ");
    }

    private static String pairKey(long id1, long id2) {
        long min = Math.min(id1, id2);
        long max = Math.max(id1, id2);
        return min + ":" + max;
    }

    static double round6(double v) {
        return Math.round(v * 1_000_000.0) / 1_000_000.0;
    }

    private record BestMatch(double score, double primaryJw, String matchedA, String matchedB,
                              String fieldA, String fieldB) {
    }

    static final class BookEntry {
        final long id;
        final String title;
        final String alternateTitle;
        final String authorName;
        final Long authorId;
        final String locNumber;
        final Boolean electronicResource;
        final BookStatus status;
        final List<TitleVariant> variants;

        BookEntry(long id, String title, String alternateTitle, String authorName, Long authorId,
                  String locNumber, Boolean electronicResource, BookStatus status,
                  List<TitleVariant> variants) {
            this.id = id;
            this.title = title;
            this.alternateTitle = alternateTitle;
            this.authorName = authorName;
            this.authorId = authorId;
            this.locNumber = locNumber;
            this.electronicResource = electronicResource;
            this.status = status;
            this.variants = variants;
        }

        static BookEntry from(BookRepository.DuplicateTitleProjection p) {
            List<TitleVariant> variants = new ArrayList<>(2);
            addVariant(variants, p.getTitle(), "title");
            addVariant(variants, p.getAlternateTitle(), "alternate title");
            return new BookEntry(
                    p.getId(),
                    p.getTitle(),
                    p.getAlternateTitle(),
                    p.getAuthorName(),
                    p.getAuthorId(),
                    p.getLocNumber(),
                    p.getElectronicResource(),
                    p.getStatus(),
                    variants);
        }

        private static void addVariant(List<TitleVariant> variants, String raw, String fieldLabel) {
            if (raw == null || raw.isBlank()) {
                return;
            }
            String folded = TitleNormalizer.foldPrimaryTitle(raw);
            String truncated = TitleNormalizer.truncateSubtitle(
                    folded.isEmpty() ? Book.stripCopySuffix(raw) : folded);
            String compare = truncated == null ? "" : truncated.toLowerCase(Locale.ROOT).trim();
            NormalizedTitle norm = TitleNormalizer.normalize(raw);
            variants.add(new TitleVariant(raw, compare, norm, fieldLabel));
        }

        /**
         * Folded primary title (copy + volume stripped) lowercased + normalized author.
         * Null when there is no usable primary title.
         */
        String representativeKey() {
            if (title == null || title.isBlank()) {
                return null;
            }
            String folded = TitleNormalizer.foldPrimaryTitle(title);
            if (folded.isEmpty()) {
                return null;
            }
            return folded.toLowerCase(Locale.ROOT) + "|" + normalizeAuthor(authorName);
        }

        String statusName() {
            return status == null ? null : status.name();
        }

        Set<String> allKeywords() {
            Set<String> all = new HashSet<>();
            for (TitleVariant v : variants) {
                all.addAll(v.normalized.keywords());
            }
            return all;
        }
    }

    record TitleVariant(String originalDisplay, String strippedForCompare, NormalizedTitle normalized,
                         String fieldLabel) {
    }

    private void enrich(List<DuplicateTitlePairDto> pairs) {
        if (pairs.isEmpty()) {
            return;
        }
        Set<Long> bookIds = new LinkedHashSet<>();
        Set<Long> authorIds = new LinkedHashSet<>();
        for (DuplicateTitlePairDto pair : pairs) {
            if (pair.getBookAId() != null) {
                bookIds.add(pair.getBookAId());
            }
            if (pair.getBookBId() != null) {
                bookIds.add(pair.getBookBId());
            }
            if (pair.getBookAAuthorId() != null) {
                authorIds.add(pair.getBookAAuthorId());
            }
            if (pair.getBookBAuthorId() != null) {
                authorIds.add(pair.getBookBAuthorId());
            }
        }
        Map<Long, String> urls = freeTextByBook(bookIds);
        Map<Long, AuthorNames> names = authorNames(authorIds);
        for (DuplicateTitlePairDto pair : pairs) {
            AuthorNames namesA = names.get(pair.getBookAAuthorId());
            AuthorNames namesB = names.get(pair.getBookBAuthorId());
            if (namesA != null) {
                pair.setBookAAuthorAlternateNames(namesA.alternates());
                if (pair.getBookAAuthorName() == null) {
                    pair.setBookAAuthorName(namesA.name());
                }
            }
            if (namesB != null) {
                pair.setBookBAuthorAlternateNames(namesB.alternates());
                if (pair.getBookBAuthorName() == null) {
                    pair.setBookBAuthorName(namesB.name());
                }
            }
            pair.setTitleDifference(DuplicateTitleDifference.titles(
                    pair.getMatchedTitleA(), pair.getMatchedTitleB(),
                    pair.getMatchedFieldA(), pair.getMatchedFieldB(),
                    pair.getBookAAlternateTitle(), pair.getBookBAlternateTitle()));
            pair.setAuthorDifference(DuplicateTitleDifference.authors(
                    pair.getBookAAuthorId(), pair.getBookAAuthorName(), pair.getBookAAuthorAlternateNames(),
                    pair.getBookBAuthorId(), pair.getBookBAuthorName(), pair.getBookBAuthorAlternateNames()));
            TitleSide leftSide = new TitleSide(pair.getBookAId(), pair.getBookATitle(), pair.getBookAAlternateTitle(),
                    pair.getBookALocNumber(), Boolean.TRUE.equals(pair.getBookAElectronicResource()),
                    urls.get(pair.getBookAId()));
            TitleSide rightSide = new TitleSide(pair.getBookBId(), pair.getBookBTitle(), pair.getBookBAlternateTitle(),
                    pair.getBookBLocNumber(), Boolean.TRUE.equals(pair.getBookBElectronicResource()),
                    urls.get(pair.getBookBId()));
            TitlePlan titlePlan = DuplicateTitlePlans.titles(leftSide, rightSide);
            pair.setTitleAction(titlePlan.actionKind());
            if (DuplicateTitlePlans.FIX_TITLES.equals(titlePlan.actionKind())) {
                BookService.FixCopyTitles copies = bookService.allocateFixTitles(
                        titlePlan.leftTitle(), titlePlan.leftId(),
                        titlePlan.rightTitle(), titlePlan.rightId());
                String leftAlternate = DuplicateTitlePlans.alternateAfterTitleChange(
                        pair.getBookAAlternateTitle(), pair.getBookATitle(), copies.leftTitle());
                String rightAlternate = DuplicateTitlePlans.alternateAfterTitleChange(
                        pair.getBookBAlternateTitle(), pair.getBookBTitle(), copies.rightTitle());
                pair.setTitleChangeNeeded(!sameText(pair.getBookATitle(), copies.leftTitle())
                        || !sameText(pair.getBookBTitle(), copies.rightTitle())
                        || !sameText(pair.getBookAAlternateTitle(), leftAlternate)
                        || !sameText(pair.getBookBAlternateTitle(), rightAlternate));
                pair.setTitlePlan(DuplicateTitlePlans.fixSummary(
                        leftSide, rightSide, copies.leftTitle(), copies.rightTitle(),
                        leftAlternate, rightAlternate));
            } else {
                pair.setTitleChangeNeeded(titlePlan.changeNeeded());
                pair.setTitlePlan(titlePlan.summary());
            }
            AuthorMergePlan authorPlan = DuplicateTitlePlans.authorMerge(
                    new AuthorSide(idOrZero(pair.getBookAAuthorId()), pair.getBookAAuthorName(),
                            pair.getBookAAuthorAlternateNames()),
                    new AuthorSide(idOrZero(pair.getBookBAuthorId()), pair.getBookBAuthorName(),
                            pair.getBookBAuthorAlternateNames()));
            pair.setAuthorMergeAvailable(authorPlan.available());
            pair.setAuthorPlan(authorPlan.summary());
        }
    }

    private static boolean sameText(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        return a.equals(b);
    }

    private Map<Long, String> freeTextByBook(Collection<Long> bookIds) {
        Map<Long, String> urls = new HashMap<>();
        if (bookIds.isEmpty()) {
            return urls;
        }
        for (Object[] row : bookRepository.findDuplicateTitleFreeText(bookIds)) {
            if (row[0] instanceof Number id) {
                urls.put(id.longValue(), row[1] == null ? null : row[1].toString());
            }
        }
        return urls;
    }

    private Map<Long, AuthorNames> authorNames(Collection<Long> authorIds) {
        Map<Long, AuthorNames> names = new HashMap<>();
        if (authorIds.isEmpty()) {
            return names;
        }
        for (Object[] row : authorRepository.findDuplicateTitleAuthorNames(authorIds)) {
            if (!(row[0] instanceof Number id)) {
                continue;
            }
            String name = row[1] == null ? null : row[1].toString();
            names.put(id.longValue(), new AuthorNames(name, alternateNames(row[2])));
        }
        return names;
    }

    private static List<String> alternateNames(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof String[] array) {
            return listOf(array);
        }
        if (value instanceof java.sql.Array array) {
            try {
                Object raw = array.getArray();
                if (raw instanceof String[] strings) {
                    return listOf(strings);
                }
                if (raw instanceof Object[] objects) {
                    List<String> names = new ArrayList<>();
                    for (Object object : objects) {
                        if (object != null && !object.toString().isBlank()) {
                            names.add(object.toString());
                        }
                    }
                    return names;
                }
            } catch (SQLException ex) {
                throw new LibraryException("Could not read alternate author names.", ex);
            }
        }
        return List.of();
    }

    private static List<String> listOf(String[] values) {
        List<String> names = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                names.add(value);
            }
        }
        return names;
    }

    private static long idOrZero(Long id) {
        return id == null ? 0L : id;
    }

    private record AuthorNames(String name, List<String> alternates) {
    }
}
