/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import com.muczynski.library.freetext.FreeTextLookupCache;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Compares and stores rejected URL and catalog matches.
 * Comparison ignores case and a trailing slash. The stored text stays as typed.
 */
public final class RejectedMatches {

    private static final String GROKIPEDIA_PAGE = "grokipedia.com/page/";
    private static final int REASON_LIMIT = 500;

    private RejectedMatches() {
    }

    public static List<RejectedUrl> emptyUrls() {
        return List.of();
    }

    public static List<RejectedHolding> emptyHoldings() {
        return List.of();
    }

    public static List<RejectedUrl> orEmptyUrls(List<RejectedUrl> urls) {
        return urls == null ? List.of() : List.copyOf(urls);
    }

    public static List<RejectedHolding> orEmptyHoldings(List<RejectedHolding> holdings) {
        return holdings == null ? List.of() : List.copyOf(holdings);
    }

    /**
     * True when {@code candidate} is the same URL as a rejection, or the same
     * Grokipedia page. A stored slug such as {@code hermann_geiger} matches
     * {@code https://grokipedia.com/page/Hermann_Geiger}.
     */
    public static boolean rejectsUrl(List<RejectedUrl> rejected, String candidate) {
        if (candidate == null || candidate.isBlank() || rejected == null || rejected.isEmpty()) {
            return false;
        }
        String candidateKey = urlKey(candidate);
        String candidateSlug = grokipediaSlug(candidate);
        for (RejectedUrl row : rejected) {
            if (row == null || row.getUrl() == null || row.getUrl().isBlank()) {
                continue;
            }
            if (candidateKey != null && candidateKey.equals(urlKey(row.getUrl()))) {
                return true;
            }
            String storedSlug = grokipediaSlug(row.getUrl());
            if (candidateSlug != null && storedSlug != null && candidateSlug.equals(storedSlug)) {
                return true;
            }
            String bare = bareSlug(row.getUrl());
            if (candidateSlug != null && bare != null && candidateSlug.equals(bare)) {
                return true;
            }
        }
        return false;
    }

