/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(length = Integer.MAX_VALUE)
    private byte[] image;

    private String contentType;

    private String caption;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private Author author;

    private Integer photoOrder;

    // Google Photos export fields
    private String permanentId;  // Google Photos permanent ID

    private LocalDateTime exportedAt;  // Timestamp when photo was exported

    @Enumerated(EnumType.STRING)
    private ExportStatus exportStatus;  // Status of the export

    private String exportErrorMessage;  // Error message if export failed

    private LocalDateTime deletedAt;  // Soft delete timestamp (null if not deleted)

    @Column(length = 64)
    private String imageChecksum;  // SHA-256 checksum of image bytes for duplicate detection

    private LocalDateTime dateTaken;  // Original photo creation time (from Google Photos mediaMetadata)

    public enum ExportStatus {
        PENDING,      // Not yet exported
        IN_PROGRESS,  // Currently being exported
        COMPLETED,    // Successfully exported
        FAILED        // Export failed
    }
}
