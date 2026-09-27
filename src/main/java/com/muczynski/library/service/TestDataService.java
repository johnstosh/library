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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TestDataService {

    private static final Logger logger = LoggerFactory.getLogger(TestDataService.class);

    /** Prefix used by RandomBook / RandomAuthor generators (e.g. "test-data - Penguin..."). */
    static final String TEST_DATA_PREFIX = "test-data";

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

    @Autowired
    private BookService bookService;

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
            book.setTitle(bookService.ensureUniqueTitle(book.getTitle(), null));
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
        // Delete test loans (generators use a fixed far-future loan date)
        loanRepository.deleteByLoanDate(java.time.LocalDate.of(2099, 1, 1));

        // Match generators: publisher / religiousAffiliation are "test-data - …", users "test-data-…"
        List<Book> testBooks = bookRepository.findAll().stream()
                .filter(book -> startsWithTestDataPrefix(book.getPublisher()))
                .collect(Collectors.toList());
        List<Long> testBookIds = testBooks.stream().map(Book::getId).collect(Collectors.toList());

        List<Author> testAuthors = authorRepository.findAll().stream()
                .filter(author -> startsWithTestDataPrefix(author.getReligiousAffiliation()))
                .collect(Collectors.toList());
        List<Long> testAuthorIds = testAuthors.stream().map(Author::getId).collect(Collectors.toList());

        List<User> testUsers = userRepository.findAll().stream()
                .filter(user -> user.getUsername() != null && user.getUsername().startsWith("test-data-"))
                .collect(Collectors.toList());
        List<Long> testUserIds = testUsers.stream().map(User::getId).collect(Collectors.toList());

        // Explicit child deletes in FK-safe order (do not rely on cascades alone).
        // favorites may reference book, author, and/or user; book_price and book_tags reference book.
        deleteWhereIdIn("favorites", "book_id", testBookIds);
        deleteWhereIdIn("favorites", "author_id", testAuthorIds);
        deleteWhereIdIn("favorites", "user_id", testUserIds);
        deleteWhereIdIn("book_price", "book_id", testBookIds);
        deleteWhereIdIn("book_tags", "book_id", testBookIds);

        // Photos for test books and authors (book delete may cascade, but be explicit)
        for (Long bookId : testBookIds) {
            List<Photo> bookPhotos = photoRepository.findByBookIdOrderByPhotoOrder(bookId);
            if (!bookPhotos.isEmpty()) {
                photoRepository.deleteAll(bookPhotos);
            }
        }
        for (Author testAuthor : testAuthors) {
            List<Photo> authorPhotos = photoRepository.findByAuthorId(testAuthor.getId());
            if (!authorPhotos.isEmpty()) {
                photoRepository.deleteAll(authorPhotos);
            }
        }

        if (!testBooks.isEmpty()) {
            bookRepository.deleteAll(testBooks);
        }

        if (!testAuthors.isEmpty()) {
            authorRepository.deleteAll(testAuthors);
        }

        if (!testUsers.isEmpty()) {
            userRepository.deleteAll(testUsers);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void totalPurge() {
        // Child / join tables first — DROP TABLE book CASCADE does NOT drop sibling tables
        // that merely hold FKs to book (favorites, book_price, book_tags).
        jdbcTemplate.execute("DROP TABLE IF EXISTS favorites CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS book_price CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS book_tags CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS photo_upload_session CASCADE");
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

    private static boolean startsWithTestDataPrefix(String value) {
        return value != null && value.startsWith(TEST_DATA_PREFIX);
    }

    private void deleteWhereIdIn(String table, String column, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "DELETE FROM " + table + " WHERE " + column + " IN (" + placeholders + ")";
        jdbcTemplate.update(sql, ids.toArray());
        logger.debug("Deleted from {} where {} in {} id(s)", table, column, ids.size());
    }
}
