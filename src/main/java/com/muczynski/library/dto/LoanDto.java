// (c) Copyright 2025 by Muczynski
package com.muczynski.library.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LoanDto {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private Long userId;
    private String userName;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
}
