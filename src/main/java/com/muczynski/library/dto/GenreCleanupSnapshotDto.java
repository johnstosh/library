/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Tags Clean Genres wrote, so Undo can put the previous lists back.
 * Stored as JSON on {@code catalog_maintenance_action}. Not part of import/export.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreCleanupSnapshotDto {

    @Builder.Default
    private List<BookTags> books = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookTags {
        private Long bookId;
        private String title;
        private List<String> before;
        private List<String> after;
    }
}
