/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Book ids a catalog tool should check. The page previews them in small batches.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogToolCandidatesDto {

    private List<Long> bookIds;
    /** Author ids for the author Grokipedia tool. Book tools leave this empty. */
    private List<Long> authorIds;
}
