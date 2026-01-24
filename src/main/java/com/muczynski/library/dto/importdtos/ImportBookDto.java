/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.muczynski.library.domain.BookStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ImportBookDto {
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
    private String locNumber;
    private String statusReason;
    private String libraryName;

    // New format: author reference by name only
    private String authorName;

    // Old format (deprecated): embedded author object for backward compatibility during import
    // Export uses authorName only; this field is only read during import
    private ImportAuthorDto author;
}