    public static boolean rejectsHolding(List<RejectedHolding> rejected, PublicLibrary library, String recordKey) {
        if (library == null || recordKey == null || recordKey.isBlank() || rejected == null) {
            return false;
        }
        String key = recordKey.trim().toLowerCase(Locale.ROOT);
        for (RejectedHolding row : rejected) {
            if (row == null || row.getLibrary() != library || row.getRecordKey() == null) {
                continue;
            }
            if (key.equals(row.getRecordKey().trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Drop rejected URLs from a space-separated free-text field.
     * A {@code librivox.org} URL is checked against {@code audioRejected}.
     * Every other URL is checked against {@code textRejected}.
     * When {@code forceAudio} is true, every URL is checked against the audio list.
     *
     * @return null when every URL was rejected
     */
    public static String withoutRejectedFreeText(String found, List<RejectedUrl> textRejected,
                                                  List<RejectedUrl> audioRejected, boolean forceAudio) {
        List<String> urls = FreeTextLookupCache.extractUrls(found);
        if (urls.isEmpty()) {
            return found;
        }
        List<String> kept = new ArrayList<>();
        for (String url : urls) {
            boolean audio = forceAudio || isLibriVox(url);
            if (audio && rejectsUrl(audioRejected, url)) {
                continue;
            }
            if (!audio && rejectsUrl(textRejected, url)) {
                continue;
            }
            kept.add(url);
        }
        if (kept.size() == urls.size()) {
            return found;
        }
        if (kept.isEmpty()) {
            return null;
        }
        return String.join(" ", kept);
    }

    /**
     * Drop rejected Grokipedia page URLs from a space-separated field.
     * Returns null when every URL was rejected. A field with no URL, such as
     * {@code "-"}, is returned unchanged.
     */
    public static String withoutRejectedGrokipedia(String field, List<RejectedUrl> rejected) {
        List<String> urls = FreeTextLookupCache.extractUrls(field);
        if (urls.isEmpty()) {
            return field;
        }
        List<String> kept = new ArrayList<>();
        for (String url : urls) {
            if (!rejectsUrl(rejected, url)) {
                kept.add(url);
            }
        }
        if (kept.size() == urls.size()) {
            return field;
        }
        if (kept.isEmpty()) {
            return null;
        }
        return String.join(" ", kept);
    }

    public static boolean isLibriVox(String url) {
        String key = urlKey(url);
        return key != null && key.contains("librivox.org");
    }

    /**
     * Replace the stored list with {@code incoming}. A null incoming list means
     * the caller omitted the field, so the stored list stays. A second copy of
     * the same URL updates reason, date, and by. The server sets date and by
     * on a new or changed row when {@code actor} is present.
     */
    public static List<RejectedUrl> acceptEditedUrls(List<RejectedUrl> incoming, List<RejectedUrl> stored, String actor) {
        if (incoming == null) {
            return stored;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<RejectedUrl> result = new ArrayList<>();
        for (RejectedUrl row : incoming) {
            if (row == null || row.getUrl() == null || row.getUrl().isBlank()) {
                continue;
            }
            String url = row.getUrl().trim();
            String reason = limitReason(row.getReason());
            RejectedUrl previous = findUrl(stored, url);
            RejectedUrl kept = new RejectedUrl();
            kept.setUrl(url);
            kept.setReason(reason);
            if (previous != null && sameReason(previous.getReason(), reason)) {
                kept.setDate(previous.getDate() != null ? previous.getDate() : today);
                kept.setBy(previous.getBy());
            } else if (actor != null && !actor.isBlank()) {
                kept.setDate(today);
                kept.setBy(actor.trim());
            } else {
                kept.setDate(row.getDate() != null ? row.getDate() : today);
                kept.setBy(blankToNull(row.getBy()));
            }
            replaceUrl(result, kept);
        }
        return result;
    }

    /**
     * Import path. Keeps the exported date and by. A missing date becomes today.
     */
    public static List<RejectedUrl> acceptImportedUrls(List<RejectedUrl> incoming) {
        if (incoming == null) {
            return null;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<RejectedUrl> result = new ArrayList<>();
        for (RejectedUrl row : incoming) {
            if (row == null || row.getUrl() == null || row.getUrl().isBlank()) {
                continue;
            }
            RejectedUrl kept = new RejectedUrl();
            kept.setUrl(row.getUrl().trim());
            kept.setReason(limitReason(row.getReason()));
            kept.setDate(row.getDate() != null ? row.getDate() : today);
            kept.setBy(blankToNull(row.getBy()));
            replaceUrl(result, kept);
        }
        return result;
    }

    public static List<RejectedHolding> acceptEditedHoldings(List<RejectedHolding> incoming,
                                                              List<RejectedHolding> stored, String actor) {
        if (incoming == null) {
            return stored;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<RejectedHolding> result = new ArrayList<>();
        for (RejectedHolding row : incoming) {
            if (row == null) {
                continue;
            }
            if (row.getLibrary() == null || row.getRecordKey() == null || row.getRecordKey().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A rejected catalog match needs a library and a record id");
            }
            String recordKey = row.getRecordKey().trim();
            String reason = limitReason(row.getReason());
            RejectedHolding previous = findHolding(stored, row.getLibrary(), recordKey);
            RejectedHolding kept = new RejectedHolding();
            kept.setLibrary(row.getLibrary());
            kept.setRecordKey(recordKey);
            kept.setUrl(blankToNull(row.getUrl()));
            kept.setReason(reason);
            if (previous != null && sameReason(previous.getReason(), reason)) {
                kept.setDate(previous.getDate() != null ? previous.getDate() : today);
                kept.setBy(previous.getBy());
                if (kept.getUrl() == null) {
                    kept.setUrl(previous.getUrl());
                }
            } else if (actor != null && !actor.isBlank()) {
                kept.setDate(today);
                kept.setBy(actor.trim());
            } else {
                kept.setDate(row.getDate() != null ? row.getDate() : today);
                kept.setBy(blankToNull(row.getBy()));
            }
            replaceHolding(result, kept);
        }
        return result;
    }

    public static List<RejectedHolding> acceptImportedHoldings(List<RejectedHolding> incoming) {
        if (incoming == null) {
            return null;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<RejectedHolding> result = new ArrayList<>();
        for (RejectedHolding row : incoming) {
            if (row == null) {
                continue;
            }
            if (row.getLibrary() == null || row.getRecordKey() == null || row.getRecordKey().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A rejected catalog match needs a library and a record id");
            }
            RejectedHolding kept = new RejectedHolding();
            kept.setLibrary(row.getLibrary());
            kept.setRecordKey(row.getRecordKey().trim());
            kept.setUrl(blankToNull(row.getUrl()));
            kept.setReason(limitReason(row.getReason()));
            kept.setDate(row.getDate() != null ? row.getDate() : today);
            kept.setBy(blankToNull(row.getBy()));
            replaceHolding(result, kept);
        }
        return result;
    }

    /** Keeper entries win when both lists reject the same URL. */
    public static List<RejectedUrl> unionUrls(List<RejectedUrl> keeper, List<RejectedUrl> source) {
        List<RejectedUrl> result = new ArrayList<>();
        if (keeper != null) {
            for (RejectedUrl row : keeper) {
                if (row != null && row.getUrl() != null && !row.getUrl().isBlank()) {
                    replaceUrl(result, copyUrl(row));
                }
            }
        }
        if (source != null) {
            for (RejectedUrl row : source) {
                if (row == null || row.getUrl() == null || row.getUrl().isBlank()) {
                    continue;
                }
                if (findUrl(result, row.getUrl()) == null) {
                    result.add(copyUrl(row));
                }
            }
        }
        return result;
    }

    /** Keeper entries win when both lists reject the same library record. */
    public static List<RejectedHolding> unionHoldings(List<RejectedHolding> keeper, List<RejectedHolding> source) {
        List<RejectedHolding> result = new ArrayList<>();
        if (keeper != null) {
            for (RejectedHolding row : keeper) {
                if (row != null && row.getLibrary() != null && row.getRecordKey() != null && !row.getRecordKey().isBlank()) {
                    replaceHolding(result, copyHolding(row));
                }
            }
        }
        if (source != null) {
            for (RejectedHolding row : source) {
                if (row == null || row.getLibrary() == null || row.getRecordKey() == null || row.getRecordKey().isBlank()) {
                    continue;
                }
                if (findHolding(result, row.getLibrary(), row.getRecordKey()) == null) {
                    result.add(copyHolding(row));
                }
            }
        }
        return result;
    }

    private static RejectedUrl copyUrl(RejectedUrl row) {
        return new RejectedUrl(row.getUrl().trim(), limitReason(row.getReason()), row.getDate(), blankToNull(row.getBy()));
    }

    private static RejectedHolding copyHolding(RejectedHolding row) {
        return new RejectedHolding(row.getLibrary(), row.getRecordKey().trim(), blankToNull(row.getUrl()),
                limitReason(row.getReason()), row.getDate(), blankToNull(row.getBy()));
    }

    private static void replaceUrl(List<RejectedUrl> result, RejectedUrl kept) {
        for (int i = 0; i < result.size(); i++) {
            if (sameUrl(result.get(i).getUrl(), kept.getUrl())) {
                result.set(i, kept);
                return;
            }
        }
        result.add(kept);
    }

    private static void replaceHolding(List<RejectedHolding> result, RejectedHolding kept) {
        for (int i = 0; i < result.size(); i++) {
            RejectedHolding existing = result.get(i);
            if (existing.getLibrary() == kept.getLibrary()
                    && sameKey(existing.getRecordKey(), kept.getRecordKey())) {
                result.set(i, kept);
                return;
            }
        }
        result.add(kept);
    }

    private static RejectedUrl findUrl(List<RejectedUrl> stored, String url) {
        if (stored == null) {
            return null;
        }
        for (RejectedUrl row : stored) {
            if (row != null && sameUrl(row.getUrl(), url)) {
                return row;
            }
        }
        return null;
    }

    private static RejectedHolding findHolding(List<RejectedHolding> stored, PublicLibrary library, String recordKey) {
        if (stored == null) {
            return null;
        }
        for (RejectedHolding row : stored) {
            if (row != null && row.getLibrary() == library && sameKey(row.getRecordKey(), recordKey)) {
                return row;
            }
        }
        return null;
    }

    private static boolean sameUrl(String left, String right) {
        String a = urlKey(left);
        String b = urlKey(right);
        if (a != null && a.equals(b)) {
            return true;
        }
        String slugA = grokipediaSlug(left);
        String slugB = grokipediaSlug(right);
        return slugA != null && slugA.equals(slugB);
    }

    private static boolean sameKey(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().toLowerCase(Locale.ROOT).equals(right.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean sameReason(String left, String right) {
        String a = left == null ? "" : left.trim();
        String b = right == null ? "" : right.trim();
        return a.equals(b);
    }

    static String urlKey(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        int query = trimmed.indexOf('?');
        if (query >= 0) {
            trimmed = trimmed.substring(0, query);
        }
        int hash = trimmed.indexOf('#');
        if (hash >= 0) {
            trimmed = trimmed.substring(0, hash);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    static String grokipediaSlug(String value) {
        if (value == null) {
            return null;
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        int index = lower.indexOf(GROKIPEDIA_PAGE);
        if (index < 0) {
            return null;
        }
        String slug = lower.substring(index + GROKIPEDIA_PAGE.length());
        int cut = slug.indexOf('?');
        if (cut >= 0) {
            slug = slug.substring(0, cut);
        }
        int hash = slug.indexOf('#');
        if (hash >= 0) {
            slug = slug.substring(0, hash);
        }
        while (slug.endsWith("/")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug.isEmpty() ? null : slug;
    }

    private static String bareSlug(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.contains("/") || trimmed.contains(" ") || trimmed.contains(":")) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static String limitReason(String reason) {
        if (reason == null) {
            return "";
        }
        String trimmed = reason.trim();
        if (trimmed.length() <= REASON_LIMIT) {
            return trimmed;
        }
        return trimmed.substring(0, REASON_LIMIT);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
