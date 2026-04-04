/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a label and its associated book count.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabelCountDto {
    private String label;
    private long count;
}
