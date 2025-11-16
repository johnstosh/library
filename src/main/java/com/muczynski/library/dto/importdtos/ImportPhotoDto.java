package com.muczynski.library.dto.importdtos;

import com.muczynski.library.domain.Photo;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImportPhotoDto {
    private String contentType;
    private String caption;
    private String bookTitle;  // Reference book by title
    private String bookAuthorName;  // Reference book by author name (for uniqueness)
    private String authorName;  // Reference author by name (for author photos)
    private Integer photoOrder;

    // Google Photos backup fields
    private String permanentId;  // Google Photos permanent ID
    private LocalDateTime backedUpAt;  // Timestamp when photo was backed up
    private Photo.BackupStatus backupStatus;  // Status of the backup
    private String backupErrorMessage;  // Error message if backup failed
}
