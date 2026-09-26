/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for illegal genres maintenance operations on the Data Management page.
 * Used for both recalc (count) and cleanup actions.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IllegalGenresMaintenanceDto {

    /** Number of books that have at least one illegal or mismatched genre tag (for Count column). */
    private long booksAffected;

    /** Total books scanned during cleanup. */
    private long booksScanned;

    /** Number of books whose tags were updated. */
    private long booksUpdated;

    /** Total number of plural/spelling corrections performed across all books. */
    private long pluralCorrections;

    /** Total number of illegal tags removed. */
    private long illegalRemoved;

    /** Human-readable summary message for Results column. */
    private String message;

    /** Optional error message if operation failed. */
    private String error;
}
