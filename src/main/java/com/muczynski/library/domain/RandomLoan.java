/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Component
public class RandomLoan {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Random RANDOM = new Random();

    public Loan create() {
        Loan loan = new Loan();
        List<Book> books = bookRepository.findAll();
        List<User> users = userRepository.findAll();

        loan.setBook(books.get(RANDOM.nextInt(books.size())));
        loan.setUser(users.get(RANDOM.nextInt(users.size())));
        loan.setLoanDate(LocalDate.of(2099, 1, 1));
        loan.setDueDate(LocalDate.of(2099, 1, 15));

        return loan;
    }
}