/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.dto.PhotoZipImportResultDto.PhotoZipImportItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadResultDto {
    private String uploadId;
    private int chunkIndex;
    private List<PhotoZipImportItemDto> processedPhotos;
    private int totalProcessedSoFar;
    private int totalSuccessSoFar;
    private int totalFailureSoFar;
    private int totalSkippedSoFar;
    private boolean complete;
    private PhotoZipImportResultDto finalResult;
}
