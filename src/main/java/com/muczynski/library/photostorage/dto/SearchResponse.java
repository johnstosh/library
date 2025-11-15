/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for search media items operation in Google Photos Library API
 */
@Data
@NoArgsConstructor
public class SearchResponse {
    private List<MediaItemResponse> mediaItems;
    private String nextPageToken;
}
