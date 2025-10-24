package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.RandomAuthor;
import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.RandomBook;
import com.muczynski.library.domain.RandomLoan;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LibraryRepository;
import com.muczynski.library.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Service
@Transactional
public class TestDataService {

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
            library.setName("Test Library");
            library.setHostname("test.local");
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
        bookRepository.deleteByPublisher("test-data");
        authorRepository.deleteByReligiousAffiliation("test-data");
        loanRepository.deleteByLoanDate(java.time.LocalDate.of(2099, 1, 1));
    }

    public void totalPurge() {
        try (Connection connection = dataSource.getConnection()) {
            String dbProductName = connection.getMetaData().getDatabaseProductName();
            if ("PostgreSQL".equals(dbProductName)) {
                purgePostgres();
            } else {
                purgeH2();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to obtain database product name", e);
        }
    }

    private void purgePostgres() {
        try {
            jdbcTemplate.execute("SET session_replication_role = 'replica'");
            List<String> tableNames = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
            );
            for (String tableName : tableNames) {
                jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName + " CASCADE");
            }
        } finally {
            jdbcTemplate.execute("SET session_replication_role = 'origin'");
        }
    }

    private void purgeH2() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        List<String> tableNames = jdbcTemplate.queryForList("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'", String.class);
        tableNames.forEach(tableName -> jdbcTemplate.execute("DROP TABLE " + tableName));
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
