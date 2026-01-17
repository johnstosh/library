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
        assertNull(book.getLocNumber());
        assertNull(book.getStatusReason());
    }
}
