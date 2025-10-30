/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.LibraryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RandomBookTest {

    @Autowired
    private RandomBook randomBook;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Test
    void testRandomBookCreation() {
        Library library = new Library();
        library.setName("St. Martin de Porres");
        libraryRepository.save(library);

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
        assertEquals("St. Martin de Porres", book.getLibrary().getName());
        assertNull(book.getLocNumber());
        assertNull(book.getStatusReason());
    }
}
