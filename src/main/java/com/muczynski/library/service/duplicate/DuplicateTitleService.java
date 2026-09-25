/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.DuplicateTitlePairDto;
import com.muczynski.library.dto.DuplicateTitlesResultDto;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.service.duplicate.TitleNormalizer.NormalizedTitle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Finds near-duplicate catalog titles using keyword indexing + Jaro–Winkler
 * (Issue #351). Memory-safe: uses lightweight projections only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateTitleService {

    /** Minimum Jaro–Winkler score to report a pair. Tuned so slight misspellings
     *  match (e.g. Confessions/Confession) while unrelated titles do not flood. */
    public static final double SIMILARITY_THRESHOLD = 0.85;

    /**
     * When both books have a non-blank author, author Jaro–Winkler must meet this
     * gate or the pair is rejected (same-title different-author must not score high).
     */
    public static final double AUTHOR_SIMILARITY_THRESHOLD = 0.85;

    public static final int MAX_RESULTS = 100;

    private static final Pattern AUTHOR_NON_LETTER_DIGIT =
            Pattern.compile("[^a-z0-9]+");

    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public DuplicateTitlesResultDto findDuplicates() {
        List<BookRepository.DuplicateTitleProjection> projections =
                bookRepository.findAllForDuplicateTitleScan();
        int booksScanned = projections.size();

        List<BookEntry> representatives = buildRepresentatives(projections);
        List<DuplicateTitlePairDto> pairs = findPairs(representatives);

        pairs.sort(Comparator.comparingDouble(DuplicateTitlePairDto::getScore).reversed()
                .thenComparing(DuplicateTitlePairDto::getBookAId)
                .thenComparing(DuplicateTitlePairDto::getBookBId));

        if (pairs.size() > MAX_RESULTS) {
            pairs = new ArrayList<>(pairs.subList(0, MAX_RESULTS));
        }

        String message = pairs.isEmpty()
                ? "No near-duplicate titles found (threshold " + SIMILARITY_THRESHOLD + ")."
                : "Found " + pairs.size() + " near-duplicate pair(s) (top "
                        + MAX_RESULTS + ", score ≥ " + SIMILARITY_THRESHOLD + ").";

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
                    if (best.score >= SIMILARITY_THRESHOLD) {
                        pairs.add(DuplicateTitlePairDto.builder()
                                .score(round4(best.score))
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
                return new BestMatch(0.0, null, null);
            }
        }

        BestMatch best = new BestMatch(0.0, null, null);
        for (TitleVariant va : a.variants) {
            for (TitleVariant vb : b.variants) {
                if (va.normalized.isEmpty() || vb.normalized.isEmpty()) {
                    continue;
                }
                double score = JaroWinklerSimilarity.similarity(
                        va.normalized.normalizedString(),
                        vb.normalized.normalizedString());
                double rawScore = JaroWinklerSimilarity.similarity(
                        va.strippedForCompare,
                        vb.strippedForCompare);
                double max = Math.max(score, rawScore);

                // Also compare "title | author" when authors are present
                if (bothAuthorsPresent) {
                    double combined = JaroWinklerSimilarity.similarity(
                            va.normalized.normalizedString() + " | " + authorA,
                            vb.normalized.normalizedString() + " | " + authorB);
                    max = Math.max(max, combined);
                }

                if (max > best.score) {
                    best = new BestMatch(max, va.originalDisplay, vb.originalDisplay);
                }
            }
        }
        return best;
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

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private record BestMatch(double score, String matchedA, String matchedB) {
    }

    static final class BookEntry {
        final long id;
        final String title;
        final String alternateTitle;
        final String authorName;
        final BookStatus status;
        final List<TitleVariant> variants;

        BookEntry(long id, String title, String alternateTitle, String authorName,
                  BookStatus status, List<TitleVariant> variants) {
            this.id = id;
            this.title = title;
            this.alternateTitle = alternateTitle;
            this.authorName = authorName;
            this.status = status;
            this.variants = variants;
        }

        static BookEntry from(BookRepository.DuplicateTitleProjection p) {
            List<TitleVariant> variants = new ArrayList<>(2);
            addVariant(variants, p.getTitle());
            addVariant(variants, p.getAlternateTitle());
            return new BookEntry(
                    p.getId(),
                    p.getTitle(),
                    p.getAlternateTitle(),
                    p.getAuthorName(),
                    p.getStatus(),
                    variants);
        }

        private static void addVariant(List<TitleVariant> variants, String raw) {
            if (raw == null || raw.isBlank()) {
                return;
            }
            String folded = TitleNormalizer.foldPrimaryTitle(raw);
            String truncated = TitleNormalizer.truncateSubtitle(
                    folded.isEmpty() ? Book.stripCopySuffix(raw) : folded);
            String compare = truncated == null ? "" : truncated.toLowerCase(Locale.ROOT).trim();
            NormalizedTitle norm = TitleNormalizer.normalize(raw);
            variants.add(new TitleVariant(raw, compare, norm));
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

    record TitleVariant(String originalDisplay, String strippedForCompare, NormalizedTitle normalized) {
    }
}
