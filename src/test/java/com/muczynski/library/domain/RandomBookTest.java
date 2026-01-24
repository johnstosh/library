/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BranchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RandomBookTest {

    @Autowired
    private RandomBook randomBook;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    void testRandomBookCreation() {
        // Create a library if one doesn't exist (required by RandomBook.create)
        if (branchRepository.count() == 0) {
            Library library = new Library();
            library.setBranchName("Test Library");
            library.setLibrarySystemName("Test Library System");
            branchRepository.save(library);
        }

        Author author = new Author();
        author.setName("Test Author");
        authorRepository.save(author);

        Book book = randomBook.create(author);

        assertNotNull(book);
        assertNotNull(book.getTitle());
        assertFalse(book.getTitle().isEmpty());
        assertNotNull(book.getAuthor());
        assertEquals("Test Author", book.getAuthor().getName());
        assertNotNull(book.getLibrary());
        // RandomBook now generates all fields including LOC number
        assertNotNull(book.getLocNumber(), "RandomBook should generate LOC number");
        assertTrue(book.getLocNumber().matches("[A-Z]{2}\\d+\\.[A-Z]\\d+"),
                "LOC number should match expected format");
        assertNull(book.getStatusReason());
        // Verify other populated fields
        assertNotNull(book.getPublisher(), "Publisher should be generated");
        assertTrue(book.getPublisher().startsWith("test-data"), "Publisher should be marked as test-data");
        assertNotNull(book.getPublicationYear(), "Publication year should be generated");
        assertNotNull(book.getPlotSummary(), "Plot summary should be generated");
        assertNotNull(book.getRelatedWorks(), "Related works should be generated");
        assertNotNull(book.getDetailedDescription(), "Detailed description should be generated");
        assertNotNull(book.getGrokipediaUrl(), "Grokipedia URL should be generated");
        assertNotNull(book.getFreeTextUrl(), "Free text URL should be generated");
        assertNotNull(book.getDateAddedToLibrary(), "Date added should be generated");
        assertEquals(BookStatus.ACTIVE, book.getStatus(), "Status should be ACTIVE");
    }
}
