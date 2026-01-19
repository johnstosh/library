/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for database statistics.
 * Returns total counts from the database for use in the Data Management page.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseStatsDto {
    private Long libraryCount;
    private Long bookCount;
    private Long authorCount;
    private Long userCount;
    private Long loanCount;
}
