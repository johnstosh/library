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
 * Latest AbeBooks listing found for a catalog book and cover type
 * (hardcover or softcover, good condition or better).
 */
@Entity
@Table(
    name = "book_price",
    indexes = {
        @Index(name = "idx_book_price_book", columnList = "book_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_book_price_book_cover", columnNames = {"book_id", "cover"})
    }
)
@Getter
@Setter
public class BookPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookCoverType cover;

    @Column(precision = 10, scale = 2)
    private BigDecimal priceDollars;

    @Column(precision = 10, scale = 2)
    private BigDecimal shippingDollars;

    @Column(length = 80)
    private String condition;

    private LocalDateTime lookedUpAt;

    @Column(length = 2000)
    private String detailsUrl;

    @Column(length = 500)
    private String lookupError;

    private LocalDateTime lastModified;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now(ZoneOffset.UTC);
    }
}
