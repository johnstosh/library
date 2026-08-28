/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

@Entity
@Table(
    indexes = {
        @Index(name = "idx_book_title", columnList = "title")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_book_title", columnNames = "title")
    }
)
@Getter
@Setter
public class Book {

    /**
     * Trailing copy-number suffix our catalog appends to disambiguate duplicate
     * titles, e.g. ", c. 2", ", c.2", ", c 3". External catalogs (YDL, EMU) do
     * not carry this suffix on their titles.
     */
    private static final Pattern COPY_SUFFIX_PATTERN =
            Pattern.compile("(?i),\\s*c\\.?\\s*\\d+\\s*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private Integer publicationYear;

    private String publisher;

    @Lob
    @Column(name = "plot_summary")
    private String plotEssay;

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

    private Boolean electronicResource = false;

    private String statusReason;

    private Boolean ydlAudioAvailable;

    private Boolean ydlPaperAvailable;

    private Boolean ydlEbookAvailable;

    private LocalDateTime ydlLastChecked;

    private String ydlLookupError;

    private Boolean emuAudioAvailable;

    private Boolean emuPaperAvailable;

    private Boolean emuEbookAvailable;

    private LocalDateTime emuLastChecked;

    private String emuLookupError;

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

    /**
     * Strips a trailing copy-number suffix of the form {@code ", c. N"} (any
     * spacing or optional period after {@code c}). Returns the original string
     * when there is no suffix, or when stripping would leave the title empty.
     */
    public static String stripCopySuffix(String title) {
        if (title == null || title.isBlank()) {
            return title;
        }
        String trimmed = title.trim();
        String stripped = COPY_SUFFIX_PATTERN.matcher(trimmed).replaceFirst("").trim();
        return stripped.isEmpty() ? trimmed : stripped;
    }

    /**
     * This book's title with any trailing {@code ", c. N"} copy-number suffix removed.
     */
    public String titleWithoutCopySuffix() {
        return stripCopySuffix(title);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (dateAddedToLibrary == null) {
            dateAddedToLibrary = now;
        }
        lastModified = now;
    }

    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now(ZoneOffset.UTC);
    }
}
