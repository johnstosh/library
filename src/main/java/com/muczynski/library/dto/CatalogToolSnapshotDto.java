/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * JSON stored on a catalog maintenance action so Undo can put the previous values back.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogToolSnapshotDto {

    private List<CatalogToolChangeDto> changes;
}
