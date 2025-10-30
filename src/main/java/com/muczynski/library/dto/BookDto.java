/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.domain.BookStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookDto {
    private Long id;
    private String title;
    private Integer publicationYear;
    private String publisher;
    private String plotSummary;
    private String relatedWorks;
    private String detailedDescription;
    private LocalDate dateAddedToLibrary;
    private BookStatus status;
    private Long authorId;
    private Long libraryId;
    private Long firstPhotoId;
    private Long loanCount;
    private String locNumber;
    private String statusReason;
}
