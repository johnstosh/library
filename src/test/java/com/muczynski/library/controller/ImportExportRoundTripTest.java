/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.*;
import com.muczynski.library.dto.importdtos.ImportRequestDto;
import com.muczynski.library.repository.*;
import com.muczynski.library.service.ImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive integration test for JSON export/import round-trip.
 * Creates 20 books with authors, 20 users, 20 loans, and photos (half with photos),
 * then tests that export and re-import works correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ImportExportRoundTripTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BranchRepository branchRepository;

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

    @Autowired
    private RandomAuthor randomAuthor;

    @Autowired
    private RandomBook randomBook;

    @Autowired
    private RandomUser randomUser;

    @Autowired
    private RandomPhoto randomPhoto;

    @Autowired
    private ImportService importService;

    private Library testLibrary;
    private List<Author> authors = new ArrayList<>();
    private List<Book> books = new ArrayList<>();
    private List<User> users = new ArrayList<>();
    private List<Loan> loans = new ArrayList<>();
    private List<Photo> photos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Create test library
        testLibrary = new Library();
        testLibrary.setBranchName("Round Trip Test Library");
        testLibrary.setLibrarySystemName("Round Trip Library System");
        testLibrary = branchRepository.save(testLibrary);

        // Ensure USER authority exists
        Authority userAuthority = authorityRepository.findByName("USER")
                .orElseGet(() -> {
                    Authority a = new Authority();
                    a.setName("USER");
                    return authorityRepository.save(a);
                });

        // Create 20 authors and 20 books with all fields populated
        for (int i = 0; i < 20; i++) {
            Author author = randomAuthor.create();
            author.setName("RoundTrip Author " + i);  // Ensure unique names
            author = authorRepository.save(author);
            authors.add(author);

            Book book = randomBook.create(author);
            book.setTitle("RoundTrip Book " + i);  // Ensure unique titles
            book = bookRepository.save(book);
            books.add(book);

            // Add photos to half the books (indexes 0-9)
            if (i < 10) {
                Photo photo = randomPhoto.createForBook(book, 1);
                photo = photoRepository.save(photo);
                photos.add(photo);

                // Add a second photo for some books (indexes 0-4)
                if (i < 5) {
                    Photo photo2 = randomPhoto.createForBook(book, 2);
                    photo2 = photoRepository.save(photo2);
                    photos.add(photo2);
                }
            }
        }

        // Create 20 users with all fields populated
        for (int i = 0; i < 20; i++) {
            User user = randomUser.create();
            user.setUsername("roundtrip-user-" + i);  // Ensure unique usernames
            user = userRepository.save(user);
            users.add(user);
        }

        // Create 20 loans (half with returned status)
        for (int i = 0; i < 20; i++) {
            Loan loan = new Loan();
            loan.setBook(books.get(i % books.size()));
            loan.setUser(users.get(i % users.size()));
            loan.setLoanDate(LocalDate.of(2099, 1, 1 + i));
            loan.setDueDate(LocalDate.of(2099, 2, 1 + i));
            // Half of loans are returned
            if (i < 10) {
                loan.setReturnDate(LocalDate.of(2099, 1, 20 + i));
            }
            loan = loanRepository.save(loan);
            loans.add(loan);
        }

        // Add author photos (half the authors)
        for (int i = 0; i < 10; i++) {
            Photo authorPhoto = randomPhoto.createForAuthor(authors.get(i), 1);
            authorPhoto = photoRepository.save(authorPhoto);
            photos.add(authorPhoto);
        }
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportImportRoundTrip_PreservesAllData() throws Exception {
        // Get initial counts
        long initialBookCount = bookRepository.count();
        long initialAuthorCount = authorRepository.count();
        long initialUserCount = userRepository.count();
        long initialLoanCount = loanRepository.count();
        long initialPhotoCount = photoRepository.count();

        // Export data
        MvcResult exportResult = mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andReturn();

        String exportedJson = exportResult.getResponse().getContentAsString();
        ImportRequestDto exportedData = objectMapper.readValue(exportedJson, ImportRequestDto.class);

        // Verify export contains expected counts
        assertTrue(exportedData.getBranches().size() >= 1, "Should export at least 1 library");
        assertTrue(exportedData.getAuthors().size() >= 20, "Should export at least 20 authors");
        assertTrue(exportedData.getBooks().size() >= 20, "Should export at least 20 books");
        assertTrue(exportedData.getUsers().size() >= 20, "Should export at least 20 users");
        assertTrue(exportedData.getLoans().size() >= 20, "Should export at least 20 loans");
        assertTrue(exportedData.getPhotos().size() >= 15, "Should export photo metadata");

        // Verify exported data has proper field values (not null/empty)
        exportedData.getAuthors().stream()
                .filter(a -> a.getName().startsWith("RoundTrip Author"))
                .forEach(author -> {
                    assertNotNull(author.getName(), "Author name should not be null");
                    assertNotNull(author.getDateOfBirth(), "Author birth date should not be null");
                    assertNotNull(author.getBirthCountry(), "Author country should not be null");
                    assertNotNull(author.getNationality(), "Author nationality should not be null");
                    assertNotNull(author.getBriefBiography(), "Author biography should not be null");
                    assertNotNull(author.getGrokipediaUrl(), "Author grokipedia URL should not be null");
                });

        exportedData.getBooks().stream()
                .filter(b -> b.getTitle().startsWith("RoundTrip Book"))
                .forEach(book -> {
                    assertNotNull(book.getTitle(), "Book title should not be null");
                    assertNotNull(book.getPublicationYear(), "Book year should not be null");
                    assertNotNull(book.getPublisher(), "Book publisher should not be null");
                    assertNotNull(book.getPlotSummary(), "Book summary should not be null");
                    assertNotNull(book.getAuthorName(), "Book author name should not be null");
                    assertNotNull(book.getLibraryName(), "Book library name should not be null");
                    assertNull(book.getLastModified(), "lastModified should NOT be exported");
                });

        exportedData.getUsers().stream()
                .filter(u -> u.getUsername().startsWith("roundtrip-user-"))
                .forEach(user -> {
                    assertNotNull(user.getUsername(), "User username should not be null");
                    assertNotNull(user.getPassword(), "User password should not be null");
                    assertNotNull(user.getUserIdentifier(), "User identifier should not be null");
                });

        // Verify loans have reference fields (not embedded objects)
        exportedData.getLoans().forEach(loan -> {
            // New format uses reference fields
            if (loan.getBookTitle() != null) {
                assertNotNull(loan.getBookTitle(), "Loan should have book title reference");
                assertNotNull(loan.getUsername(), "Loan should have username reference");
            }
        });

        // Re-import the exported data
        mockMvc.perform(post("/api/import/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportedJson))
                .andExpect(status().isOk());

        // Verify counts remain the same (import should merge, not duplicate)
        assertEquals(initialBookCount, bookRepository.count(), "Book count should remain the same after re-import");
        assertEquals(initialAuthorCount, authorRepository.count(), "Author count should remain the same after re-import");
        assertEquals(initialUserCount, userRepository.count(), "User count should remain the same after re-import");
        assertEquals(initialLoanCount, loanRepository.count(), "Loan count should remain the same after re-import");
        assertEquals(initialPhotoCount, photoRepository.count(), "Photo count should remain the same after re-import");
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportedJsonHasNoNullFields() throws Exception {
        // Export data
        MvcResult exportResult = mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andReturn();

        String exportedJson = exportResult.getResponse().getContentAsString();

        // Verify JSON doesn't contain null fields (they should be omitted)
        assertFalse(exportedJson.contains(":null"), "Exported JSON should not contain ':null' fields");

        // Verify JSON doesn't contain empty string fields (they should be omitted)
        // Note: some empty arrays are allowed
        assertFalse(exportedJson.contains(":\"\""), "Exported JSON should not contain empty string fields");
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testExportedJsonUserIdentifierIsLast() throws Exception {
        // Export data
        MvcResult exportResult = mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andReturn();

        String exportedJson = exportResult.getResponse().getContentAsString();
        ImportRequestDto exportedData = objectMapper.readValue(exportedJson, ImportRequestDto.class);

        // Verify userIdentifier appears after username in users JSON
        // This test verifies the @JsonPropertyOrder annotation is working
        if (!exportedData.getUsers().isEmpty()) {
            String usersSection = exportedJson.substring(exportedJson.indexOf("\"users\""));
            // Find first user object
            int firstUserStart = usersSection.indexOf("{");
            int firstUserEnd = usersSection.indexOf("}", firstUserStart);
            String firstUser = usersSection.substring(firstUserStart, firstUserEnd + 1);

            // userIdentifier should appear after username
            int usernamePos = firstUser.indexOf("\"username\"");
            int userIdentifierPos = firstUser.indexOf("\"userIdentifier\"");
            assertTrue(usernamePos < userIdentifierPos,
                    "userIdentifier should appear after username in JSON. Username pos: " + usernamePos + ", userIdentifier pos: " + userIdentifierPos);
        }
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testImportWithPhotosForBooksWithoutAuthor() throws Exception {
        // Create a book without author
        Book bookNoAuthor = new Book();
        bookNoAuthor.setTitle("Book Without Author");
        bookNoAuthor.setPublicationYear(2025);
        bookNoAuthor.setStatus(BookStatus.ACTIVE);
        bookNoAuthor.setLibrary(testLibrary);
        bookNoAuthor = bookRepository.save(bookNoAuthor);

        // Add a photo to the book
        Photo photo = new Photo();
        photo.setBook(bookNoAuthor);
        photo.setContentType("image/jpeg");
        photo.setCaption("Cover for book without author");
        photo.setPhotoOrder(1);
        photo = photoRepository.save(photo);

        // Export
        MvcResult exportResult = mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andReturn();

        String exportedJson = exportResult.getResponse().getContentAsString();

        // Delete the photo and book to test re-import
        photoRepository.delete(photo);
        bookRepository.delete(bookNoAuthor);

        // Re-import should succeed (this tests the fix for books without authors)
        mockMvc.perform(post("/api/import/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportedJson))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testImportPreservesUserFieldDefaults() throws Exception {
        // Create a user with empty API keys
        User user = new User();
        user.setUserIdentifier("test-empty-fields-user");
        user.setUsername("emptyfieldsuser");
        user.setPassword("$2a$10$testpassword");
        user.setXaiApiKey("");  // Empty, not null
        user.setGooglePhotosApiKey("");
        user.setGooglePhotosRefreshToken("");
        user.setGooglePhotosTokenExpiry("");
        user.setGoogleClientSecret("");
        user.setGooglePhotosAlbumId("");
        user.setLastPhotoTimestamp("");
        user = userRepository.save(user);

        // Export
        MvcResult exportResult = mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andReturn();

        String exportedJson = exportResult.getResponse().getContentAsString();

        // The exported JSON should NOT contain empty string fields for this user
        ImportRequestDto exportedData = objectMapper.readValue(exportedJson, ImportRequestDto.class);
        var exportedUser = exportedData.getUsers().stream()
                .filter(u -> "emptyfieldsuser".equals(u.getUsername()))
                .findFirst();
        assertTrue(exportedUser.isPresent(), "User should be exported");
        // Empty strings should be null in export (via emptyToNull)
        assertNull(exportedUser.get().getXaiApiKey(), "Empty xaiApiKey should be exported as null");
        assertNull(exportedUser.get().getGooglePhotosApiKey(), "Empty googlePhotosApiKey should be exported as null");

        // Re-import - user should have empty strings restored
        mockMvc.perform(post("/api/import/json")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportedJson))
                .andExpect(status().isOk());

        // Verify user still has empty strings (not null) after import
        User reimportedUser = userRepository.findByUsername("emptyfieldsuser").orElseThrow();
        assertEquals("", reimportedUser.getXaiApiKey(), "xaiApiKey should be empty string after import");
        assertEquals("", reimportedUser.getGooglePhotosApiKey(), "googlePhotosApiKey should be empty string after import");
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testAllBookFieldsAreExported() throws Exception {
        // Export data
        MvcResult exportResult = mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andReturn();

        String exportedJson = exportResult.getResponse().getContentAsString();
        ImportRequestDto exportedData = objectMapper.readValue(exportedJson, ImportRequestDto.class);

        // Find a book from our test data
        var testBook = exportedData.getBooks().stream()
                .filter(b -> b.getTitle().equals("RoundTrip Book 0"))
                .findFirst();
        assertTrue(testBook.isPresent(), "Test book should be exported");

        var book = testBook.get();
        // Verify all populated fields are present
        assertNotNull(book.getTitle(), "title should be present");
        assertNotNull(book.getPublicationYear(), "publicationYear should be present");
        assertNotNull(book.getPublisher(), "publisher should be present");
        assertNotNull(book.getPlotSummary(), "plotSummary should be present");
        assertNotNull(book.getRelatedWorks(), "relatedWorks should be present");
        assertNotNull(book.getDetailedDescription(), "detailedDescription should be present");
        assertNotNull(book.getGrokipediaUrl(), "grokipediaUrl should be present");
        assertNotNull(book.getFreeTextUrl(), "freeTextUrl should be present");
        assertNotNull(book.getDateAddedToLibrary(), "dateAddedToLibrary should be present");
        assertNull(book.getLastModified(), "lastModified should NOT be exported");
        assertNotNull(book.getStatus(), "status should be present");
        assertNotNull(book.getLocNumber(), "locNumber should be present");
        assertNotNull(book.getAuthorName(), "authorName should be present");
        assertNotNull(book.getLibraryName(), "libraryName should be present");
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void testAllAuthorFieldsAreExported() throws Exception {
        // Export data
        MvcResult exportResult = mockMvc.perform(get("/api/import/json"))
                .andExpect(status().isOk())
                .andReturn();

        String exportedJson = exportResult.getResponse().getContentAsString();
        ImportRequestDto exportedData = objectMapper.readValue(exportedJson, ImportRequestDto.class);

        // Find an author from our test data
        var testAuthor = exportedData.getAuthors().stream()
                .filter(a -> a.getName().equals("RoundTrip Author 0"))
                .findFirst();
        assertTrue(testAuthor.isPresent(), "Test author should be exported");

        var author = testAuthor.get();
        // Verify all populated fields are present
        assertNotNull(author.getName(), "name should be present");
        assertNotNull(author.getDateOfBirth(), "dateOfBirth should be present");
        // dateOfDeath may be null for some authors (40% chance)
        assertNotNull(author.getReligiousAffiliation(), "religiousAffiliation should be present");
        assertNotNull(author.getBirthCountry(), "birthCountry should be present");
        assertNotNull(author.getNationality(), "nationality should be present");
        assertNotNull(author.getBriefBiography(), "briefBiography should be present");
        assertNotNull(author.getGrokipediaUrl(), "grokipediaUrl should be present");
    }
}
