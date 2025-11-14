/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Google Photos Library API integration
 */
@Configuration
@ConfigurationProperties(prefix = "google.photos")
@Data
public class PhotoStorageConfig {
    /**
     * Base URL for Google Photos Library API (v1)
     */
    private String baseUrl = "https://photoslibrary.googleapis.com/v1";

    /**
     * Upload endpoint for uploading photo bytes
     */
    private String uploadUrl = "https://photoslibrary.googleapis.com/v1/uploads";

    /**
     * Base URL for Google Photos Picker API
     */
    private String pickerBaseUrl = "https://photospicker.googleapis.com/v1";

    /**
     * Album ID for storing book covers (app-created album)
     * Should be configured after creating the album initially
     */
    private String bookCoversAlbumId;

    /**
     * Album ID for storing author photos (app-created album)
     * Should be configured after creating the album initially
     */
    private String authorPhotosAlbumId;

    /**
     * Default page size for paginated API requests
     */
    private Integer defaultPageSize = 100;

    /**
     * Whether to cache baseUrls locally (they expire after ~1 hour)
     */
    private Boolean cacheBaseUrls = true;

    /**
     * Cache duration for baseUrls in minutes (max 60, recommended 50 for safety)
     */
    private Integer baseUrlCacheDurationMinutes = 50;
}
