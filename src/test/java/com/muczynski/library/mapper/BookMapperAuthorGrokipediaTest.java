/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.BookDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookMapperAuthorGrokipediaTest {

    private final BookMapper bookMapper = new BookMapper();

    @Test
    void toDtoWithData_copiesAuthorGrokipediaUrl() {
        Author author = new Author();
        author.setId(7L);
        author.setName("Mark Twain");
        author.setGrokipediaUrl("https://grokipedia.com/page/Mark_Twain");

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Adventures of Huckleberry Finn");
        book.setStatus(BookStatus.ACTIVE);
        book.setAuthor(author);

        BookDto dto = bookMapper.toDtoWithData(book, null, null, 0);

        assertEquals(7L, dto.getAuthorId());
        assertEquals("Mark Twain", dto.getAuthor());
        assertEquals("https://grokipedia.com/page/Mark_Twain", dto.getAuthorGrokipediaUrl());
    }

    @Test
    void toDtoWithData_leavesAuthorGrokipediaUrlNullWhenAuthorHasNone() {
        Author author = new Author();
        author.setId(8L);
        author.setName("Unknown Author");

        Book book = new Book();
        book.setId(2L);
        book.setTitle("Obscure Title");
        book.setStatus(BookStatus.ACTIVE);
        book.setAuthor(author);

        BookDto dto = bookMapper.toDtoWithData(book, null, null, 0);

        assertEquals("Unknown Author", dto.getAuthor());
        assertNull(dto.getAuthorGrokipediaUrl());
    }
}
