/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Titles and authors from one row of the duplicate-catalog table.
 * The server folds copy and volume suffixes before storing the mark.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateTitleDismissalRequestDto {

    private String titleA;
    private String authorA;
    private String titleB;
    private String authorB;
}
