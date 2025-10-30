/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.domain.RandomAuthor;
import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.RandomBook;
import com.muczynski.library.domain.RandomLoan;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LibraryRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TestDataService {

    private static final Logger logger = LoggerFactory.getLogger(TestDataService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private RandomAuthor randomAuthor;

    @Autowired
    private RandomBook randomBook;

    @Autowired
    private RandomLoan randomLoan;

    @Autowired
    private LoanRepository loanRepository;

    public void generateTestData(int count) {
        if (libraryRepository.findAll().isEmpty()) {
            Library library = new Library();
            library.setName("St. Martin de Porres");
            library.setHostname("library.muczynskifamily.com");
            libraryRepository.save(library);
        }

        for (int i = 0; i < count; i++) {
            Author author = randomAuthor.create();
            authorRepository.save(author);

            Book book = randomBook.create(author);
            bookRepository.save(book);
        }
    }

    public void generateLoanData(int count) {
        for (int i = 0; i < count; i++) {
            Loan loan = randomLoan.create();
            loanRepository.save(loan);
        }
    }

    public void deleteTestData() {
        loanRepository.deleteByLoanDate(java.time.LocalDate.of(2099, 1, 1));
        bookRepository.deleteByPublisher("test-data");

        // Explicitly handle deletion of test authors, including their photos
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
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void totalPurge() {
        try (Connection connection = dataSource.getConnection()) {
            String dbProductName = connection.getMetaData().getDatabaseProductName();
            if ("PostgreSQL".equals(dbProductName)) {
                purgePostgres();
            } else {
                purgeH2();
            }
        } catch (SQLException e) {
            logger.debug("Failed to obtain database product name during total purge: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to obtain database product name", e);
        }
    }

    private void purgePostgres() {
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

    private void purgeH2() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        List<String> tableNames = jdbcTemplate.queryForList("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'", String.class);
        tableNames.forEach(tableName -> jdbcTemplate.execute("DROP TABLE " + tableName));
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
