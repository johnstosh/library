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
 * Dry-run result for a Data Management catalog tool. Nothing has been saved.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogToolPreviewDto {

    private int booksScanned;
    private int changeCount;
    private String message;
    private String error;
    private List<CatalogToolChangeDto> changes;
    private List<CatalogToolChangeDto> skipped;
}
