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
 * Result of Do It or Undo. {@code actionId} is the new row in {@code catalog_maintenance_action}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogToolApplyResultDto {

    private Long actionId;
    private String actionKind;
    private int applied;
    private int skipped;
    private String message;
    private String error;
    private List<CatalogToolChangeDto> changes;
    private List<CatalogToolChangeDto> skippedChanges;
}
