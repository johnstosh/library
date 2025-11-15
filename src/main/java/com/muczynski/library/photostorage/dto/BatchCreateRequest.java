/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch creating media items in Google Photos Library API
 */
@Data
@NoArgsConstructor
public class BatchCreateRequest {
    private String albumId;
    private List<NewMediaItem> newMediaItems;
    private AlbumPosition albumPosition;

    @Data
    @NoArgsConstructor
    public static class NewMediaItem {
        private String description;
        private SimpleMediaItem simpleMediaItem;
    }

    @Data
    @NoArgsConstructor
    public static class SimpleMediaItem {
        private String uploadToken;
    }

    @Data
    @NoArgsConstructor
    public static class AlbumPosition {
        private String position; // e.g., "FIRST_IN_ALBUM"
    }
}
