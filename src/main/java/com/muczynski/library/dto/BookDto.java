/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.domain.BookStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

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
    private LocalDate dateAddedToLibrary;
    private BookStatus status;

    @NotNull(message = "Author ID is required")
    private Long authorId;

    private String author; // Author name for display purposes

    private Long libraryId;
    private Long firstPhotoId;
    private Long loanCount;
    private String locNumber;
    private String statusReason;
}
