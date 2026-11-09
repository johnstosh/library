/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.Instant;

/**
 * Append-only record of a Data Management catalog tool (Issue #386).
 * Undo inserts another row that points at the forward row. Rows are not updated or deleted.
 * The table is not part of JSON import/export.
 */
@Entity
@Table(
        name = "catalog_maintenance_action",
        indexes = @Index(name = "idx_catalog_maintenance_action_reverses", columnList = "reverses_action_id")
)
@Getter
@Setter
public class CatalogMaintenanceAction {

    public static final String CHICAGO_TITLES = "CHICAGO_TITLES";
    public static final String GROKIPEDIA_URLS = "GROKIPEDIA_URLS";
    public static final String GROKIPEDIA_AUTHOR_URLS = "GROKIPEDIA_AUTHOR_URLS";
    public static final String FREE_TEXT_URLS = "FREE_TEXT_URLS";
    public static final String UNDO_CHICAGO_TITLES = "UNDO_CHICAGO_TITLES";
    public static final String UNDO_GROKIPEDIA_URLS = "UNDO_GROKIPEDIA_URLS";
    public static final String UNDO_GROKIPEDIA_AUTHOR_URLS = "UNDO_GROKIPEDIA_AUTHOR_URLS";
    public static final String UNDO_FREE_TEXT_URLS = "UNDO_FREE_TEXT_URLS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_kind", nullable = false, length = 40)
    private String actionKind;

    /** Set only on an undo row. Points at the forward row it reverses. */
    @Column(name = "reverses_action_id")
    private Long reversesActionId;

    @Lob
    @Column(name = "summary", columnDefinition = "text")
    @JdbcTypeCode(java.sql.Types.LONGVARCHAR)
    private String summary;

    @Lob
    @Column(name = "snapshot_json", columnDefinition = "text")
    @JdbcTypeCode(java.sql.Types.LONGVARCHAR)
    private String snapshotJson;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
