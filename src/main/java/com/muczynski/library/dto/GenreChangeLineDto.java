/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row in the genre Recalc table (Issue #385).
 * Drops are listed before renames. A row names the titles when only one, two,
 * or three books share the change. Larger groups are a count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreChangeLineDto {

    /** {@code DROP} or {@code CHANGE}. */
    private String kind;

    /** Stored tag that would be removed or rewritten. */
    private String fromTag;

    /** Canonical tag. Empty for a drop. */
    private String toTag;

    private int titleCount;

    /** Sentence shown in the Recalc table. */
    private String summary;
}
