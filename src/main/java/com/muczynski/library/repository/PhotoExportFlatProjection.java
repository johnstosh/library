/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import java.time.LocalDateTime;

/**
 * Flat (non-nested) projection for photo export list page.
 * Uses a native SQL query to select only metadata columns – never loads the image blob.
 * Avoids the N+1 query problem caused by nested Spring Data JPA projections.
 */
public interface PhotoExportFlatProjection {
    Long getId();
    String getContentType();
    String getCaption();
    String getPermanentId();
    LocalDateTime getExportedAt();
    String getExportStatus();         // returned as String from native query
    String getExportErrorMessage();
    String getImageChecksum();
    LocalDateTime getDeletedAt();

    // Book fields (null if photo has no book)
    Long getBookId();
    String getBookTitle();
    String getBookLocNumber();
    LocalDateTime getBookDateAdded();

    // Book's author (null if book has no author)
    Long getBookAuthorId();
    String getBookAuthorName();

    // Direct author (null if photo has no direct author)
    Long getAuthorId();
    String getAuthorName();
}
