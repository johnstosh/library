/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    indexes = {
        @Index(name = "idx_author_name", columnList = "name")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_author_name", columnNames = "name")
    }
)
@Getter
@Setter
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /**
     * Other catalog forms of this same person (pen name, saint's name, Latin
     * name, spelling the catalogs use). PostgreSQL {@code text[]}. Not a place
     * for punctuation cleanup of {@link #name}.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "alternate_names", columnDefinition = "text[]")
    private List<String> alternateNames;

    private LocalDate dateOfBirth;

    private LocalDate dateOfDeath;

    @Lob
    private String religiousAffiliation;

    @Lob
    private String birthCountry;

    @Lob
    private String nationality;

    @Lob
    @Column(name = "brief_biography")
    private String biographicalEssay;

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
