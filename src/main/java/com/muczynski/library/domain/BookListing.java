/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * One AbeBooks offer captured during price lookup. The Prices page still keeps
 * only the cheapest row per cover in {@link BookPrice}.
 */
@Entity
@Table(
        name = "book_listing",
        indexes = {
                @Index(name = "idx_book_listing_book", columnList = "book_id"),
                @Index(name = "idx_book_listing_seller", columnList = "seller_id")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_book_listing_abe", columnNames = "abe_listing_id")
)
@Getter
@Setter
public class BookListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Book book;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private AbeSeller seller;

    @Column(name = "abe_listing_id", nullable = false)
    private Long abeListingId;

    @Column(length = 500)
    private String listingTitle;

    @Column(length = 2000)
    private String detailsUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal priceDollars;

    @Column(precision = 10, scale = 2)
    private BigDecimal shippingDollars;

    @Column(precision = 10, scale = 2)
    private BigDecimal priorityShippingDollars;

    @Column(length = 80)
    private String condition;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BookCoverType binding;

    @Column(length = 20)
    private String isbn13;

    private Integer quantity;

    private LocalDateTime lookedUpAt;

    private LocalDateTime lastModified;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now(ZoneOffset.UTC);
    }
}
