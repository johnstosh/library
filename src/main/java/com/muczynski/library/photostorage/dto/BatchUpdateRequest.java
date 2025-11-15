/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch updating media item metadata in Google Photos Library API
 */
@Data
@NoArgsConstructor
public class BatchUpdateRequest {
    private List<MediaItemUpdate> mediaItemUpdates;

    @Data
    @NoArgsConstructor
    public static class MediaItemUpdate {
        private String mediaItemId;
        private String updateMask; // e.g., "description"
        private MediaItem mediaItem;

        @Data
        @NoArgsConstructor
        public static class MediaItem {
            private String description;
            private String filename;
        }
    }
}
