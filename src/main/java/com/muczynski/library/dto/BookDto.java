package com.muczynski.library.dto;

import com.muczynski.library.domain.BookStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

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
    private List<PhotoDto> photos;
}