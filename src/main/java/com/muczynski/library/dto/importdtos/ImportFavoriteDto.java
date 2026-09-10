/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ImportFavoriteDto {
    private String username;
    private String listName;
    /** Book favorite: title plus author name, matching loan import keys. */
    private String bookTitle;
    private String bookAuthorName;
    /** Author favorite. */
    private String authorName;
}
