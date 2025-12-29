/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.*;
import com.muczynski.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LoanController.
 * Tests loan retrieval endpoints with real database and services.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoanControllerIntegrationTest {

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
    private ObjectMapper objectMapper;

    private Library testLibrary;
    private Author testAuthor;
    private Book testBook1;
    private Book testBook2;
    private Book testBook3;
    private User testUser;
    private User otherUser;
    private User librarianUser;
    private Loan activeLoan1;
    private Loan activeLoan2;
    private Loan returnedLoan;
    private Authority userAuthority;
    private Authority librarianAuthority;

    @BeforeEach
    void setUp() {
        // Create authorities
        userAuthority = new Authority();
        userAuthority.setName("USER");
        userAuthority = authorityRepository.save(userAuthority);

        librarianAuthority = new Authority();
        librarianAuthority.setName("LIBRARIAN");
        librarianAuthority = authorityRepository.save(librarianAuthority);

        // Create test library
        testLibrary = new Library();
        testLibrary.setName("Test Library");
        testLibrary.setHostname("test.library.com");
        testLibrary = libraryRepository.save(testLibrary);

        // Create test author
        testAuthor = new Author();
        testAuthor.setName("Test Author");
        testAuthor = authorRepository.save(testAuthor);

        // Create test books
        testBook1 = new Book();
        testBook1.setTitle("Book 1 - Active Loan");
        testBook1.setAuthor(testAuthor);
        testBook1.setLibrary(testLibrary);
        testBook1.setStatus(BookStatus.ACTIVE);
        testBook1 = bookRepository.save(testBook1);

        testBook2 = new Book();
        testBook2.setTitle("Book 2 - Returned");
        testBook2.setAuthor(testAuthor);
        testBook2.setLibrary(testLibrary);
        testBook2.setStatus(BookStatus.ACTIVE);
        testBook2 = bookRepository.save(testBook2);

        testBook3 = new Book();
        testBook3.setTitle("Book 3 - Other User");
        testBook3.setAuthor(testAuthor);
        testBook3.setLibrary(testLibrary);
        testBook3.setStatus(BookStatus.ACTIVE);
        testBook3 = bookRepository.save(testBook3);

        // Create test users
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setSsoProvider("local");
        testUser.setLibraryCardDesign(LibraryCardDesign.CLASSICAL_DEVOTION);
        Set<Authority> userAuthorities = new HashSet<>();
        userAuthorities.add(userAuthority);
        testUser.setAuthorities(userAuthorities);
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setPassword("password");
        otherUser.setSsoProvider("local");
        otherUser.setLibraryCardDesign(LibraryCardDesign.CLASSICAL_DEVOTION);
        Set<Authority> otherAuthorities = new HashSet<>();
        otherAuthorities.add(userAuthority);
        otherUser.setAuthorities(otherAuthorities);
        otherUser = userRepository.save(otherUser);

        librarianUser = new User();
        librarianUser.setUsername("librarian");
        librarianUser.setPassword("password");
        librarianUser.setSsoProvider("local");
        librarianUser.setLibraryCardDesign(LibraryCardDesign.CLASSICAL_DEVOTION);
        Set<Authority> librarianAuthorities = new HashSet<>();
        librarianAuthorities.add(librarianAuthority);
        librarianUser.setAuthorities(librarianAuthorities);
        librarianUser = userRepository.save(librarianUser);

        // Create test loans
        // Active loan for testUser
        activeLoan1 = new Loan();
        activeLoan1.setBook(testBook1);
        activeLoan1.setUser(testUser);
        activeLoan1.setLoanDate(LocalDate.now().minusDays(5));
        activeLoan1.setDueDate(LocalDate.now().plusDays(9));
        activeLoan1.setReturnDate(null);
        activeLoan1 = loanRepository.save(activeLoan1);

        // Returned loan for testUser
        returnedLoan = new Loan();
        returnedLoan.setBook(testBook2);
        returnedLoan.setUser(testUser);
        returnedLoan.setLoanDate(LocalDate.now().minusDays(20));
        returnedLoan.setDueDate(LocalDate.now().minusDays(6));
        returnedLoan.setReturnDate(LocalDate.now().minusDays(5));
        returnedLoan = loanRepository.save(returnedLoan);

        // Active loan for otherUser
        activeLoan2 = new Loan();
        activeLoan2.setBook(testBook3);
        activeLoan2.setUser(otherUser);
        activeLoan2.setLoanDate(LocalDate.now().minusDays(3));
        activeLoan2.setDueDate(LocalDate.now().plusDays(11));
        activeLoan2.setReturnDate(null);
        activeLoan2 = loanRepository.save(activeLoan2);
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "USER")
    void testGetLoansAsUser_OnlyActiveLoans() throws Exception {
        // Regular user should only see their own active loans (not returned, not other users')
        mockMvc.perform(get("/api/loans")
                        .param("showAll", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(activeLoan1.getId().intValue())))
                .andExpect(jsonPath("$[0].bookTitle", is("Book 1 - Active Loan")))
                .andExpect(jsonPath("$[0].userName", is("testuser")))
                .andExpect(jsonPath("$[0].bookId", is(testBook1.getId().intValue())))
                .andExpect(jsonPath("$[0].userId", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$[0].returnDate", nullValue()));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "USER")
    void testGetLoansAsUser_AllLoans() throws Exception {
        // Regular user should see their own loans (both active and returned)
        mockMvc.perform(get("/api/loans")
                        .param("showAll", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        activeLoan1.getId().intValue(),
                        returnedLoan.getId().intValue()
                )))
                .andExpect(jsonPath("$[*].bookTitle", containsInAnyOrder(
                        "Book 1 - Active Loan",
                        "Book 2 - Returned"
                )))
                .andExpect(jsonPath("$[*].userName", everyItem(is("testuser"))));
    }

    @Test
    @WithMockUser(username = "librarian", authorities = "LIBRARIAN")
    void testGetLoansAsLibrarian_OnlyActiveLoans() throws Exception {
        // Librarian should see all active loans from all users
        mockMvc.perform(get("/api/loans")
                        .param("showAll", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        activeLoan1.getId().intValue(),
                        activeLoan2.getId().intValue()
                )))
                .andExpect(jsonPath("$[*].bookTitle", containsInAnyOrder(
                        "Book 1 - Active Loan",
                        "Book 3 - Other User"
                )))
                .andExpect(jsonPath("$[*].userName", containsInAnyOrder("testuser", "otheruser")))
                .andExpect(jsonPath("$[*].returnDate", everyItem(nullValue())));
    }

    @Test
    @WithMockUser(username = "librarian", authorities = "LIBRARIAN")
    void testGetLoansAsLibrarian_AllLoans() throws Exception {
        // Librarian should see all loans from all users (active and returned)
        mockMvc.perform(get("/api/loans")
                        .param("showAll", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        activeLoan1.getId().intValue(),
                        activeLoan2.getId().intValue(),
                        returnedLoan.getId().intValue()
                )))
                .andExpect(jsonPath("$[*].bookTitle", containsInAnyOrder(
                        "Book 1 - Active Loan",
                        "Book 2 - Returned",
                        "Book 3 - Other User"
                )))
                .andExpect(jsonPath("$[*].userName", hasItems("testuser", "otheruser")));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "USER")
    void testGetLoans_VerifyBookTitleNotNull() throws Exception {
        // Verify that bookTitle field is properly populated (tests JOIN FETCH)
        mockMvc.perform(get("/api/loans")
                        .param("showAll", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookTitle", notNullValue()))
                .andExpect(jsonPath("$[0].bookTitle", not(emptyString())));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "USER")
    void testGetLoans_VerifyUserNameNotNull() throws Exception {
        // Verify that userName field is properly populated (tests JOIN FETCH)
        mockMvc.perform(get("/api/loans")
                        .param("showAll", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userName", notNullValue()))
                .andExpect(jsonPath("$[0].userName", not(emptyString())));
    }

    @Test
    @WithMockUser(username = "librarian", authorities = "LIBRARIAN")
    void testGetLoans_VerifyAllFieldsPresent() throws Exception {
        // Comprehensive verification of all loan DTO fields
        mockMvc.perform(get("/api/loans")
                        .param("showAll", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].bookId", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].bookTitle", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].userId", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].userName", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].loanDate", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].dueDate", everyItem(notNullValue())));
    }

    @Test
    @WithMockUser(username = "otheruser", authorities = "USER")
    void testGetLoansAsOtherUser_OnlySeesOwnLoans() throws Exception {
        // otherUser should only see their own loan, not testuser's loans
        mockMvc.perform(get("/api/loans")
                        .param("showAll", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(activeLoan2.getId().intValue())))
                .andExpect(jsonPath("$[0].bookTitle", is("Book 3 - Other User")))
                .andExpect(jsonPath("$[0].userName", is("otheruser")));
    }

    @Test
    void testGetLoans_Unauthenticated() throws Exception {
        // Unauthenticated requests should be rejected
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "nonexistentuser", authorities = "USER")
    void testGetLoans_UserNotFound() throws Exception {
        // Request for a user that doesn't exist should return error
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "librarian", authorities = "LIBRARIAN")
    void testGetLoans_EmptyDatabase() throws Exception {
        // Delete all loans first
        loanRepository.deleteAll();

        // Librarian viewing empty database should get empty array, not error
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "userwithnoloans", authorities = "USER")
    void testGetLoans_UserWithNoLoans() throws Exception {
        // Create a new user with no loans
        User newUser = new User();
        newUser.setUsername("userwithnoloans");
        newUser.setPassword("password");
        newUser.setSsoProvider("local");
        newUser.setLibraryCardDesign(LibraryCardDesign.CLASSICAL_DEVOTION);
        Set<Authority> authorities = new HashSet<>();
        authorities.add(userAuthority);
        newUser.setAuthorities(authorities);
        newUser = userRepository.save(newUser);

        // Request loans for user with no loans should return empty array
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "USER")
    void testGetLoans_ResponseTimeIsReasonable() throws Exception {
        // Verify the response is fast (no N+1 query problems)
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - startTime;

        // Response should be under 1 second (generous for CI environments)
        assert duration < 1000 : "Loan query took too long: " + duration + "ms";
    }
}
