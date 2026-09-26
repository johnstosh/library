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
 * One page of a catalog section for chunked JSON export.
 * Clients page with {@code afterId} (exclusive keyset cursor) until {@code hasMore} is false.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportChunkDto {

    private String section;
    /** Exclusive lower bound on entity id (0 = start). */
    private long afterId;
    private int limit;
    /** Id of the last item in this page; pass as next afterId. Null when empty. */
    private Long nextAfterId;
    private long total;
    private int count;
    private boolean hasMore;
    private List<Object> items = new ArrayList<>();
}
