/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Named book-count statistics for the Data Management availability section.
 * Boolean availability fields are counted only when true (null/false are excluded).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookAvailabilityStatsDto {
    private long electronicResource;
    private long hasCallNumber;
    private long withdrawn;
    private long availableAtYdl;
    private long ydlPaper;
    private long ydlEbook;
    private long ydlAudio;
    private long availableAtEmu;
    private long emuPaper;
    private long emuEbook;
    private long emuAudio;
}
