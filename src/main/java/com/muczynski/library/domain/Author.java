/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Entity
@Table(indexes = {
    @Index(name = "idx_author_name", columnList = "name")
})
@Getter
@Setter
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDate dateOfBirth;

    private LocalDate dateOfDeath;

    @Lob
    private String religiousAffiliation;

    @Lob
    private String birthCountry;

    @Lob
    private String nationality;

    @Lob
    private String briefBiography;

    private String grokipediaUrl;

    private LocalDateTime lastModified;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Photo> photos = new ArrayList<>();

    @OneToMany(mappedBy = "author")
    private java.util.List<Book> books = new ArrayList<>();

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }
}
