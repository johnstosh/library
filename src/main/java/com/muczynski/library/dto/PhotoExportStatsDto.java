/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

@Data
public class PhotoExportStatsDto {
    private Long total;
    private Long exported;
    private Long imported;
    private Long pendingExport;
    private Long pendingImport;
    private Long failed;
    private Long inProgress;

    // Legacy fields for backwards compatibility
    private Long completed;
    private Long pending;

    private String albumName;
    private String albumId;
}
