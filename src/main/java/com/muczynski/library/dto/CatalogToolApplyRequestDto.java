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
 * The change list Recalc showed. Do It applies this list after checking each row still matches.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogToolApplyRequestDto {

    private List<CatalogToolChangeDto> changes;
}
