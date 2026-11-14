/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * A URL that was checked and rejected. Lookups skip it instead of saving it again.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectedUrl {
    private String url;
    private String reason;
    private LocalDate date;
    /** Username of the person who rejected it. */
    private String by;
}
