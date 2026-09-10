/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * One named favorite-list membership for a user and either a book or an author.
 * Exactly one of {@code book} or {@code author} is set.
 */
@Entity
@Table(
    name = "favorites",
    indexes = {
        @Index(name = "idx_favorite_user", columnList = "user_id"),
        @Index(name = "idx_favorite_book", columnList = "book_id"),
        @Index(name = "idx_favorite_author", columnList = "author_id"),
        @Index(name = "idx_favorite_list", columnList = "list_name")
    }
)
@Getter
@Setter
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Author author;

    @Column(name = "list_name", nullable = false)
    private String listName;

    private LocalDateTime lastModified;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
        if ((book == null) == (author == null)) {
            throw new IllegalStateException("Favorite must reference exactly one of book or author");
        }
    }
}
