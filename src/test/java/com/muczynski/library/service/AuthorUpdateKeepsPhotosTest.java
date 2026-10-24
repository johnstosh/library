/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.PhotoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthorUpdateKeepsPhotosTest {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private BookRepository bookRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void updateAuthor_keepsTheExistingPortraitWhenAnotherFieldChanges() {
        String token = UUID.randomUUID().toString().substring(0, 8);
        Author author = new Author();
        author.setName("Issue374 Author " + token);
        author.setNationality("English");
        author = authorRepository.saveAndFlush(author);

        Photo portrait = new Photo();
        portrait.setAuthor(author);
        portrait.setImage(new byte[] {1, 2, 3, 4});
        portrait.setContentType("image/jpeg");
        portrait.setCaption("Portrait");
        portrait.setPermanentId("google-photos-" + token);
        portrait.setPhotoOrder(1);
        portrait = photoRepository.saveAndFlush(portrait);

        Book book = new Book();
        book.setTitle("Issue374 Book " + token);
        book.setAuthor(author);
        book.setStatus(BookStatus.ACTIVE);
        book = bookRepository.saveAndFlush(book);

        entityManager.clear();

        AuthorDto update = new AuthorDto();
        update.setName(author.getName());
        update.setNationality("Irish");

        AuthorDto saved = authorService.updateAuthor(author.getId(), update);
        entityManager.flush();
        entityManager.clear();

        assertEquals("Irish", saved.getNationality());
        Photo kept = photoRepository.findById(portrait.getId()).orElseThrow();
        assertEquals(author.getId(), kept.getAuthor().getId());
        assertEquals("google-photos-" + token, kept.getPermanentId());
        assertEquals("Portrait", kept.getCaption());
        assertArrayEquals(new byte[] {1, 2, 3, 4}, kept.getImage());
        assertNull(kept.getDeletedAt());
        assertEquals(1, photoRepository.findByAuthorId(author.getId()).size());
        assertEquals(author.getId(), bookRepository.findById(book.getId()).orElseThrow().getAuthor().getId());
        assertEquals("Irish", authorRepository.findById(author.getId()).orElseThrow().getNationality());
    }
}
