/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

/**
 * One active book for the checkout form. Plot and description are omitted.
 */
@Data
public class CheckoutMatchDto {
    private Long id;
    private String title;
    private String author;
    private String locNumber;
    private String status;
}
