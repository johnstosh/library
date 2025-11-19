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

    // Nested projections for related entities
    BookProjection getBook();
    AuthorProjection getAuthor();

    interface BookProjection {
        String getTitle();
        AuthorProjection getAuthor();
    }

    interface AuthorProjection {
        String getName();
    }
}
