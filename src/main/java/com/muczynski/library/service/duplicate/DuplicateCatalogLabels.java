/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.BookStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Books-page status filter names for one catalog row.
 * The words match {@code BOOK_STATUS_FILTER_LABELS} in the React app.
 * A book can carry more than one name when it is both on the shelf and electronic.
 */
public final class DuplicateCatalogLabels {

    private DuplicateCatalogLabels() {
    }

    public static boolean hasCallNumber(String locNumber) {
        return locNumber != null && !locNumber.isBlank();
    }

    public static List<String> of(BookStatus status, String locNumber, Boolean electronicResource) {
        List<String> labels = new ArrayList<>();
        boolean active = status == BookStatus.ACTIVE;
        boolean callNumber = hasCallNumber(locNumber);
        boolean electronic = Boolean.TRUE.equals(electronicResource);
        if (active && callNumber) {
            labels.add("In-library");
        }
        if (active && electronic) {
            labels.add("Electronic resource");
        }
        if (active && !callNumber && !electronic) {
            labels.add("Without LOC");
        }
        if (status == BookStatus.LOST) {
            labels.add("Lost");
        }
        if (status == BookStatus.WITHDRAWN) {
            labels.add("Withdrawn");
        }
        if (status == BookStatus.ON_ORDER) {
            labels.add("On Order");
        }
        if (status == BookStatus.REQUESTED) {
            labels.add("Requested");
        }
        if (labels.isEmpty()) {
            labels.add(status == null ? "No status" : status.name());
        }
        return List.copyOf(labels);
    }
}
