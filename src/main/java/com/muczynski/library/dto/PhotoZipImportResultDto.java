/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoZipImportResultDto {
    private int totalFiles;
    private int successCount;
    private int failureCount;
    private int skippedCount;
    private List<PhotoZipImportItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoZipImportItemDto {
        private String filename;
        private String status; // SUCCESS, FAILURE, SKIPPED
        private String entityType; // book, author, loan
        private String entityName; // title or author name
        private Long entityId;
        private Long photoId;
        private String errorMessage;
    }
}
