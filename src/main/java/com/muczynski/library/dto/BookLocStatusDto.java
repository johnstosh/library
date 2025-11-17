package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for displaying book LOC status in bulk lookup table
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookLocStatusDto {
    private Long id;
    private String title;
    private String authorName;
    private String currentLocNumber;
    private boolean hasLocNumber;
    private Integer publicationYear;
    private Long firstPhotoId;
    private String dateAdded;
}
