/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
    @Index(name = "idx_book_title", columnList = "title")
})
@Getter
@Setter
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private Integer publicationYear;

    private String publisher;

    @Lob
    private String plotSummary;

    @Lob
    private String relatedWorks;

    @Lob
    private String detailedDescription;

    private String grokipediaUrl;

    /**
     * Space-separated list of URLs where free online text can be found.
     * Using @Lob with explicit LONGVARCHAR type for PostgreSQL compatibility.
     * This avoids Hibernate 6's default OID handling for LOBs in PostgreSQL.
     */
    @Lob
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.LONGVARCHAR)
    private String freeTextUrl;

    private LocalDateTime dateAddedToLibrary;

    private LocalDateTime lastModified;

    @Enumerated(EnumType.STRING)
    private BookStatus status;

    private String locNumber;

    private String statusReason;

    /**
     * List of tags for categorizing the book (e.g., fiction, fantasy, theology).
     * Tags should be lowercase with only letters, numbers, and dashes.
     */
    @ElementCollection
    @CollectionTable(name = "book_tags", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "tag")
    private java.util.List<String> tagsList = new java.util.ArrayList<>();

    @ManyToOne
    private Author author;

    @ManyToOne
    private Library library;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Photo> photos;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (dateAddedToLibrary == null) {
            dateAddedToLibrary = now;
        }
        lastModified = now;
    }

    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }
}
