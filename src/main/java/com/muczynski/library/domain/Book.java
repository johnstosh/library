/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
     * titles, e.g. ", c. 2", ", c.2", ", c 3". External catalogs (YDL, EMU, ACLA, WCC, MSU) do
     * not carry this suffix on their titles.
     */
    private static final Pattern COPY_SUFFIX_PATTERN =
            Pattern.compile("(?i),\\s*c\\.?\\s*\\d+\\s*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    /**
     * Other wordings of this same work (original language, another English
     * title, a cover or spine title). PostgreSQL {@code text[]}. Not unique.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "alternate_titles", columnDefinition = "text[]")
    private List<String> alternateTitles;

    private Integer publicationYear;

    private String publisher;

    /**
     * PostgreSQL {@code text}. LONGVARCHAR keeps JDBC {@code getString}.
     * Without {@code columnDefinition}, Hibernate casts an existing {@code oid}
     * to the object id before startup can copy the large object.
     */
    @Lob
    @Column(name = "plot_summary", columnDefinition = "text")
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.LONGVARCHAR)
    private String plotEssay;

    @Lob
    private String relatedWorks;

    /**
     * Same text mapping as {@link #plotEssay}.
     */
    @Lob
    @Column(columnDefinition = "text")
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.LONGVARCHAR)
    private String detailedDescription;

    /**
     * Space-separated Grokipedia page URLs for the primary title and alternate titles.
     * {@code text} so more than one page fits. {@code ddl-auto=update} does not widen varchar.
     */
    @Column(columnDefinition = "text")
    private String grokipediaUrl;

    /** Grokipedia pages that were checked and rejected. Lookups skip these URLs. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "invalid_grokipedia_urls", columnDefinition = "jsonb")
    private List<RejectedUrl> invalidGrokipediaUrls;

    /**
     * Space-separated list of URLs where free online text can be found.
     * Using @Lob with explicit LONGVARCHAR type for PostgreSQL compatibility.
     * This avoids Hibernate 6's default OID handling for LOBs in PostgreSQL.
     */
    @Lob
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.LONGVARCHAR)
    private String freeTextUrl;

    /** Free-text URLs that were checked and rejected. Text providers skip these. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "invalid_free_text_urls", columnDefinition = "jsonb")
    private List<RejectedUrl> invalidFreeTextUrls;

    /** LibriVox URLs that were checked and rejected. Audio lookup skips these. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "invalid_free_audio_urls", columnDefinition = "jsonb")
    private List<RejectedUrl> invalidFreeAudioUrls;

    /**
     * Catalog records that were checked and found wrong. The next lookup for
     * that library skips the record id.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "invalid_holdings", columnDefinition = "jsonb")
    private List<RejectedHolding> invalidHoldings;

    /**
     * When a free-text search last finished for this book.
     * Null means never searched. A miss or a per-book error still stamps this
     * so the next oldest-first pass does not start over on the same book.
     */
    private LocalDateTime freeTextLastSearched;

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

    /** Last YDL catalog-lookup error; max 255 characters. */
    @Column(length = 255)
    private String ydlLookupError;

    private Boolean emuAudioAvailable;
    private Boolean emuPaperAvailable;
    private Boolean emuEbookAvailable;

    private LocalDateTime emuLastChecked;

    /** Last EMU catalog-lookup error; max 255 characters. */
    @Column(length = 255)
    private String emuLookupError;

    private Boolean aclaAudioAvailable;
    private Boolean aclaPaperAvailable;
    private Boolean aclaEbookAvailable;

    private LocalDateTime aclaLastChecked;

    /** Last ACLA catalog-lookup error; max 255 characters. */
    @Column(length = 255)
    private String aclaLookupError;

    private Boolean wccAudioAvailable;
    private Boolean wccPaperAvailable;
    private Boolean wccEbookAvailable;

    private LocalDateTime wccLastChecked;

    /** Last WCC catalog-lookup error; max 255 characters. */
    @Column(length = 255)
    private String wccLookupError;

    private Boolean msuAudioAvailable;
    private Boolean msuPaperAvailable;
    private Boolean msuEbookAvailable;

    private LocalDateTime msuLastChecked;

    /** Last MSU catalog-lookup error; max 255 characters. */
    @Column(length = 255)
    private String msuLookupError;


    @Convert(converter = ReadingDifficultyConverter.class)
    @Column(length = 20)
    private ReadingDifficulty readingDifficulty = ReadingDifficulty.UNSET;

    /**
     * Physical binding of this catalog copy. Legacy rows with a null column
     * are treated as {@link BookCoverType#UNKNOWN}.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BookCoverType binding = BookCoverType.UNKNOWN;

    /** Purchase priority, from 0 (already own enough) through 10 (first priority). */
    @Min(0)
    @Max(10)
    private Integer desireToPurchase;

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

    /**
     * Titles to search: the primary title first, then each distinct alternate.
     * A trailing {@code ", c. N"} suffix is removed. A case-insensitive repeat
     * is skipped. Never includes an author-only lookup.
     */
    public static List<String> titlesToSearch(Book book) {
        List<String> titles = new ArrayList<>();
        if (book.getTitle() != null) {
            String primary = stripCopySuffix(book.getTitle());
            if (primary != null && !primary.isBlank()) {
                titles.add(primary);
            }
        }
        if (book.getAlternateTitles() != null) {
            for (String alternate : book.getAlternateTitles()) {
                if (alternate == null) {
                    continue;
                }
                String alt = stripCopySuffix(alternate);
                if (alt == null || alt.isBlank() || containsIgnoreCase(titles, alt)) {
                    continue;
                }
                titles.add(alt);
            }
        }
        return titles;
    }

    private static boolean containsIgnoreCase(List<String> titles, String candidate) {
        for (String title : titles) {
            if (title != null && title.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
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
