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
    private RoleRepository roleRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PhotoRepository photoRepository;

    private Library testLibrary;
    private Author testAuthor;
    private Book testBook;
    private User testUser;
    private Role testRole;

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

        // Create test role
        testRole = new Role();
        testRole.setName("LIBRARIAN");
        testRole = roleRepository.save(testRole);

        // Create test user with roles
        testUser = new User();
        testUser.setUserIdentifier("test-user-id");
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$hashedpassword");
        Set<Role> roles = new HashSet<>();
        roles.add(testRole);
        testUser.setRoles(roles);
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportJson_WithCompleteData() throws Exception {
        // This test verifies that export works with books having author and library associations
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.libraries", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.authors", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.books", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.users", hasSize(greaterThanOrEqualTo(1))))
                // Verify book has author and library data loaded (no lazy loading exception)
                .andExpect(jsonPath("$.books[?(@.title=='Test Book')].author.name", hasItem("Test Author")))
                .andExpect(jsonPath("$.books[?(@.title=='Test Book')].libraryName", hasItem("Test Library")));
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

        // This test verifies that loans with nested book/author/library/user associations work
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loans", hasSize(greaterThanOrEqualTo(1))))
                // Verify loan has book data with nested author
                .andExpect(jsonPath("$.loans[0].book.title", notNullValue()))
                .andExpect(jsonPath("$.loans[0].book.author.name", notNullValue()))
                .andExpect(jsonPath("$.loans[0].book.libraryName", notNullValue()))
                // Verify loan has user data with roles
                .andExpect(jsonPath("$.loans[0].user.username", notNullValue()))
                .andExpect(jsonPath("$.loans[0].user.roles", notNullValue()));
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

        // This test verifies that photos with book and author associations work
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos", hasSize(greaterThanOrEqualTo(2))))
                // Verify photo has book reference loaded
                .andExpect(jsonPath("$.photos[?(@.caption=='Test book cover')].bookTitle", hasItem("Test Book")))
                .andExpect(jsonPath("$.photos[?(@.caption=='Test book cover')].bookAuthorName", hasItem("Test Author")))
                // Verify author photo has author reference
                .andExpect(jsonPath("$.photos[?(@.caption=='Author portrait')].authorName", hasItem("Test Author")));
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
            Set<Role> roles = new HashSet<>();
            roles.add(testRole);
            user.setRoles(roles);
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

        // This test verifies that books without authors are exported correctly
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[?(@.title=='Anonymous Book')]", hasSize(1)))
                .andExpect(jsonPath("$.books[?(@.title=='Anonymous Book')].author", hasItem(nullValue())));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportJson_UserWithMultipleRoles() throws Exception {
        // Create additional role
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole = roleRepository.save(adminRole);

        // Create user with multiple roles
        User multiRoleUser = new User();
        multiRoleUser.setUserIdentifier("multi-role-user");
        multiRoleUser.setUsername("multiroleuser");
        multiRoleUser.setPassword("$2a$10$hashedpw");
        Set<Role> roles = new HashSet<>();
        roles.add(testRole);
        roles.add(adminRole);
        multiRoleUser.setRoles(roles);
        userRepository.save(multiRoleUser);

        // Create a loan for this user
        Loan loan = new Loan();
        loan.setBook(testBook);
        loan.setUser(multiRoleUser);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusWeeks(2));
        loanRepository.save(loan);

        // This test verifies that users with multiple roles are exported correctly
        mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loans[?(@.user.username=='multiroleuser')].user.roles", hasItem(hasSize(2))));
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
}
