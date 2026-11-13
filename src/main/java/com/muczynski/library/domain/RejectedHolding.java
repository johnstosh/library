/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * A catalog record that was checked and found to be the wrong book, the wrong
 * edition, or a record that does not hold the item. The next lookup for
 * {@link #library} skips {@link #recordKey}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectedHolding {
    private PublicLibrary library;
    /** Stable id from that catalog, such as a Primo record id or a VuFind id. */
    private String recordKey;
    /** Optional link a person can open. Not used for matching. */
    private String url;
    private String reason;
    private LocalDate date;
    /** Username of the person who rejected it. */
    private String by;
}
