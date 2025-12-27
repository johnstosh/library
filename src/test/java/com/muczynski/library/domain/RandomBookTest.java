/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import com.muczynski.library.repository.AuthorRepository;
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

    @Test
    void testRandomBookCreation() {
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
