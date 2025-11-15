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

    // Google Photos backup fields
    private String permanentId;  // Google Photos permanent ID

    private LocalDateTime backedUpAt;  // Timestamp when photo was backed up

    @Enumerated(EnumType.STRING)
    private BackupStatus backupStatus;  // Status of the backup

    private String backupErrorMessage;  // Error message if backup failed

    public enum BackupStatus {
        PENDING,      // Not yet backed up
        IN_PROGRESS,  // Currently being backed up
        COMPLETED,    // Successfully backed up
        FAILED        // Backup failed
    }
}
