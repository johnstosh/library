/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the {@code favoriteLists} query value. Names are comma-separated.
 * A name that contains a comma, or that starts with "~", is written by the
 * client as "~" plus a percent-encoded name. The servlet has already decoded
 * the query string once, so this class decodes only that inner encoding.
 * Plain names such as {@code Have Read,Saints} stay unchanged.
 */
public final class FavoriteListParams {

    private static final String ENCODED_PREFIX = "~";

    private FavoriteListParams() {
    }

    public static List<String> split(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String part : raw.split(",", -1)) {
            String name = decode(part.trim());
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    private static String decode(String token) {
        if (!token.startsWith(ENCODED_PREFIX)) {
            return token;
        }
        String encoded = token.substring(ENCODED_PREFIX.length()).replace("+", "%2B");
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return token;
        }
    }
}
