/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthorSummaryDto {
    private Long id;
    private LocalDateTime lastModified;
}
