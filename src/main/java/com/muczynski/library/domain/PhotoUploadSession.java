/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Persists chunked photo upload progress so imports can resume after Cloud Run reboots.
 */
@Entity
@Table(name = "photo_upload_session")
@Getter
@Setter
public class PhotoUploadSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uploadId;

    private int totalProcessed;
    private int successCount;
    private int failureCount;
    private int skippedCount;
    private int lastChunkIndex;

    /** Bytes consumed by ZipInputStream through last completed entry */
    private long totalBytesConsumed;

    private boolean complete;

    private Instant createdAt;
    private Instant lastActivityAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        lastActivityAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        lastActivityAt = Instant.now();
    }
}
