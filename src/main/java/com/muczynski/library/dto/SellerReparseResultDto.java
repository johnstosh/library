/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * How many stored seller terms were read again. This does not call AbeBooks.
 * A rate entered on Seller Info is left as the librarian typed it.
 */
@Data
@AllArgsConstructor
public class SellerReparseResultDto {
    private int examined;
    private int updated;
    private int unchanged;
    private int keptLibrarian;
}
