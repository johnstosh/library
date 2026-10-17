/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a find-duplicate-titles scan on the Data Management page (Issue #351).
 * Find-only: does not mutate catalog data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DuplicateTitlesResultDto {

    @Builder.Default
    private List<DuplicateTitlePairDto> pairs = new ArrayList<>();

    /** Books considered by the scan. WITHDRAWN rows are omitted. */
    private long booksScanned;

    /** Books kept after dropping WITHDRAWN and collapsing exact copy-suffix duplicates to one representative. */
    private long representativesCompared;

    private String message;

    private String error;
}
