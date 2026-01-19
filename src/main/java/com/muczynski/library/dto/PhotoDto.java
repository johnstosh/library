/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhotoDto {
    private Long id;
    private String contentType;
    private String caption;
    private Long bookId;
    private Long authorId;
    private Long loanId;
    private String imageChecksum;
    private LocalDateTime dateTaken;
}
