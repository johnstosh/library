/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

@Data
public class PhotoImportResultDto {
    private String message;
    private Integer imported;
    private Integer failed;
}
