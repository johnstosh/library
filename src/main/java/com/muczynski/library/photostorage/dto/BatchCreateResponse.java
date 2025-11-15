/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for batch create media items operation in Google Photos Library API
 */
@Data
@NoArgsConstructor
public class BatchCreateResponse {
    private List<NewMediaItemResult> newMediaItemResults;

    @Data
    @NoArgsConstructor
    public static class NewMediaItemResult {
        private String uploadToken;
        private Status status;
        private MediaItem mediaItem;
    }

    @Data
    @NoArgsConstructor
    public static class Status {
        private String message;
        private Integer code;
    }

    @Data
    @NoArgsConstructor
    public static class MediaItem {
        private String id;
        private String description;
        private String filename;
        private String mimeType;
        private String baseUrl;
        private String productUrl;
    }
}
