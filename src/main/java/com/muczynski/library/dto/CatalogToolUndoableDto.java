/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The latest Do It for one tool that has not been undone. {@code actionId} is null when there is none.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogToolUndoableDto {

    private Long actionId;
    private Integer changeCount;
    private String summary;
}
