/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ImportLoanDto {
    private ImportBookDto book;
    private ImportUserDto user;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
}
