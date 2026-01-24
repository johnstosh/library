/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ImportLoanDto {
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    // New format: references by natural keys
    private String bookTitle;
    private String bookAuthorName;
    private String username;

    // Old format (deprecated): embedded objects for backward compatibility during import
    // Export uses reference fields only; these are only read during import
    private ImportBookDto book;
    private ImportUserDto user;
}
