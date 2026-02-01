/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.*;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.*;
import com.muczynski.library.photostorage.client.GooglePhotosLibraryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for data integrity: findOrCreate methods and duplicate prevention.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataIntegrityTest {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private BranchService branchService;

    @Autowired
    private BookService bookService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @MockitoBean
    private GooglePhotosService googlePhotosService;

    @MockitoBean
    private GooglePhotosLibraryClient googlePhotosLibraryClient;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        photoRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- Author findOrCreate ---

    @Test
    void testFindOrCreateAuthor_createsNew() {
        Author author = authorService.findOrCreateAuthor("New Author");
        assertNotNull(author.getId());
        assertEquals("New Author", author.getName());
    }

    @Test
    void testFindOrCreateAuthor_returnsExisting() {
        Author first = authorService.findOrCreateAuthor("Existing Author");
        Author second = authorService.findOrCreateAuthor("Existing Author");
        assertEquals(first.getId(), second.getId());
    }

    // --- Branch findOrCreate ---

    @Test
    void testFindOrCreateBranch_createsNew() {
        Library branch = branchService.findOrCreateBranch("New Branch", "Test System");
        assertNotNull(branch.getId());
        assertEquals("New Branch", branch.getBranchName());
    }

    @Test
    void testFindOrCreateBranch_returnsExisting() {
        Library first = branchService.findOrCreateBranch("Test Branch", "System A");
        Library second = branchService.findOrCreateBranch("Test Branch", "System B");
        assertEquals(first.getId(), second.getId());
    }

    // --- Book findOrCreate ---

    @Test
    void testFindOrCreateBook_createsNew() {
        Author author = authorService.findOrCreateAuthor("Test Author");
        Library branch = branchService.findOrCreateBranch("Test Branch", "Test System");
        Book book = bookService.findOrCreateBook("New Book", author.getName(), branch);
        assertNotNull(book.getId());
        assertEquals("New Book", book.getTitle());
    }

    @Test
    void testFindOrCreateBook_returnsExisting() {
        Author author = authorService.findOrCreateAuthor("Test Author");
        Library branch = branchService.findOrCreateBranch("Test Branch", "Test System");
        Book first = bookService.findOrCreateBook("Same Book", author.getName(), branch);
        Book second = bookService.findOrCreateBook("Same Book", author.getName(), branch);
        assertEquals(first.getId(), second.getId());
    }

    // --- Loan findOrCreate ---

    @Test
    void testFindOrCreateLoan_createsNew() {
        Author author = authorService.findOrCreateAuthor("Loan Author");
        Library branch = branchService.findOrCreateBranch("Loan Branch", "System");
        Book book = bookService.findOrCreateBook("Loan Book", author.getName(), branch);

        Authority authority = userService.findOrCreateAuthority("USER");
        User user = new User();
        user.setUsername("loanuser");
        user.setPassword("{bcrypt}$2a$10$...");
        user.setUserIdentifier("loan-user-id");
        user.setAuthorities(java.util.Set.of(authority));
        user = userRepository.save(user);

        Loan loan = loanService.findOrCreateLoan(book.getId(), user.getId(), LocalDate.of(2025, 6, 15));
        assertNotNull(loan.getId());
        assertEquals(LocalDate.of(2025, 6, 15), loan.getLoanDate());
    }

    @Test
    void testFindOrCreateLoan_returnsExisting() {
        Author author = authorService.findOrCreateAuthor("Loan Author 2");
        Library branch = branchService.findOrCreateBranch("Loan Branch 2", "System");
        Book book = bookService.findOrCreateBook("Loan Book 2", author.getName(), branch);

        Authority authority = userService.findOrCreateAuthority("USER");
        User user = new User();
        user.setUsername("loanuser2");
        user.setPassword("{bcrypt}$2a$10$...");
        user.setUserIdentifier("loan-user-id-2");
        user.setAuthorities(java.util.Set.of(authority));
        user = userRepository.save(user);

        Loan first = loanService.findOrCreateLoan(book.getId(), user.getId(), LocalDate.of(2025, 7, 1));
        Loan second = loanService.findOrCreateLoan(book.getId(), user.getId(), LocalDate.of(2025, 7, 1));
        assertEquals(first.getId(), second.getId());
    }

    // --- Authority findOrCreate ---

    @Test
    void testFindOrCreateAuthority_createsNew() {
        Authority authority = userService.findOrCreateAuthority("TESTROLE");
        assertNotNull(authority.getId());
        assertEquals("TESTROLE", authority.getName());
    }

    @Test
    void testFindOrCreateAuthority_returnsExisting() {
        Authority first = userService.findOrCreateAuthority("DUPROLE");
        Authority second = userService.findOrCreateAuthority("DUPROLE");
        assertEquals(first.getId(), second.getId());
    }

    // --- Constraint violation throws DataIntegrityViolationException ---

    @Test
    void testUniqueConstraintViolation_throwsException() {
        // Create an author directly
        Author author = new Author();
        author.setName("Constraint Author");
        authorRepository.save(author);

        // Try to create another with the same name
        Author duplicate = new Author();
        duplicate.setName("Constraint Author");

        // Should throw DataIntegrityViolationException due to unique constraint
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            authorRepository.save(duplicate);
        });
    }
}
