/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeInfoDto {
    private String uploadId;
    private int resumeFromChunkIndex;
    private long bytesToSkipInChunk;
    private int totalProcessed;
    private int successCount;
    private int failureCount;
    private int skippedCount;
}
