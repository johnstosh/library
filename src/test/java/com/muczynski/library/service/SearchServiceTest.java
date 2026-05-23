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
import static org.mockito.ArgumentMatchers.*;
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

    // ── Helper to create a stubbed book page ──────────────────────────────

    private Page<Book> bookPageOf(Pageable pageable, Book... books) {
        return new PageImpl<>(Arrays.asList(books), pageable, books.length);
    }

    private Page<Book> emptyBookPage(Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    private Page<Author> emptyAuthorPage(Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    // ── No-filter (show all) tests ────────────────────────────────────────

    @Test
    void searchWithNoFiltersReturnsMatchingBooks() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        Author author = new Author();
        author.setId(1L);
        author.setName("Test Author");

        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Test Book");

        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Test Author");

        when(bookRepository.findWithFilters(eq(query), eq(false), eq(false), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(bookPageOf(pageable, book));
        when(authorRepository.findByNameContainingIgnoreCase(eq(query), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(author), pageable, 1));
        when(bookMapper.toDto(book)).thenReturn(bookDto);
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        SearchResponseDto result = searchService.search(query, page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals(1, result.getAuthors().size());
        assertEquals(bookDto, result.getBooks().get(0));
        assertEquals(authorDto, result.getAuthors().get(0));
        assertEquals(1, result.getBookPage().getTotalPages());
        assertEquals(1, result.getBookPage().getTotalElements());
        assertEquals(0, result.getBookPage().getCurrentPage());
        assertEquals(20, result.getBookPage().getPageSize());
    }

    @Test
    void searchWithNoResultsReturnsEmpty() {
        String query = "nonexistent";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        when(bookRepository.findWithFilters(eq(query), eq(false), eq(false), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(emptyBookPage(pageable));
        when(authorRepository.findByNameContainingIgnoreCase(eq(query), any(Pageable.class)))
                .thenReturn(emptyAuthorPage(pageable));

        SearchResponseDto result = searchService.search(query, page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
        assertEquals(0, result.getAuthors().size());
        assertEquals(0, result.getBookPage().getTotalElements());
        assertEquals(0, result.getAuthorPage().getTotalElements());
    }

    // ── Empty query tests ─────────────────────────────────────────────────

    @Test
    void searchWithEmptyQueryReturnsAllBooks() {
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Any Book");

        Author author = new Author();
        author.setId(1L);
        author.setName("Any Author");

        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Any Book");

        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Any Author");

        // Empty query uses findWithFilters with empty string
        when(bookRepository.findWithFilters(eq(""), eq(false), eq(false), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(bookPageOf(pageable, book));
        when(authorRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(author), pageable, 1));
        when(bookMapper.toDto(book)).thenReturn(bookDto);
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        SearchResponseDto result = searchService.search("", page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals(1, result.getAuthors().size());
    }

    @Test
    void searchWithNullQueryReturnsAllBooks() {
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        when(bookRepository.findWithFilters(eq(""), eq(false), eq(false), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(emptyBookPage(pageable));
        when(authorRepository.findAll(any(Pageable.class)))
                .thenReturn(emptyAuthorPage(pageable));

        SearchResponseDto result = searchService.search(null, page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
        assertEquals(0, result.getAuthors().size());
    }

    @Test
    void searchWithWhitespaceQueryTreatsAsEmpty() {
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        when(bookRepository.findWithFilters(eq(""), eq(false), eq(false), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(emptyBookPage(pageable));
        when(authorRepository.findAll(any(Pageable.class)))
                .thenReturn(emptyAuthorPage(pageable));

        SearchResponseDto result = searchService.search("   ", page, size, false, false, false, false, null);

        assertNotNull(result);
    }

    // ── Type filter tests ─────────────────────────────────────────────────

    @Test
    void searchWithInLibraryFilterPassesTrueToRepository() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Library Book");
        book.setLocNumber("PS3511.I9 G7");
        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Library Book");

        Author author = new Author();
        author.setId(1L);
        author.setName("A Library Author");
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("A Library Author");

        when(bookRepository.findWithFilters(eq(query), eq(true), eq(false), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(bookPageOf(pageable, book));
        // Filter active → uses EXISTS-based query (not name search)
        when(authorRepository.findAuthorsOfBooksMatchingFilters(
                eq(query), eq(true), eq(false), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(author), pageable, 1));
        when(bookMapper.toDto(book)).thenReturn(bookDto);
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        SearchResponseDto result = searchService.search(query, page, size, true, false, false, false, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("Library Book", result.getBooks().get(0).getTitle());
        assertEquals(1, result.getAuthors().size());
        assertEquals("A Library Author", result.getAuthors().get(0).getName());
    }

    @Test
    void searchWithElectronicFilterPassesTrueToRepository() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(2L);
        book.setTitle("Electronic Book");
        book.setElectronicResource(true);
        BookDto bookDto = new BookDto();
        bookDto.setId(2L);
        bookDto.setTitle("Electronic Book");

        when(bookRepository.findWithFilters(eq(query), eq(false), eq(true), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(bookPageOf(pageable, book));
        // Filter active → uses EXISTS-based query (not name search)
        when(authorRepository.findAuthorsOfBooksMatchingFilters(
                eq(query), eq(false), eq(true), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(emptyAuthorPage(pageable));
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        SearchResponseDto result = searchService.search(query, page, size, false, true, false, false, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("Electronic Book", result.getBooks().get(0).getTitle());
    }

    @Test
    void searchWithFreeTextFilterPassesTrueToRepository() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(3L);
        book.setTitle("Free Text Book");
        book.setFreeTextUrl("https://gutenberg.org/ebooks/1234");
        BookDto bookDto = new BookDto();
        bookDto.setId(3L);
        bookDto.setTitle("Free Text Book");

        when(bookRepository.findWithFilters(eq(query), eq(false), eq(false), eq(true), eq(false), any(Pageable.class)))
                .thenReturn(bookPageOf(pageable, book));
        // Filter active → uses EXISTS-based query (not name search)
        when(authorRepository.findAuthorsOfBooksMatchingFilters(
                eq(query), eq(false), eq(false), eq(true), eq(false), any(Pageable.class)))
                .thenReturn(emptyAuthorPage(pageable));
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        SearchResponseDto result = searchService.search(query, page, size, false, false, true, false, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("Free Text Book", result.getBooks().get(0).getTitle());
    }

    @Test
    void searchWithAudioFilterPassesTrueToRepository() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(4L);
        book.setTitle("LibriVox Book");
        book.setFreeTextUrl("https://librivox.org/some-book");
        BookDto bookDto = new BookDto();
        bookDto.setId(4L);
        bookDto.setTitle("LibriVox Book");

        when(bookRepository.findWithFilters(eq(query), eq(false), eq(false), eq(false), eq(true), any(Pageable.class)))
                .thenReturn(bookPageOf(pageable, book));
        // Filter active → uses EXISTS-based query (not name search)
        when(authorRepository.findAuthorsOfBooksMatchingFilters(
                eq(query), eq(false), eq(false), eq(false), eq(true), any(Pageable.class)))
                .thenReturn(emptyAuthorPage(pageable));
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        SearchResponseDto result = searchService.search(query, page, size, false, false, false, true, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("LibriVox Book", result.getBooks().get(0).getTitle());
    }

    @Test
    void searchWithMultipleFiltersPassesAllTrueToRepository() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        when(bookRepository.findWithFilters(eq(query), eq(true), eq(true), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(emptyBookPage(pageable));
        // Filter active → uses EXISTS-based query (not name search)
        when(authorRepository.findAuthorsOfBooksMatchingFilters(
                eq(query), eq(true), eq(true), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(emptyAuthorPage(pageable));

        SearchResponseDto result = searchService.search(query, page, size, true, true, false, false, null);

        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
    }

    @Test
    void searchWithFilterActive_authorsAreFromFilteredBooks_notNameSearch() {
        // When a filter is active, the author list is the authors of the filtered books,
        // NOT a name-based search for the query string.
        String query = "bible";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Author authorOfFilteredBook = new Author();
        authorOfFilteredBook.setId(10L);
        authorOfFilteredBook.setName("Augustine of Hippo"); // name does NOT contain "bible"
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(10L);
        authorDto.setName("Augustine of Hippo");

        when(bookRepository.findWithFilters(eq(query), eq(true), eq(false), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(emptyBookPage(pageable));
        when(authorRepository.findAuthorsOfBooksMatchingFilters(
                eq(query), eq(true), eq(false), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(authorOfFilteredBook), pageable, 1));
        when(authorMapper.toDto(authorOfFilteredBook)).thenReturn(authorDto);

        SearchResponseDto result = searchService.search(query, page, size, true, false, false, false, null);

        // The author whose name doesn't match the query is still returned because they
        // wrote a book that matches the filter.
        assertNotNull(result);
        assertEquals(1, result.getAuthors().size());
        assertEquals("Augustine of Hippo", result.getAuthors().get(0).getName());
    }

    // ── Pagination tests ──────────────────────────────────────────────────

    @Test
    void searchPaginationBehavior() {
        String query = "test";
        int page = 1, size = 10;
        Pageable pageable = PageRequest.of(page, size);

        List<Book> bookList = Arrays.asList(new Book(), new Book());
        Page<Book> bookPage = new PageImpl<>(bookList, pageable, 25); // 25 total → 3 pages

        List<Author> authorList = Arrays.asList(new Author());
        Page<Author> authorPage = new PageImpl<>(authorList, pageable, 11); // 11 total → 2 pages

        when(bookRepository.findWithFilters(eq(query), eq(false), eq(false), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(bookPage);
        when(authorRepository.findByNameContainingIgnoreCase(eq(query), any(Pageable.class)))
                .thenReturn(authorPage);
        when(bookMapper.toDto(any(Book.class))).thenReturn(new BookDto());
        when(authorMapper.toDto(any(Author.class))).thenReturn(new AuthorDto());

        SearchResponseDto result = searchService.search(query, page, size, false, false, false, false, null);

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

    // ── Labels tests ──────────────────────────────────────────────────────

    @Test
    void searchWithLabels_usesLabelFilteredRepository() {
        String query = "test";
        int page = 0, size = 20;
        List<String> labels = Arrays.asList("fiction", "fantasy");
        Pageable pageable = PageRequest.of(page, size);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Fiction Book");
        book.getTagsList().add("fiction");
        BookDto bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setTitle("Fiction Book");

        when(bookRepository.findWithFiltersAndLabels(
                eq(query), eq(false), eq(false), eq(false), eq(false),
                eq(labels), eq(2L), any(Pageable.class)))
                .thenReturn(bookPageOf(pageable, book));
        // Labels are active → uses EXISTS-based author query (not name search)
        when(authorRepository.findAuthorsOfBooksMatchingFiltersAndLabels(
                eq(query), eq(false), eq(false), eq(false), eq(false),
                eq(labels), eq(2L), any(Pageable.class)))
                .thenReturn(emptyAuthorPage(pageable));
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        SearchResponseDto result = searchService.search(query, page, size, false, false, false, false, labels);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("Fiction Book", result.getBooks().get(0).getTitle());
    }

    @Test
    void searchWithLabels_andInLibraryFilter_passesAllParamsToRepository() {
        String query = "test";
        int page = 0, size = 20;
        List<String> labels = Arrays.asList("theology");
        Pageable pageable = PageRequest.of(page, size);

        when(bookRepository.findWithFiltersAndLabels(
                eq(query), eq(true), eq(false), eq(false), eq(false),
                eq(labels), eq(1L), any(Pageable.class)))
                .thenReturn(emptyBookPage(pageable));
        // Labels+filter active → uses EXISTS-based author query (not name search)
        when(authorRepository.findAuthorsOfBooksMatchingFiltersAndLabels(
                eq(query), eq(true), eq(false), eq(false), eq(false),
                eq(labels), eq(1L), any(Pageable.class)))
                .thenReturn(emptyAuthorPage(pageable));

        SearchResponseDto result = searchService.search(query, page, size, true, false, false, false, labels);

        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
    }

    @Test
    void searchWithNullLabels_behavesLikeNoLabels() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        when(bookRepository.findWithFilters(eq(query), eq(false), eq(false), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(emptyBookPage(pageable));
        when(authorRepository.findByNameContainingIgnoreCase(eq(query), any(Pageable.class)))
                .thenReturn(emptyAuthorPage(pageable));

        SearchResponseDto result = searchService.search(query, page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
    }
}
