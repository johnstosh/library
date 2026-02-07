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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestDataServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private RandomAuthor randomAuthor;

    @Mock
    private RandomBook randomBook;

    @Mock
    private RandomLoan randomLoan;

    @Mock
    private RandomUser randomUser;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookService bookService;

    @InjectMocks
    private TestDataService testDataService;

    @BeforeEach
    void setUp() {
        // ensureUniqueTitle returns the title as-is by default
        lenient().when(bookService.ensureUniqueTitle(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testGenerateTestData_CreatesLibraryIfNoneExists() {
        // Arrange
        when(branchRepository.findAll()).thenReturn(Collections.emptyList());
        when(randomAuthor.create()).thenReturn(new Author());
        when(randomBook.create(any(Author.class))).thenReturn(new Book());

        // Act
        testDataService.generateTestData(1);

        // Assert
        verify(branchRepository, times(1)).save(any(Library.class));
        verify(authorRepository, times(1)).save(any(Author.class));
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void testGenerateTestData_DoesNotCreateLibraryIfExists() {
        // Arrange
        Library existingLibrary = new Library();
        when(branchRepository.findAll()).thenReturn(Collections.singletonList(existingLibrary));
        when(randomAuthor.create()).thenReturn(new Author());
        when(randomBook.create(any(Author.class))).thenReturn(new Book());

        // Act
        testDataService.generateTestData(1);

        // Assert
        verify(branchRepository, never()).save(any(Library.class));
        verify(authorRepository, times(1)).save(any(Author.class));
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void testGenerateTestData_CreatesCorrectNumberOfBooksAndAuthors() {
        // Arrange
        Library library = new Library();
        when(branchRepository.findAll()).thenReturn(Collections.singletonList(library));
        when(randomAuthor.create()).thenReturn(new Author());
        when(randomBook.create(any(Author.class))).thenReturn(new Book());

        // Act
        testDataService.generateTestData(5);

        // Assert
        verify(randomAuthor, times(5)).create();
        verify(randomBook, times(5)).create(any(Author.class));
        verify(authorRepository, times(5)).save(any(Author.class));
        verify(bookRepository, times(5)).save(any(Book.class));
    }

    @Test
    void testGenerateTestData_WithZeroCount() {
        // Arrange
        Library library = new Library();
        when(branchRepository.findAll()).thenReturn(Collections.singletonList(library));

        // Act
        testDataService.generateTestData(0);

        // Assert
        verify(randomAuthor, never()).create();
        verify(randomBook, never()).create(any(Author.class));
        verify(authorRepository, never()).save(any(Author.class));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void testGenerateLoanData_CreatesCorrectNumberOfLoans() {
        // Arrange
        when(randomLoan.create()).thenReturn(new Loan());

        // Act
        testDataService.generateLoanData(3);

        // Assert
        verify(randomLoan, times(3)).create();
        verify(loanRepository, times(3)).save(any(Loan.class));
    }

    @Test
    void testGenerateLoanData_WithZeroCount() {
        // Act
        testDataService.generateLoanData(0);

        // Assert
        verify(randomLoan, never()).create();
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void testGenerateUserData_CreatesCorrectNumberOfUsers() {
        // Arrange
        when(randomUser.create()).thenReturn(new User());

        // Act
        testDataService.generateUserData(3);

        // Assert
        verify(randomUser, times(3)).create();
        verify(userRepository, times(3)).save(any(User.class));
    }

    @Test
    void testGenerateUserData_WithZeroCount() {
        // Act
        testDataService.generateUserData(0);

        // Assert
        verify(randomUser, never()).create();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testDeleteTestData_DeletesLoansAndBooks() {
        // Arrange
        when(authorRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        testDataService.deleteTestData();

        // Assert
        verify(loanRepository, times(1)).deleteByLoanDate(LocalDate.of(2099, 1, 1));
        verify(bookRepository, times(1)).deleteByPublisher("test-data");
    }

    @Test
    void testDeleteTestData_DeletesAuthorsAndPhotos() {
        // Arrange
        Author testAuthor = new Author();
        testAuthor.setId(1L);
        testAuthor.setReligiousAffiliation("test-data");

        Author normalAuthor = new Author();
        normalAuthor.setId(2L);
        normalAuthor.setReligiousAffiliation("Catholic");

        List<Author> allAuthors = List.of(testAuthor, normalAuthor);
        when(authorRepository.findAll()).thenReturn(allAuthors);

        Photo photo1 = new Photo();
        Photo photo2 = new Photo();
        when(photoRepository.findByAuthorId(1L)).thenReturn(List.of(photo1, photo2));

        // Act
        testDataService.deleteTestData();

        // Assert
        verify(photoRepository, times(1)).findByAuthorId(1L);
        verify(photoRepository, times(1)).deleteAll(List.of(photo1, photo2));
        verify(authorRepository, times(1)).deleteAll(Collections.singletonList(testAuthor));
    }

    @Test
    void testDeleteTestData_DoesNotDeleteNonTestAuthors() {
        // Arrange
        Author normalAuthor = new Author();
        normalAuthor.setId(1L);
        normalAuthor.setReligiousAffiliation("Catholic");

        when(authorRepository.findAll()).thenReturn(Collections.singletonList(normalAuthor));
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        testDataService.deleteTestData();

        // Assert
        verify(photoRepository, never()).findByAuthorId(any());
        verify(photoRepository, never()).deleteAll(any());
        verify(authorRepository, never()).deleteAll(any());
    }

    @Test
    void testDeleteTestData_DeletesTestUsers() {
        // Arrange
        User testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setUsername("test-data-john.doe123");

        User testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setUsername("test-data-jane.smith456");

        User normalUser = new User();
        normalUser.setId(3L);
        normalUser.setUsername("normaluser");

        List<User> allUsers = List.of(testUser1, testUser2, normalUser);
        when(userRepository.findAll()).thenReturn(allUsers);
        when(authorRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        testDataService.deleteTestData();

        // Assert
        verify(userRepository, times(1)).deleteAll(List.of(testUser1, testUser2));
    }

    @Test
    void testDeleteTestData_DoesNotDeleteNonTestUsers() {
        // Arrange
        User normalUser = new User();
        normalUser.setId(1L);
        normalUser.setUsername("regularuser");

        when(userRepository.findAll()).thenReturn(Collections.singletonList(normalUser));
        when(authorRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        testDataService.deleteTestData();

        // Assert
        verify(userRepository, never()).deleteAll(any());
    }

    @Test
    void testTotalPurge() {
        // Act
        testDataService.totalPurge();

        // Assert
        verify(jdbcTemplate, times(1)).execute("DROP TABLE IF EXISTS users_roles CASCADE");
        verify(jdbcTemplate, times(1)).execute("DROP TABLE IF EXISTS loan CASCADE");
        verify(jdbcTemplate, times(1)).execute("DROP TABLE IF EXISTS photo CASCADE");
        verify(jdbcTemplate, times(1)).execute("DROP TABLE IF EXISTS book CASCADE");
        verify(jdbcTemplate, times(1)).execute("DROP TABLE IF EXISTS users CASCADE");
        verify(jdbcTemplate, times(1)).execute("DROP TABLE IF EXISTS role CASCADE");
        verify(jdbcTemplate, times(1)).execute("DROP TABLE IF EXISTS author CASCADE");
        verify(jdbcTemplate, times(1)).execute("DROP TABLE IF EXISTS library CASCADE");
        verify(jdbcTemplate, times(1)).execute("DROP TABLE IF EXISTS applied CASCADE");
    }
}
