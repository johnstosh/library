/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for batch getting media items in Google Photos Library API
 * Returned from POST /v1/mediaItems:batchGet endpoint
 */
@Data
@NoArgsConstructor
public class BatchGetResponse {
    private List<MediaItemResult> mediaItemResults;

    @Data
    @NoArgsConstructor
    public static class MediaItemResult {
        private Status status;
        private MediaItemResponse mediaItem;

        @Data
        @NoArgsConstructor
        public static class Status {
            private Integer code;
            private String message;
        }
    }
}
