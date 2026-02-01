/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.muczynski.library.domain.Photo;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ImportPhotoDto {
    // SHA-256 checksum of image bytes for photo identification/matching
    private String imageChecksum;

    private String contentType;
    private String caption;
    private String bookTitle;  // Reference book by title
    private String bookAuthorName;  // Reference book by author name (for uniqueness)
    private String authorName;  // Reference author by name (for author photos)
    private Integer photoOrder;

    // Google Photos export fields
    private String permanentId;  // Google Photos permanent ID
    private LocalDateTime exportedAt;  // Timestamp when photo was backed up
    private Photo.ExportStatus exportStatus;  // Status of the export
    private String exportErrorMessage;  // Error message if export failed
}
