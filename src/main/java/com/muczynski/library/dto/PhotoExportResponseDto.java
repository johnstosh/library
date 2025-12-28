/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

@Data
public class PhotoExportResponseDto {
    private String message;
    private Long photoId;
    private PhotoExportStatsDto stats;
}
