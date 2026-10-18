/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.exception.LibraryException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Identity of a duplicate-title pair that should stay hidden.
 *
 * <p>Copies collapse on the folded primary title plus the normalized author, so
 * a mark stores that pair instead of book ids. A later scan that picks a
 * different copy as the representative still matches. Argument order does not
 * matter: the two sides are stored in lexicographic order.
 */
public record DuplicateTitlePairKey(String titleA, String authorA, String titleB, String authorB) {

    public static DuplicateTitlePairKey fromRaw(String rawTitleA, String rawAuthorA,
                                                 String rawTitleB, String rawAuthorB) {
        String titleA = foldTitle(rawTitleA);
        String authorA = DuplicateTitleService.normalizeAuthor(rawAuthorA);
        String titleB = foldTitle(rawTitleB);
        String authorB = DuplicateTitleService.normalizeAuthor(rawAuthorB);
        if (titleA.isEmpty() || titleB.isEmpty()) {
            throw new LibraryException("Both books need a title before this pair can be marked.");
        }
        if (compare(titleA, authorA, titleB, authorB) > 0) {
            return new DuplicateTitlePairKey(titleB, authorB, titleA, authorA);
        }
        return new DuplicateTitlePairKey(titleA, authorA, titleB, authorB);
    }

    /**
     * Same title fold the scan uses when collapsing extra copies: copy suffix
     * and trailing volume suffix removed, then lowercased.
     */
    public static String foldTitle(String rawTitle) {
        return TitleNormalizer.foldPrimaryTitle(rawTitle).toLowerCase(Locale.ROOT);
    }

    /**
     * Stable lookup key. Length-prefixed so titles that contain spaces or
     * punctuation cannot be confused with each other.
     */
    public String pairKey() {
        String payload = lengthPrefixed(titleA)
                + lengthPrefixed(authorA)
                + lengthPrefixed(titleB)
                + lengthPrefixed(authorB);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }

    private static String lengthPrefixed(String value) {
        return value.length() + ":" + value;
    }

    private static int compare(String leftTitle, String leftAuthor, String rightTitle, String rightAuthor) {
        int byTitle = leftTitle.compareTo(rightTitle);
        if (byTitle != 0) {
            return byTitle;
        }
        return leftAuthor.compareTo(rightAuthor);
    }
}
