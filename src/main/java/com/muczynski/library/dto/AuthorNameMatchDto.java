/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

/**
 * One author for the book form. Biography and other large fields are omitted.
 */
@Data
public class AuthorNameMatchDto {
    private Long id;
    private String name;
}
