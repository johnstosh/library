/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Photo;

import java.time.LocalDateTime;

/**
 * Projection interface for Photo metadata that excludes the image bytes.
 * Used for export operations to avoid OutOfMemoryError when loading all photos.
 */
public interface PhotoMetadataProjection {
    Long getId();
    String getContentType();
    String getCaption();
    Integer getPhotoOrder();
    String getPermanentId();
    LocalDateTime getExportedAt();
    Photo.ExportStatus getExportStatus();
    String getExportErrorMessage();
    String getImageChecksum();  // Used to determine if photo has image data
    LocalDateTime getDeletedAt();  // Used to filter out soft-deleted photos

    // Nested projections for related entities
    BookProjection getBook();
    AuthorProjection getAuthor();

    interface BookProjection {
        Long getId();
        String getTitle();
        String getLocNumber();
        java.time.LocalDateTime getDateAddedToLibrary();
        AuthorProjection getAuthor();
    }

    interface AuthorProjection {
        Long getId();
        String getName();
    }
}
