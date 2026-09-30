/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import java.util.List;

/**
 * Shared cap for {@code POST /books/by-ids} and {@code POST /authors/by-ids}.
 * One request larger than this holds the database long enough to time out.
 */
public final class ByIds {

    public static final int MAX_BATCH = 100;

    private ByIds() {
    }

    public static boolean exceedsBatch(List<?> ids) {
        return ids != null && ids.size() > MAX_BATCH;
    }
}
