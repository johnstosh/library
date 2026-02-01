/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    indexes = {
        @Index(name = "idx_loan_book_return", columnList = "book_id, return_date"),
        @Index(name = "idx_loan_user_return", columnList = "user_id, return_date"),
        @Index(name = "idx_loan_due_date", columnList = "due_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_loan_book_user_date", columnNames = {"book_id", "user_id", "loan_date"})
    }
)
@Getter
@Setter
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Book book;

    @ManyToOne
    private User user;

    private LocalDate loanDate = LocalDate.now();

    private LocalDate dueDate = LocalDate.now().plusWeeks(2);

    private LocalDate returnDate;

    private LocalDateTime lastModified;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }
}
