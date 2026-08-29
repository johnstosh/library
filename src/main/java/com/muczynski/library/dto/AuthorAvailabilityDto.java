/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

/**
 * Per-author YDL/EMU holdings rolled up from that author's books.
 * Used by the Authors page filter chips. Not stored on Author; derived from Book flags.
 */
@Data
public class AuthorAvailabilityDto {
    private Long authorId;
    private Boolean hasYdlBook;
    private Boolean hasYdlEbook;
    private Boolean hasYdlAudio;
    private Boolean hasEmuBook;
    private Boolean hasEmuEbook;
    private Boolean hasEmuAudio;
}
