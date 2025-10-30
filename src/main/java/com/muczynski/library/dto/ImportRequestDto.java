package com.muczynski.library.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImportRequestDto {
    private List<LibraryDto> libraries;
    private List<AuthorImportDto> authors;
    private List<UserImportDto> users;
    private List<BookImportDto> books;
    private List<LoanImportDto> loans;
}
