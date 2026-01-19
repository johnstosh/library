/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhotoExportInfoDto {
    private Long id;
    private String caption;
    private String exportStatus;
    private LocalDateTime exportedAt;
    private String permanentId;
    private String exportErrorMessage;
    private String contentType;
    private Boolean hasImage;
    private String checksum;

    // Book-related fields
    private String bookTitle;
    private Long bookId;
    private String bookLocNumber;
    private LocalDateTime bookDateAdded;
    private String bookAuthorName;

    // Author-related fields
    private String authorName;
    private Long authorId;
}
