/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

@Data
public class PhotoVerifyResultDto {
    private Long photoId;
    private String permanentId;
    private Boolean valid;
    private String message;
    private String filename;
    private String mimeType;
}
