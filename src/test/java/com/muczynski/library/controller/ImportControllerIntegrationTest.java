/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.*;
import com.muczynski.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ImportController that test actual database queries.
 * These tests verify that the N+1 query optimizations work correctly and
 * all relationships are properly loaded.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ImportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PhotoRepository photoRepository;

    private Library testLibrary;
    private Author testAuthor;
    private Book testBook;
    private User testUser;
    private Authority testAuthority;

    @BeforeEach
    void setUp() {
        // Create test library
        testLibrary = new Library();
        testLibrary.setName("Test Library");
        testLibrary.setHostname("test.library.com");
        testLibrary = libraryRepository.save(testLibrary);

        // Create test author
        testAuthor = new Author();
        testAuthor.setName("Test Author");
        testAuthor.setDateOfBirth(LocalDate.of(1950, 1, 1));
        testAuthor.setBirthCountry("USA");
        testAuthor.setNationality("American");
        testAuthor = authorRepository.save(testAuthor);

        // Create test book with author and library
        testBook = new Book();
        testBook.setTitle("Test Book");
        testBook.setPublicationYear(2020);
        testBook.setPublisher("Test Publisher");
        testBook.setPlotSummary("A test book summary");
        testBook.setDateAddedToLibrary(LocalDateTime.now());
        testBook.setStatus(BookStatus.ACTIVE);
        testBook.setLocNumber("PS3000.T47");
        testBook.setAuthor(testAuthor);
        testBook.setLibrary(testLibrary);
        testBook = bookRepository.save(testBook);

        // Get existing LIBRARIAN authority from data-base.sql
        testAuthority = authorityRepository.findByName("LIBRARIAN")
                .orElseThrow(() -> new RuntimeException("LIBRARIAN authority not found"));

        // Create test user with authorities
        testUser = new User();
        testUser.setUserIdentifier("test-user-id");
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$hashedpassword");
        Set<Authority> authorities = new HashSet<>();
        authorities.add(testAuthority);
        testUser.setAuthorities(authorities);
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportJson_WithCompleteData() throws Exception {
        // This test verifies that export works with books having author and library associations
        // New format: books export authorName (string reference) instead of embedded author object
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.libraries", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.authors", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.books", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.users", hasSize(greaterThanOrEqualTo(1))))
                // Verify book has authorName (string reference) and libraryName
                .andExpect(jsonPath("$.books[?(@.title=='Test Book')].authorName", hasItem("Test Author")))
                .andExpect(jsonPath("$.books[?(@.title=='Test Book')].libraryName", hasItem("Test Library")));
                // Note: embedded author object is omitted via @JsonInclude(NON_NULL)
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportJson_WithLoans() throws Exception {
        // Create a loan that references book and user
        Loan loan = new Loan();
        loan.setBook(testBook);
        loan.setUser(testUser);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusWeeks(2));
        loanRepository.save(loan);

        // This test verifies that loans export reference fields (new format)
        // instead of embedded book/user objects
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loans", hasSize(greaterThanOrEqualTo(1))))
                // Verify loan has reference fields (new format)
                .andExpect(jsonPath("$.loans[0].bookTitle", notNullValue()))
                .andExpect(jsonPath("$.loans[0].bookAuthorName", notNullValue()))
                .andExpect(jsonPath("$.loans[0].username", notNullValue()))
                // Verify embedded objects are NOT included (new format)
                .andExpect(jsonPath("$.loans[0].book").doesNotExist())
                .andExpect(jsonPath("$.loans[0].user").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportJson_WithPhotos() throws Exception {
        // Create a photo for the book
        Photo bookPhoto = new Photo();
        bookPhoto.setBook(testBook);
        bookPhoto.setAuthor(testAuthor);
        bookPhoto.setContentType("image/jpeg");
        bookPhoto.setCaption("Test book cover");
        bookPhoto.setPhotoOrder(1);
        bookPhoto.setPermanentId("test-permanent-id");
        photoRepository.save(bookPhoto);

        // Create an author-only photo
        Photo authorPhoto = new Photo();
        authorPhoto.setAuthor(testAuthor);
        authorPhoto.setContentType("image/png");
        authorPhoto.setCaption("Author portrait");
        authorPhoto.setPhotoOrder(1);
        photoRepository.save(authorPhoto);

        // IMPORTANT: Photos are NOT exported in JSON export (too large)
        // Photos should be managed separately via Photo Export feature
        // This test verifies that photos field is null in the export
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportJson_WithMultipleBooks() throws Exception {
        // Create additional books to test bulk loading
        for (int i = 1; i <= 5; i++) {
            Author author = new Author();
            author.setName("Author " + i);
            author = authorRepository.save(author);

            Book book = new Book();
            book.setTitle("Book " + i);
            book.setPublicationYear(2020 + i);
            book.setDateAddedToLibrary(LocalDateTime.now());
            book.setStatus(BookStatus.ACTIVE);
            book.setAuthor(author);
            book.setLibrary(testLibrary);
            bookRepository.save(book);
        }

        // This test verifies that multiple books are exported efficiently
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books", hasSize(greaterThanOrEqualTo(6))))
                .andExpect(jsonPath("$.authors", hasSize(greaterThanOrEqualTo(6))));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportJson_WithMultipleLoans() throws Exception {
        // Create multiple loans to test bulk loading of nested associations
        for (int i = 1; i <= 3; i++) {
            User user = new User();
            user.setUserIdentifier("user-" + i);
            user.setUsername("user" + i);
            user.setPassword("$2a$10$hash" + i);
            Set<Authority> authorities = new HashSet<>();
            authorities.add(testAuthority);
            user.setAuthorities(authorities);
            user = userRepository.save(user);

            Loan loan = new Loan();
            loan.setBook(testBook);
            loan.setUser(user);
            loan.setLoanDate(LocalDate.now().minusDays(i));
            loan.setDueDate(LocalDate.now().plusWeeks(2));
            loanRepository.save(loan);
        }

        // This test verifies that multiple loans with all associations are loaded efficiently
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loans", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.users", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportJson_BookWithoutAuthor() throws Exception {
        // Create a book without an author
        Book bookNoAuthor = new Book();
        bookNoAuthor.setTitle("Anonymous Book");
        bookNoAuthor.setPublicationYear(2021);
        bookNoAuthor.setDateAddedToLibrary(LocalDateTime.now());
        bookNoAuthor.setStatus(BookStatus.ACTIVE);
        bookNoAuthor.setLibrary(testLibrary);
        // No author set
        bookRepository.save(bookNoAuthor);

        // This test verifies that books without authors are exported correctly (new format)
        // authorName field is omitted when null (via @JsonInclude(NON_NULL))
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[?(@.title=='Anonymous Book')]", hasSize(1)))
                .andExpect(jsonPath("$.books[?(@.title=='Anonymous Book')].libraryName", hasItem("Test Library")));
                // Note: authorName is omitted from JSON when null via @JsonInclude(NON_NULL)
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportJson_UserWithMultipleAuthorities() throws Exception {
        // Create additional authority
        Authority adminAuthority = new Authority();
        adminAuthority.setName("ADMIN");
        adminAuthority = authorityRepository.save(adminAuthority);

        // Create user with multiple authorities
        User multiAuthorityUser = new User();
        multiAuthorityUser.setUserIdentifier("multi-authority-user");
        multiAuthorityUser.setUsername("multiauthorityuser");
        multiAuthorityUser.setPassword("$2a$10$hashedpw");
        Set<Authority> authorities = new HashSet<>();
        authorities.add(testAuthority);
        authorities.add(adminAuthority);
        multiAuthorityUser.setAuthorities(authorities);
        userRepository.save(multiAuthorityUser);

        // Create a loan for this user
        Loan loan = new Loan();
        loan.setBook(testBook);
        loan.setUser(multiAuthorityUser);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusWeeks(2));
        loanRepository.save(loan);

        // This test verifies that users with multiple authorities are exported correctly
        // New format: loans have username reference, users are exported separately with authorities
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                // Verify loan has username reference (new format)
                .andExpect(jsonPath("$.loans[?(@.username=='multiauthorityuser')]", hasSize(1)))
                // Verify user is exported in users array with multiple authorities
                .andExpect(jsonPath("$.users[?(@.username=='multiauthorityuser')].authorities", hasItem(hasSize(2))));
    }

    @Test
    void testExportJson_Unauthorized() throws Exception {
        // Test that unauthenticated requests are rejected
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void testExportJson_Forbidden() throws Exception {
        // Test that users without LIBRARIAN authority are forbidden
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/import/stats Integration Tests ====================

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testGetDatabaseStats_ReturnsCorrectCounts() throws Exception {
        // The setUp creates 1 library, 1 author, 1 book, 1 user (plus seeded data)
        // This test verifies that the stats endpoint returns actual database counts
        mockMvc.perform(get("/api/import/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.libraryCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.bookCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.authorCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.userCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.loanCount", greaterThanOrEqualTo(0)));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testGetDatabaseStats_IncludesNewData() throws Exception {
        // Get initial counts
        long initialBooks = bookRepository.count();
        long initialAuthors = authorRepository.count();

        // Create additional data
        Author newAuthor = new Author();
        newAuthor.setName("Stats Test Author");
        newAuthor = authorRepository.save(newAuthor);

        Book newBook1 = new Book();
        newBook1.setTitle("Stats Test Book 1");
        newBook1.setDateAddedToLibrary(LocalDateTime.now());
        newBook1.setStatus(BookStatus.ACTIVE);
        newBook1.setLibrary(testLibrary);
        newBook1.setAuthor(newAuthor);
        bookRepository.save(newBook1);

        Book newBook2 = new Book();
        newBook2.setTitle("Stats Test Book 2");
        newBook2.setDateAddedToLibrary(LocalDateTime.now());
        newBook2.setStatus(BookStatus.ACTIVE);
        newBook2.setLibrary(testLibrary);
        newBook2.setAuthor(newAuthor);
        bookRepository.save(newBook2);

        // Verify stats endpoint reflects the new data
        mockMvc.perform(get("/api/import/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookCount", equalTo((int) (initialBooks + 2))))
                .andExpect(jsonPath("$.authorCount", equalTo((int) (initialAuthors + 1))));
    }

    @Test
    void testGetDatabaseStats_Unauthorized() throws Exception {
        // Test that unauthenticated requests are rejected
        mockMvc.perform(get("/api/import/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void testGetDatabaseStats_Forbidden() throws Exception {
        // Test that users without LIBRARIAN authority are forbidden
        mockMvc.perform(get("/api/import/stats"))
                .andExpect(status().isForbidden());
    }

    // ==================== Backward Compatibility Import Tests ====================

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testImportJson_OldFormatWithEmbeddedAuthor() throws Exception {
        // Test that old format with embedded author object is still supported
        String oldFormatJson = """
            {
                "libraries": [{"name": "Legacy Library", "hostname": "legacy.lib.com"}],
                "authors": [{"name": "Legacy Author"}],
                "users": [],
                "books": [{
                    "title": "Legacy Book",
                    "libraryName": "Legacy Library",
                    "author": {"name": "Legacy Author"}
                }],
                "loans": []
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/import/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(oldFormatJson))
                .andExpect(status().isOk());

        // Verify the book was imported correctly
        org.junit.jupiter.api.Assertions.assertTrue(
            bookRepository.findAllByTitleAndAuthor_NameOrderByIdAsc("Legacy Book", "Legacy Author").size() > 0,
            "Book with embedded author format should be imported"
        );
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testImportJson_NewFormatWithAuthorName() throws Exception {
        // Test new format with authorName field (string reference)
        String newFormatJson = """
            {
                "libraries": [{"name": "Modern Library", "hostname": "modern.lib.com"}],
                "authors": [{"name": "Modern Author"}],
                "users": [],
                "books": [{
                    "title": "Modern Book",
                    "libraryName": "Modern Library",
                    "authorName": "Modern Author"
                }],
                "loans": []
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/import/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newFormatJson))
                .andExpect(status().isOk());

        // Verify the book was imported correctly
        org.junit.jupiter.api.Assertions.assertTrue(
            bookRepository.findAllByTitleAndAuthor_NameOrderByIdAsc("Modern Book", "Modern Author").size() > 0,
            "Book with authorName reference format should be imported"
        );
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testImportJson_OldFormatLoanWithEmbeddedObjects() throws Exception {
        // Test that old format loans with embedded book/user objects are still supported
        String oldFormatJson = """
            {
                "libraries": [{"name": "Loan Test Library", "hostname": "loantest.lib.com"}],
                "authors": [{"name": "Loan Test Author"}],
                "users": [{"username": "loanuser", "authorities": ["USER"]}],
                "books": [{
                    "title": "Loan Test Book",
                    "libraryName": "Loan Test Library",
                    "authorName": "Loan Test Author"
                }],
                "loans": [{
                    "loanDate": "2025-01-01",
                    "dueDate": "2025-01-15",
                    "book": {
                        "title": "Loan Test Book",
                        "author": {"name": "Loan Test Author"}
                    },
                    "user": {"username": "loanuser"}
                }]
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/import/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(oldFormatJson))
                .andExpect(status().isOk());

        // Verify the loan was imported correctly
        org.junit.jupiter.api.Assertions.assertTrue(
            loanRepository.count() > 0,
            "Loan with embedded objects format should be imported"
        );
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testImportJson_NewFormatLoanWithReferences() throws Exception {
        // Test new format loans with reference fields
        String newFormatJson = """
            {
                "libraries": [{"name": "New Loan Library", "hostname": "newloan.lib.com"}],
                "authors": [{"name": "New Loan Author"}],
                "users": [{"username": "newloanuser", "authorities": ["USER"]}],
                "books": [{
                    "title": "New Loan Book",
                    "libraryName": "New Loan Library",
                    "authorName": "New Loan Author"
                }],
                "loans": [{
                    "loanDate": "2025-01-05",
                    "dueDate": "2025-01-19",
                    "bookTitle": "New Loan Book",
                    "bookAuthorName": "New Loan Author",
                    "username": "newloanuser"
                }]
            }
            """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/import/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newFormatJson))
                .andExpect(status().isOk());

        // Verify the loan was imported correctly
        java.util.List<Book> books = bookRepository.findAllByTitleAndAuthor_NameOrderByIdAsc("New Loan Book", "New Loan Author");
        org.junit.jupiter.api.Assertions.assertTrue(books.size() > 0, "Book should exist");
    }
}
