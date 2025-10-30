/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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

    private LocalDate dateAddedToLibrary;

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
}
