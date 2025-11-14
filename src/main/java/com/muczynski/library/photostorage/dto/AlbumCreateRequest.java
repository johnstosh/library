/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new album in Google Photos Library API
 */
@Data
@NoArgsConstructor
public class AlbumCreateRequest {
    private Album album;

    @Data
    @NoArgsConstructor
    public static class Album {
        private String title;
        private String description;
    }
}
