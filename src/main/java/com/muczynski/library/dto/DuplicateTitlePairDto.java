/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One near-duplicate title pair from the Data Management duplicate scan (Issue #351).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DuplicateTitlePairDto {

    /** Jaro–Winkler closeness in [0, 1]; higher is closer. */
    private double score;

    private Long bookAId;
    private String bookATitle;
    private String bookAAlternateTitle;
    private String bookAAuthorName;
    /** BookStatus name: ACTIVE, WITHDRAWN, etc. */
    private String bookAStatus;

    private Long bookBId;
    private String bookBTitle;
    private String bookBAlternateTitle;
    private String bookBAuthorName;
    /** BookStatus name: ACTIVE, WITHDRAWN, etc. */
    private String bookBStatus;

    /** The specific title strings (primary or alternate) that produced the max score. */
    private String matchedTitleA;
    private String matchedTitleB;
}
