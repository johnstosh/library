/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for batch update media items operation in Google Photos Library API
 */
@Data
@NoArgsConstructor
public class BatchUpdateResponse {
    private List<MediaItemResult> mediaItemResults;

    @Data
    @NoArgsConstructor
    public static class MediaItemResult {
        private MediaItemResponse mediaItem;
        private Status status;
    }

    @Data
    @NoArgsConstructor
    public static class Status {
        private String message;
        private Integer code;
    }
}
