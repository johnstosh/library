/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A librarian mark that two catalog works are not duplicates.
 * Stored by folded title and normalized author, not by book id, so every
 * extra copy of that pair stays out of later scans.
 */
@Entity
@Table(
        name = "duplicate_title_dismissal",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_duplicate_title_dismissal_pair_key",
                columnNames = "pair_key"
        )
)
@Getter
@Setter
public class DuplicateTitleDismissal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 of the canonical folded-title pair. */
    @Column(name = "pair_key", nullable = false, length = 64)
    private String pairKey;

    /** Folded primary title, lowercased, lexicographically first side. */
    @Column(name = "title_a", nullable = false, length = 512)
    private String titleA;

    /** Normalized author for {@link #titleA}. Blank when the book has no author. */
    @Column(name = "author_a", nullable = false, length = 512)
    private String authorA;

    /** Folded primary title, lowercased, lexicographically second side. */
    @Column(name = "title_b", nullable = false, length = 512)
    private String titleB;

    /** Normalized author for {@link #titleB}. Blank when the book has no author. */
    @Column(name = "author_b", nullable = false, length = 512)
    private String authorB;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
