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
 * Append-only record of a Duplicate Titles merge or fix.
 * Rows are only inserted. Undo adds another row; it does not delete or update
 * the row it reverses. The table is not pruned.
 */
@Entity
@Table(
        name = "duplicate_title_action",
        indexes = @Index(name = "idx_duplicate_title_action_reverses", columnList = "reverses_action_id")
)
@Getter
@Setter
public class DuplicateTitleAction {

    public static final String MERGE_AUTHORS = "MERGE_AUTHORS";
    public static final String MERGE_TITLES = "MERGE_TITLES";
    public static final String FIX_TITLES = "FIX_TITLES";
    public static final String UNDO_MERGE_AUTHORS = "UNDO_MERGE_AUTHORS";
    public static final String UNDO_MERGE_TITLES = "UNDO_MERGE_TITLES";
    public static final String UNDO_FIX_TITLES = "UNDO_FIX_TITLES";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_kind", nullable = false, length = 40)
    private String actionKind;

    @Column(name = "book_a_id")
    private Long bookAId;

    @Column(name = "book_b_id")
    private Long bookBId;

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
