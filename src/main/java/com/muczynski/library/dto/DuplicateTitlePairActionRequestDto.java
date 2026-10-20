/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

/**
 * The two books on one Duplicate Titles result row.
 * This is not a general author or book merge request.
 */
@Data
public class DuplicateTitlePairActionRequestDto {

    private Long bookAId;
    private Long bookBId;
}
