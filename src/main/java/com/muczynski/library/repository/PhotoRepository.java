/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByBookIdOrderByPhotoOrder(Long bookId);
    List<Photo> findByAuthorIdOrderByPhotoOrder(Long authorId);
    List<Photo> findByAuthorId(Long authorId);
    Optional<Photo> findByPermanentId(String permanentId);
    Optional<Photo> findByImageChecksum(String imageChecksum);
    List<Photo> findByImageChecksumIsNull();
    List<Photo> findByBookIdAndPhotoOrderOrderByIdAsc(Long bookId, Integer photoOrder);
    List<Photo> findByAuthorIdAndBookIsNullAndPhotoOrderOrderByIdAsc(Long authorId, Integer photoOrder);

    @Query("SELECT DISTINCT p FROM Photo p LEFT JOIN FETCH p.book LEFT JOIN FETCH p.author")
    List<Photo> findAllWithBookAndAuthor();

    @Query("SELECT DISTINCT p FROM Photo p LEFT JOIN FETCH p.book LEFT JOIN FETCH p.author ORDER BY p.id")
    List<Photo> findAllWithBookAndAuthorOrderById();

    // Efficient queries that don't load image bytes

    @Query("SELECT p.id FROM Photo p WHERE p.imageChecksum IS NULL")
    List<Long> findIdsWithoutChecksum();

    // Count queries for export stats (avoid loading image bytes)
    @Query("SELECT COUNT(p) FROM Photo p WHERE p.deletedAt IS NULL")
    long countActivePhotos();

    @Query("SELECT COUNT(p) FROM Photo p WHERE p.deletedAt IS NULL AND p.permanentId IS NOT NULL AND p.permanentId <> ''")
    long countExportedPhotos();

    @Query("SELECT COUNT(p) FROM Photo p WHERE p.deletedAt IS NULL AND p.permanentId IS NOT NULL AND p.permanentId <> '' AND p.imageChecksum IS NOT NULL")
    long countImportedPhotos();

    @Query("SELECT COUNT(p) FROM Photo p WHERE p.deletedAt IS NULL AND p.imageChecksum IS NOT NULL AND (p.permanentId IS NULL OR p.permanentId = '')")
    long countPendingExportPhotos();

    @Query("SELECT COUNT(p) FROM Photo p WHERE p.deletedAt IS NULL AND p.permanentId IS NOT NULL AND p.permanentId <> '' AND p.imageChecksum IS NULL")
    long countPendingImportPhotos();

    @Query("SELECT COUNT(p) FROM Photo p WHERE p.deletedAt IS NULL AND p.exportStatus = :status")
    long countByExportStatus(@Param("status") Photo.ExportStatus status);

    // Find photo IDs needing export (have checksum but no permanentId)
    @Query("SELECT p.id FROM Photo p WHERE p.deletedAt IS NULL AND p.imageChecksum IS NOT NULL " +
           "AND (p.permanentId IS NULL OR p.permanentId = '') " +
           "AND (p.exportStatus IS NULL OR p.exportStatus = com.muczynski.library.domain.Photo$ExportStatus.PENDING " +
           "OR p.exportStatus = com.muczynski.library.domain.Photo$ExportStatus.FAILED)")
    List<Long> findIdsNeedingExport();

    // Find photo IDs needing import (have permanentId but no checksum)
    @Query("SELECT p.id FROM Photo p WHERE p.deletedAt IS NULL " +
           "AND p.permanentId IS NOT NULL AND p.permanentId <> '' " +
           "AND p.imageChecksum IS NULL")
    List<Long> findIdsNeedingImport();

    // Check if a photo has image data without loading the bytes
    @Query("SELECT CASE WHEN p.imageChecksum IS NOT NULL THEN true ELSE false END FROM Photo p WHERE p.id = :id")
    boolean hasImageData(@Param("id") Long id);

    // Get first photo ID for a book without loading the photos collection
    @Query("SELECT p.id FROM Photo p WHERE p.book.id = :bookId ORDER BY p.photoOrder ASC LIMIT 1")
    Long findFirstPhotoIdByBookId(@Param("bookId") Long bookId);

    // Get first photo ID for an author without loading the photos collection
    @Query("SELECT p.id FROM Photo p WHERE p.author.id = :authorId AND p.book IS NULL ORDER BY p.photoOrder ASC LIMIT 1")
    Long findFirstPhotoIdByAuthorId(@Param("authorId") Long authorId);

    // Projection query that returns only metadata without image bytes - for export operations
    List<PhotoMetadataProjection> findAllProjectedBy();
}