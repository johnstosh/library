/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for album operations in Google Photos Library API
 */
@Data
@NoArgsConstructor
public class AlbumResponse {
    private Album album;

    @Data
    @NoArgsConstructor
    public static class Album {
        private String id;
        private String title;
        private String description;
        private String productUrl;
    }
}
