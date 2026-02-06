/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.domain.RandomAuthor;
import com.muczynski.library.domain.RandomBook;
import com.muczynski.library.domain.RandomLoan;
import com.muczynski.library.domain.RandomUser;
import com.muczynski.library.domain.User;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.BranchRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import com.muczynski.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TestDataService {

    private static final Logger logger = LoggerFactory.getLogger(TestDataService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private RandomAuthor randomAuthor;

    @Autowired
    private RandomBook randomBook;

    @Autowired
    private RandomLoan randomLoan;

    @Autowired
    private RandomUser randomUser;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    public void generateTestData(int count) {
        if (branchRepository.findAll().isEmpty()) {
            Library branch = new Library();
            branch.setBranchName("St. Martin de Porres");
            branch.setLibrarySystemName("Sacred Heart Library System");
            branchRepository.save(branch);
        }

        for (int i = 0; i < count; i++) {
            Author author = randomAuthor.create();
            author.setGrokipediaUrl("https://grokipedia.example.com/author/" + author.getId());
            authorRepository.save(author);

            Book book = randomBook.create(author);
            book.setGrokipediaUrl("https://grokipedia.example.com/book/" + book.getId());
            bookRepository.save(book);
        }
    }

    public void generateLoanData(int count) {
        for (int i = 0; i < count; i++) {
            Loan loan = randomLoan.create();
            loanRepository.save(loan);
        }
    }

    public void generateUserData(int count) {
        for (int i = 0; i < count; i++) {
            User user = randomUser.create();
            userRepository.save(user);
        }
    }

    public void deleteTestData() {
        // Delete test loans
        loanRepository.deleteByLoanDate(java.time.LocalDate.of(2099, 1, 1));

        // Delete test books
        bookRepository.deleteByPublisher("test-data");

        // Delete test authors and their photos
        List<Author> allAuthors = authorRepository.findAll();
        List<Author> testAuthors = allAuthors.stream()
                .filter(author -> "test-data".equals(author.getReligiousAffiliation()))
                .collect(Collectors.toList());

        for (Author testAuthor : testAuthors) {
            List<Photo> authorPhotos = photoRepository.findByAuthorId(testAuthor.getId());
            if (!authorPhotos.isEmpty()) {
                photoRepository.deleteAll(authorPhotos);
            }
        }

        if (!testAuthors.isEmpty()) {
            authorRepository.deleteAll(testAuthors);
        }

        // Delete test users (username starts with "test-data-")
        List<User> allUsers = userRepository.findAll();
        List<User> testUsers = allUsers.stream()
                .filter(user -> user.getUsername() != null && user.getUsername().startsWith("test-data-"))
                .collect(Collectors.toList());

        if (!testUsers.isEmpty()) {
            userRepository.deleteAll(testUsers);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void totalPurge() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS users_roles CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS loan CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS photo CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS book CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS users CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS role CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS author CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS library CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS applied CASCADE");
    }
}
