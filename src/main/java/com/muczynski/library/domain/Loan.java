// (c) Copyright 2025 by Muczynski
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
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
}
