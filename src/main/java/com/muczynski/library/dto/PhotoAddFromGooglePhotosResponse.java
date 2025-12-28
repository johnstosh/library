/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PhotoAddFromGooglePhotosResponse {
    private int savedCount;
    private int failedCount;
    private List<PhotoDto> savedPhotos;
    private List<Map<String, Object>> failedPhotos;
}
