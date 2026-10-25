/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * AbeBooks bookseller seen on a price lookup, plus the shipping terms we could read.
 */
@Entity
@Table(
        name = "abe_seller",
        uniqueConstraints = @UniqueConstraint(name = "uk_abe_seller_vendor", columnNames = "vendor_id")
)
@Getter
@Setter
public class AbeSeller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(length = 200)
    private String name;

    @Column(length = 120)
    private String city;

    @Column(length = 80)
    private String region;

    @Column(length = 80)
    private String country;

    private Integer starRating;

    @Column(length = 500)
    private String storefrontUrl;

    /** Listing id used to open the ShipRates page. */
    private Long sampleListingId;

    @Column(precision = 10, scale = 2)
    private BigDecimal sampleStandardShipping;

    @Column(precision = 10, scale = 2)
    private BigDecimal samplePriorityShipping;

    @Lob
    @Column(columnDefinition = "text")
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String shippingTerms;

    @Column(precision = 10, scale = 2)
    private BigDecimal additionalShippingDollars;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AdditionalShippingSource additionalShippingSource = AdditionalShippingSource.UNKNOWN;

    private LocalDateTime lookedUpAt;

    /** When ShipRates was last fetched. Null means the terms have not been read. */
    private LocalDateTime ratesLookedUpAt;

    private LocalDateTime lastModified;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (additionalShippingSource == null) {
            additionalShippingSource = AdditionalShippingSource.UNKNOWN;
        }
        lastModified = LocalDateTime.now(ZoneOffset.UTC);
    }

    public String shipRatesUrl() {
        if (vendorId == null || sampleListingId == null) {
            return null;
        }
        return "https://www.abebooks.com/servlet/ShipRates?vid=" + vendorId + "&bi=" + sampleListingId;
    }
}
