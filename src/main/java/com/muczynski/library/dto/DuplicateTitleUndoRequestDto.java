/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

/**
 * Identifies one Duplicate Titles action row to reverse.
 * Reversal inserts a new row. The original row stays.
 */
@Data
public class DuplicateTitleUndoRequestDto {

    private Long actionId;
}
