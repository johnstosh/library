/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for listing albums from Google Photos Library API
 */
@Data
@NoArgsConstructor
public class AlbumsListResponse {
    private List<Album> albums;
    private String nextPageToken;

    @Data
    @NoArgsConstructor
    public static class Album {
        private String id;
        private String title;
        private String productUrl;
        private String coverPhotoBaseUrl;
        private String coverPhotoMediaItemId;
        private Boolean isWriteable;
        private Long mediaItemsCount;
    }
}
