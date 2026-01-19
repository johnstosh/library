/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch getting media items in Google Photos Library API
 * Used with POST /v1/mediaItems:batchGet endpoint
 */
@Data
@NoArgsConstructor
public class BatchGetRequest {
    private List<String> mediaItemIds;
}
