/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One catalog record that a holdings lookup used. The edit page can reject
 * {@link #recordKey} without searching again.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogMatchRef {
    private String recordKey;
    private String url;
}
