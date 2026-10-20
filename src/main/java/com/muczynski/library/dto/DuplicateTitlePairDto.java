/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
    /** BookStatus name of a book included in the scan. WITHDRAWN is omitted. */
    private String bookAStatus;

    private Long bookBId;
    private String bookBTitle;
    private String bookBAlternateTitle;
    private String bookBAuthorName;
    /** BookStatus name of a book included in the scan. WITHDRAWN is omitted. */
    private String bookBStatus;

    /** The specific title strings (primary or alternate) that produced the max score. */
    private String matchedTitleA;
    private String matchedTitleB;

    /** {@code title} or {@code alternate title} for the strings that scored highest. */
    private String matchedFieldA;
    private String matchedFieldB;

    private Long bookAAuthorId;
    private Long bookBAuthorId;
    @Builder.Default
    private List<String> bookAAuthorAlternateNames = new ArrayList<>();
    @Builder.Default
    private List<String> bookBAuthorAlternateNames = new ArrayList<>();

    private String bookALocNumber;
    private String bookBLocNumber;
    private Boolean bookAElectronicResource;
    private Boolean bookBElectronicResource;

    /** Books-page filter names, such as In-library or Electronic resource. */
    @Builder.Default
    private List<String> bookACatalogLabels = new ArrayList<>();
    @Builder.Default
    private List<String> bookBCatalogLabels = new ArrayList<>();

    private String titleDifference;
    private String authorDifference;

    /** {@code MERGE_TITLES} or {@code FIX_TITLES}. */
    private String titleAction;
    private boolean titleChangeNeeded;
    private String titlePlan;
    private boolean authorMergeAvailable;
    private String authorPlan;
}
