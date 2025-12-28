/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseDto {
    private List<BookDto> books;
    private List<AuthorDto> authors;
    private PageInfoDto bookPage;
    private PageInfoDto authorPage;
}
