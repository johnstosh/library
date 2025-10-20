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

        LocalDate today = LocalDate.now();
        LocalDate twoWeeksFromToday = today.plusWeeks(2);
        int randomDays = RANDOM.nextInt(15) - 7; // -7 to +7
        loan.setDueDate(twoWeeksFromToday.plusDays(randomDays));

        return loan;
    }
}