/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

/**
 * Jaro–Winkler string similarity in [0, 1] with no external dependency.
 * Used for duplicate title detection (Issue #351).
 */
public final class JaroWinklerSimilarity {

    private static final double PREFIX_SCALE = 0.1;
    private static final int MAX_PREFIX = 4;

    private JaroWinklerSimilarity() {
    }

    /**
     * @return similarity in [0, 1]; 1 means identical; empty/blank vs non-blank is 0
     */
    public static double similarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        double jaro = jaro(s1, s2);
        if (jaro < 0.7) {
            // Winkler boost only applies when Jaro is already reasonably high
            return jaro;
        }

        int prefix = 0;
        int max = Math.min(MAX_PREFIX, Math.min(s1.length(), s2.length()));
        while (prefix < max && s1.charAt(prefix) == s2.charAt(prefix)) {
            prefix++;
        }
        return jaro + prefix * PREFIX_SCALE * (1.0 - jaro);
    }

    static double jaro(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 && len2 == 0) {
            return 1.0;
        }
        if (len1 == 0 || len2 == 0) {
            return 0.0;
        }

        int matchDistance = Math.max(len1, len2) / 2 - 1;
        if (matchDistance < 0) {
            matchDistance = 0;
        }

        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];

        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) {
                    continue;
                }
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        double t = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) {
                continue;
            }
            while (!s2Matches[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                t += 0.5;
            }
            k++;
        }

        double m = matches;
        return (m / len1 + m / len2 + (m - t) / m) / 3.0;
    }
}
