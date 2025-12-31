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

    private LocalDateTime dateAddedToLibrary;

    private LocalDateTime lastModified;

    @Enumerated(EnumType.STRING)
    private BookStatus status;

    private String locNumber;

    private String statusReason;

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
