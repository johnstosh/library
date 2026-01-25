/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.domain.BookStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookDto {
    private Long id;

    @NotBlank(message = "Book title is required")
    private String title;

    private Integer publicationYear;
    private String publisher;
    private String plotSummary;
    private String relatedWorks;
    private String detailedDescription;
    private String grokipediaUrl;
    private String freeTextUrl;
    private LocalDateTime dateAddedToLibrary;
    private LocalDateTime lastModified;
    private BookStatus status;

    private Long authorId;  // Nullable - can be set during AI processing

    private String author; // Author name for display purposes

    private Long libraryId;
    private String library; // Library name for display purposes
    private Long firstPhotoId;
    private String firstPhotoChecksum;
    private Long loanCount;
    private String locNumber;
    private String statusReason;

    /**
     * List of tags for categorizing the book.
     * Tags should be lowercase with only letters, numbers, and dashes.
     */
    private List<String> tagsList;
}
