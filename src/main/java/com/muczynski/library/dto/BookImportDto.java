package com.muczynski.library.dto;

import com.muczynski.library.domain.BookStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class BookImportDto {
    private String title;
    private Integer publicationYear;
    private String publisher;
    private String plotSummary;
    private String relatedWorks;
    private String detailedDescription;
    private LocalDate dateAddedToLibrary;
    private BookStatus status;
    private String locNumber;
    private String statusReason;
    private String authorName;
    private String libraryName;
    private List<PhotoImportDto> photos = new ArrayList<>();
}
