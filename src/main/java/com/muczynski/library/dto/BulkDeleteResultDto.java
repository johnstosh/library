/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk delete operation results.
 * Returns partial success - books that could be deleted are deleted,
 * and error messages are returned for those that couldn't.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDeleteResultDto {
    private int deletedCount;
    private int failedCount;
    private List<Long> deletedIds;
    private List<BulkDeleteFailureDto> failures;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDeleteFailureDto {
        private Long id;
        private String title;
        private String errorMessage;
    }
}
