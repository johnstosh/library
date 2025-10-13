package com.muczynski.library.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LoanDto {
    private Long id;
    private Long bookId;
    private Long userId;
    private LocalDate loanDate;
    private LocalDate returnDate;
}