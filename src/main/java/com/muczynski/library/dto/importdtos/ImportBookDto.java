package com.muczynski.library.dto.importdtos;

import com.muczynski.library.domain.BookStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImportBookDto {
    private String title;
    private Integer publicationYear;
    private String publisher;
    private String plotSummary;
    private String relatedWorks;
    private String detailedDescription;
    private LocalDateTime dateAddedToLibrary;
    private LocalDateTime lastModified;
    private BookStatus status;
    private String locNumber;
    private String statusReason;
    private ImportAuthorDto author;
    private String libraryName;
}
