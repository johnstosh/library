/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a Duplicate Titles merge, fix, or undo.
 * {@code actionId} is the new row in {@code duplicate_title_action}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateTitleActionResultDto {

    private Long actionId;
    private String actionKind;
    private String summary;
}
