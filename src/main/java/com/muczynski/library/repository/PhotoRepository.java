/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByBookIdOrderByPhotoOrder(Long bookId);
    List<Photo> findByAuthorIdOrderByPhotoOrder(Long authorId);
    List<Photo> findByAuthorId(Long authorId);
    Optional<Photo> findByLoanId(Long loanId);
    long countByLoanId(Long loanId);

    // Get first photo ID for a loan without loading the photo (avoids LOB issue in tests)
    @Query("SELECT p.id FROM Photo p WHERE p.loan.id = :loanId ORDER BY p.id ASC LIMIT 1")
    Long findFirstPhotoIdByLoanId(@Param("loanId") Long loanId);

    // Get first photo checksum for a loan without loading the photo
    @Query("SELECT p.imageChecksum FROM Photo p WHERE p.loan.id = :loanId ORDER BY p.id ASC LIMIT 1")
    String findFirstPhotoChecksumByLoanId(@Param("loanId") Long loanId);
    /** @deprecated Use findAllByPermanentIdOrderByIdAsc() instead to handle duplicates safely. */
    @Deprecated
    Optional<Photo> findByPermanentId(String permanentId);
    /** @deprecated Use findAllByImageChecksumOrderByIdAsc() instead to handle duplicates safely. */
    @Deprecated
    Optional<Photo> findByImageChecksum(String imageChecksum);
    List<Photo> findAllByPermanentIdOrderByIdAsc(String permanentId);
    List<Photo> findAllByImageChecksumOrderByIdAsc(String imageChecksum);
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

    // Pending Export: has checksum (local image) but no permanentId (not uploaded), excluding FAILED
    @Query("SELECT COUNT(p) FROM Photo p WHERE p.deletedAt IS NULL AND p.imageChecksum IS NOT NULL " +
           "AND (p.permanentId IS NULL OR p.permanentId = '') " +
           "AND (p.exportStatus IS NULL OR p.exportStatus <> com.muczynski.library.domain.Photo$ExportStatus.FAILED)")
    long countPendingExportPhotos();

    // Pending Import: has permanentId (uploaded) but no checksum (no local image), excluding FAILED
    @Query("SELECT COUNT(p) FROM Photo p WHERE p.deletedAt IS NULL AND p.permanentId IS NOT NULL AND p.permanentId <> '' " +
           "AND p.imageChecksum IS NULL " +
           "AND (p.exportStatus IS NULL OR p.exportStatus <> com.muczynski.library.domain.Photo$ExportStatus.FAILED)")
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

    // Check if a photo has image data without loading the bytes (checks actual bytes, not just checksum)
    @Query(value = "SELECT CASE WHEN image IS NOT NULL THEN true ELSE false END FROM photo WHERE id = :id", nativeQuery = true)
    boolean hasImageData(@Param("id") Long id);

    // Get first photo ID for a book without loading the photos collection
    @Query("SELECT p.id FROM Photo p WHERE p.book.id = :bookId ORDER BY p.photoOrder ASC LIMIT 1")
    Long findFirstPhotoIdByBookId(@Param("bookId") Long bookId);

    // Get first photo checksum for a book without loading the photos collection
    @Query("SELECT p.imageChecksum FROM Photo p WHERE p.book.id = :bookId ORDER BY p.photoOrder ASC LIMIT 1")
    String findFirstPhotoChecksumByBookId(@Param("bookId") Long bookId);

    // Get first photo ID for an author without loading the photos collection
    @Query("SELECT p.id FROM Photo p WHERE p.author.id = :authorId AND p.book IS NULL ORDER BY p.photoOrder ASC LIMIT 1")
    Long findFirstPhotoIdByAuthorId(@Param("authorId") Long authorId);

    // Get first photo checksum for an author without loading the photos collection
    @Query("SELECT p.imageChecksum FROM Photo p WHERE p.author.id = :authorId AND p.book IS NULL ORDER BY p.photoOrder ASC LIMIT 1")
    String findFirstPhotoChecksumByAuthorId(@Param("authorId") Long authorId);

    // Projection query that returns only metadata without image bytes - for export operations
    // Note: Using findBy() with no criteria returns all records with the projection type
    List<PhotoMetadataProjection> findBy();

    /**
     * Efficient flat projection for the photo export list page.
     * Single native SQL query: no image blob loaded, no N+1 queries.
     * Joins book, book's author, and direct author in one round-trip.
     */
    @Query(value = """
            SELECT
                p.id             AS id,
                p.content_type   AS contentType,
                p.caption        AS caption,
                p.permanent_id   AS permanentId,
                p.exported_at    AS exportedAt,
                p.export_status  AS exportStatus,
                p.export_error_message AS exportErrorMessage,
                p.image_checksum AS imageChecksum,
                p.deleted_at     AS deletedAt,
                b.id             AS bookId,
                b.title          AS bookTitle,
                b.loc_number     AS bookLocNumber,
                b.date_added_to_library AS bookDateAdded,
                ba.id            AS bookAuthorId,
                ba.name          AS bookAuthorName,
                a.id             AS authorId,
                a.name           AS authorName
            FROM photo p
            LEFT JOIN book b   ON p.book_id   = b.id
            LEFT JOIN author ba ON b.author_id = ba.id
            LEFT JOIN author a  ON p.author_id = a.id
            WHERE p.deleted_at IS NULL
            ORDER BY b.date_added_to_library DESC NULLS LAST, p.id
            """,
            nativeQuery = true)
    List<PhotoExportFlatProjection> findAllForExportPage();

    /**
     * Efficient flat projection for a single photo on the export page.
     * Same query as findAllForExportPage but filtered to one photo ID.
     */
    @Query(value = """
            SELECT
                p.id             AS id,
                p.content_type   AS contentType,
                p.caption        AS caption,
                p.permanent_id   AS permanentId,
                p.exported_at    AS exportedAt,
                p.export_status  AS exportStatus,
                p.export_error_message AS exportErrorMessage,
                p.image_checksum AS imageChecksum,
                p.deleted_at     AS deletedAt,
                b.id             AS bookId,
                b.title          AS bookTitle,
                b.loc_number     AS bookLocNumber,
                b.date_added_to_library AS bookDateAdded,
                ba.id            AS bookAuthorId,
                ba.name          AS bookAuthorName,
                a.id             AS authorId,
                a.name           AS authorName
            FROM photo p
            LEFT JOIN book b   ON p.book_id   = b.id
            LEFT JOIN author ba ON b.author_id = ba.id
            LEFT JOIN author a  ON p.author_id = a.id
            WHERE p.deleted_at IS NULL
              AND p.id = :photoId
            """,
            nativeQuery = true)
    Optional<PhotoExportFlatProjection> findByIdForExportPage(@Param("photoId") Long photoId);

    // Find active photos that have images for ZIP export
    // Note: This loads full Photo entities including image bytes - use only for actual export
    @Query("SELECT DISTINCT p FROM Photo p " +
           "LEFT JOIN FETCH p.book b LEFT JOIN FETCH b.author " +
           "LEFT JOIN FETCH p.author " +
           "LEFT JOIN FETCH p.loan l LEFT JOIN FETCH l.book LEFT JOIN FETCH l.user " +
           "WHERE p.deletedAt IS NULL AND (p.imageChecksum IS NOT NULL OR p.image IS NOT NULL) " +
           "ORDER BY p.id")
    List<Photo> findActivePhotosWithImages();

    // Find photo IDs that have images (for streaming ZIP export)
    // Include photos with image data even if checksum hasn't been computed yet
    @Query("SELECT p.id FROM Photo p WHERE p.deletedAt IS NULL AND (p.imageChecksum IS NOT NULL OR p.image IS NOT NULL) ORDER BY p.id")
    List<Long> findActivePhotoIdsWithImages();

    // Find ALL active photo IDs (for ZIP export that downloads missing images from Google Photos)
    @Query("SELECT p.id FROM Photo p WHERE p.deletedAt IS NULL ORDER BY p.id")
    List<Long> findAllActivePhotoIds();

    // Count photos that have actual image bytes stored locally.
    // Unlike imageChecksum (which can be set by JSON import without storing bytes),
    // image IS NOT NULL means the bytes are physically present in the database.
    @Query("SELECT COUNT(p) FROM Photo p WHERE p.deletedAt IS NULL AND p.image IS NOT NULL")
    long countPhotosWithActualImageBytes();

    // Find IDs of photos that have local image bytes but no checksum (need checksum backfill)
    @Query(value = "SELECT id FROM photo WHERE deleted_at IS NULL AND image IS NOT NULL AND image_checksum IS NULL", nativeQuery = true)
    List<Long> findIdsWithLocalImageButNoChecksum();

    // Update just the checksum for a single photo (efficient — no blob round-trip)
    @Modifying
    @Query("UPDATE Photo p SET p.imageChecksum = :checksum WHERE p.id = :id")
    void updateImageChecksum(@Param("id") Long id, @Param("checksum") String checksum);
}