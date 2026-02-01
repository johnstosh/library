/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.SearchResponseDto;
import com.muczynski.library.mapper.AuthorMapper;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private AuthorMapper authorMapper;

    @InjectMocks
    private SearchService searchService;

    @Test
    void searchWithResultsFound() {
        // Arrange
        String query = "test";
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        List<Book> bookList = Arrays.asList(book);
        Page<Book> bookPage = new PageImpl<>(bookList, pageable, 1);

        Author author = new Author();
        author.setId(1L);
        author.setName("Test Author");
        List<Author> authorList = Arrays.asList(author);
        Page<Author> authorPage = new PageImpl<>(authorList, pageable, 1);

        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Test Book");

        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Test Author");

        when(bookRepository.findByTitleContainingIgnoreCaseAndLocNumberIsNotNull(eq(query), any(Pageable.class))).thenReturn(bookPage);
        when(authorRepository.findByNameContainingIgnoreCase(eq(query), any(Pageable.class))).thenReturn(authorPage);
        when(bookMapper.toDto(book)).thenReturn(bookDto);
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        // Act
        SearchResponseDto result = searchService.search(query, page, size, "IN_LIBRARY");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals(1, result.getAuthors().size());
        assertEquals(bookDto, result.getBooks().get(0));
        assertEquals(authorDto, result.getAuthors().get(0));

        assertEquals(1, result.getBookPage().getTotalPages());
        assertEquals(1, result.getBookPage().getTotalElements());
        assertEquals(0, result.getBookPage().getCurrentPage());
        assertEquals(20, result.getBookPage().getPageSize());

        assertEquals(1, result.getAuthorPage().getTotalPages());
        assertEquals(1, result.getAuthorPage().getTotalElements());
        assertEquals(0, result.getAuthorPage().getCurrentPage());
        assertEquals(20, result.getAuthorPage().getPageSize());
    }

    @Test
    void searchWithNoResults() {
        // Arrange
        String query = "nonexistent";
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Page<Book> emptyBookPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        Page<Author> emptyAuthorPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(bookRepository.findByTitleContainingIgnoreCaseAndLocNumberIsNotNull(eq(query), any(Pageable.class))).thenReturn(emptyBookPage);
        when(authorRepository.findByNameContainingIgnoreCase(eq(query), any(Pageable.class))).thenReturn(emptyAuthorPage);

        // Act
        SearchResponseDto result = searchService.search(query, page, size, "IN_LIBRARY");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
        assertEquals(0, result.getAuthors().size());
        assertEquals(0, result.getBookPage().getTotalElements());
        assertEquals(0, result.getAuthorPage().getTotalElements());
    }

    @Test
    void searchWithEmptyQueryReturnsAllBooks() {
        // Arrange
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        List<Book> bookList = Arrays.asList(book);
        Page<Book> bookPage = new PageImpl<>(bookList, pageable, 1);

        Author author = new Author();
        author.setId(1L);
        author.setName("Test Author");
        List<Author> authorList = Arrays.asList(author);
        Page<Author> authorPage = new PageImpl<>(authorList, pageable, 1);

        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Test Book");

        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Test Author");

        when(bookRepository.findAll(any(Pageable.class))).thenReturn(bookPage);
        when(authorRepository.findAll(any(Pageable.class))).thenReturn(authorPage);
        when(bookMapper.toDto(book)).thenReturn(bookDto);
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        // Act
        SearchResponseDto result = searchService.search("", page, size, "IN_LIBRARY");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals(1, result.getAuthors().size());
    }

    @Test
    void searchWithNullQueryReturnsAllBooks() {
        // Arrange
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Page<Book> bookPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        Page<Author> authorPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(bookRepository.findAll(any(Pageable.class))).thenReturn(bookPage);
        when(authorRepository.findAll(any(Pageable.class))).thenReturn(authorPage);

        // Act
        SearchResponseDto result = searchService.search(null, page, size, "IN_LIBRARY");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
        assertEquals(0, result.getAuthors().size());
    }

    @Test
    void searchWithWhitespaceQueryReturnsAllBooks() {
        // Arrange
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Page<Book> bookPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        Page<Author> authorPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(bookRepository.findAll(any(Pageable.class))).thenReturn(bookPage);
        when(authorRepository.findAll(any(Pageable.class))).thenReturn(authorPage);

        // Act
        SearchResponseDto result = searchService.search("   ", page, size, "IN_LIBRARY");

        // Assert
        assertNotNull(result);
    }

    @Test
    void searchPaginationBehavior() {
        // Arrange
        String query = "test";
        int page = 1;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        List<Book> bookList = Arrays.asList(new Book(), new Book());
        Page<Book> bookPage = new PageImpl<>(bookList, pageable, 25); // 25 total, 3 pages

        List<Author> authorList = Arrays.asList(new Author());
        Page<Author> authorPage = new PageImpl<>(authorList, pageable, 11); // 11 total, 2 pages

        when(bookRepository.findByTitleContainingIgnoreCaseAndLocNumberIsNotNull(eq(query), any(Pageable.class))).thenReturn(bookPage);
        when(authorRepository.findByNameContainingIgnoreCase(eq(query), any(Pageable.class))).thenReturn(authorPage);
        when(bookMapper.toDto(any(Book.class))).thenReturn(new BookDto());
        when(authorMapper.toDto(any(Author.class))).thenReturn(new AuthorDto());

        // Act
        SearchResponseDto result = searchService.search(query, page, size, "IN_LIBRARY");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getBookPage().getTotalPages());
        assertEquals(25, result.getBookPage().getTotalElements());
        assertEquals(1, result.getBookPage().getCurrentPage());
        assertEquals(10, result.getBookPage().getPageSize());

        assertEquals(2, result.getAuthorPage().getTotalPages());
        assertEquals(11, result.getAuthorPage().getTotalElements());
        assertEquals(1, result.getAuthorPage().getCurrentPage());
        assertEquals(10, result.getAuthorPage().getPageSize());
    }

    @Test
    void searchWithSearchTypeOnline() {
        // Arrange
        String query = "test";
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Online Book");
        book.setFreeTextUrl("https://example.com/book");
        List<Book> bookList = Arrays.asList(book);
        Page<Book> bookPage = new PageImpl<>(bookList, pageable, 1);
        Page<Author> authorPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Online Book");

        when(bookRepository.findByTitleContainingIgnoreCaseAndFreeTextUrlIsNotNull(eq(query), any(Pageable.class))).thenReturn(bookPage);
        when(authorRepository.findByNameContainingIgnoreCase(eq(query), any(Pageable.class))).thenReturn(authorPage);
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        // Act
        SearchResponseDto result = searchService.search(query, page, size, "ONLINE");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("Online Book", result.getBooks().get(0).getTitle());
    }

    @Test
    void searchWithSearchTypeAll() {
        // Arrange
        String query = "test";
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Any Book");
        List<Book> bookList = Arrays.asList(book);
        Page<Book> bookPage = new PageImpl<>(bookList, pageable, 1);
        Page<Author> authorPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Any Book");

        when(bookRepository.findByTitleContainingIgnoreCase(eq(query), any(Pageable.class))).thenReturn(bookPage);
        when(authorRepository.findByNameContainingIgnoreCase(eq(query), any(Pageable.class))).thenReturn(authorPage);
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        // Act
        SearchResponseDto result = searchService.search(query, page, size, "ALL");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("Any Book", result.getBooks().get(0).getTitle());
    }

    @Test
    void searchWithSearchTypeInLibrary() {
        // Arrange
        String query = "test";
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Library Book");
        book.setLocNumber("PS3511.I9 G7");
        List<Book> bookList = Arrays.asList(book);
        Page<Book> bookPage = new PageImpl<>(bookList, pageable, 1);
        Page<Author> authorPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Library Book");

        when(bookRepository.findByTitleContainingIgnoreCaseAndLocNumberIsNotNull(eq(query), any(Pageable.class))).thenReturn(bookPage);
        when(authorRepository.findByNameContainingIgnoreCase(eq(query), any(Pageable.class))).thenReturn(authorPage);
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        // Act
        SearchResponseDto result = searchService.search(query, page, size, "IN_LIBRARY");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("Library Book", result.getBooks().get(0).getTitle());
    }
}
