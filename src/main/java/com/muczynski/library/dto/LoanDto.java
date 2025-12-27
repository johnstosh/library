/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LoanDto {
    private Long id;

    @NotNull(message = "Book ID is required")
    private Long bookId;

    private String bookTitle;

    @NotNull(message = "User ID is required")
    private Long userId;

    private String userName;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private LocalDateTime lastModified;
}
