package com.muczynski.library.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LoanImportDto {
    private String bookTitle;
    private String authorName;
    private String userUsername;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
}
