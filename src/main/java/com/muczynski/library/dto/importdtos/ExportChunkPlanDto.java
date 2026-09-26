/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Plan for chunked JSON export: section totals and a page size chosen so
 * sequential GETs stay near {@code targetChunks} (~33) under Cloud Run's 600s timeout.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportChunkPlanDto {

    public static final int TARGET_CHUNKS = 33;

    private int targetChunks = TARGET_CHUNKS;
    /** Suggested items per chunk request (shared across sections). */
    private int pageSize;
    private long totalItems;
    private List<SectionPlan> sections = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionPlan {
        private String section;
        private long total;
    }
}
