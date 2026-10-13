/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatusFilter;
import com.muczynski.library.domain.ReadingDifficulty;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.SearchResponseDto;
import com.muczynski.library.mapper.AuthorMapper;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.CatalogCriteria;
import com.muczynski.library.service.CatalogFilterService.BookCatalogFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CatalogCriteria catalogCriteria;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private AuthorMapper authorMapper;

    @Mock
    private FavoriteService favoriteService;

    @InjectMocks
    private SearchService searchService;

    private Page<Book> bookPageOf(Pageable pageable, Book... books) {
        return new PageImpl<>(Arrays.asList(books), pageable, books.length);
    }

    private Page<Book> emptyBookPage(Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    private Page<Author> emptyAuthorPage(Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    private SearchResponseDto search(String query, int page, int size,
            boolean inLib, boolean elec, boolean freeText, boolean audio, List<String> labels) {
        return searchService.search(query, page, page, size, inLib, elec, freeText, audio,
                false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false, false, labels, null, null, null, null);
    }

    private void stubBooks(Page<Book> bookPage) {
        when(catalogCriteria.searchBooks(any(), any(Pageable.class))).thenReturn(bookPage);
    }

    private void stubAuthorsByName(String query, Page<Author> authorPage) {
        when(catalogCriteria.searchAuthorsByName(eq(query), any(Pageable.class))).thenReturn(authorPage);
    }

    private void stubAuthorsOfBooks(Page<Author> authorPage) {
        when(catalogCriteria.searchAuthorsOfMatchingBooks(any(), any(Pageable.class))).thenReturn(authorPage);
    }

    private BookCatalogFilter capturedBookFilter() {
        ArgumentCaptor<BookCatalogFilter> captor = ArgumentCaptor.forClass(BookCatalogFilter.class);
        verify(catalogCriteria).searchBooks(captor.capture(), any(Pageable.class));
        return captor.getValue();
    }

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

        stubBooks(bookPageOf(pageable, book));
        stubAuthorsByName(query, new PageImpl<>(Arrays.asList(author), pageable, 1));
        when(bookMapper.toDto(book)).thenReturn(bookDto);
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        SearchResponseDto result = search(query, page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals(1, result.getAuthors().size());
        assertEquals(bookDto, result.getBooks().get(0));
        assertEquals(authorDto, result.getAuthors().get(0));
        assertEquals(1, result.getBookPage().getTotalPages());
        assertEquals(1, result.getBookPage().getTotalElements());
        assertEquals(0, result.getBookPage().getCurrentPage());
        assertEquals(20, result.getBookPage().getPageSize());
        BookCatalogFilter filter = capturedBookFilter();
        assertEquals(query, filter.query);
        assertTrue(filter.statuses.isEmpty());
        assertTrue(filter.labels.isEmpty());
        verify(catalogCriteria).searchAuthorsByName(eq(query), any(Pageable.class));
        verify(catalogCriteria, never()).searchAuthorsOfMatchingBooks(any(), any());
    }

    @Test
    void searchWithNoResultsReturnsEmpty() {
        String query = "nonexistent";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        stubBooks(emptyBookPage(pageable));
        stubAuthorsByName(query, emptyAuthorPage(pageable));

        SearchResponseDto result = search(query, page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
        assertEquals(0, result.getAuthors().size());
        assertEquals(0, result.getBookPage().getTotalElements());
        assertEquals(0, result.getAuthorPage().getTotalElements());
        verify(catalogCriteria).searchAuthorsByName(eq(query), any(Pageable.class));
    }

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

        stubBooks(bookPageOf(pageable, book));
        when(authorRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(author), pageable, 1));
        when(bookMapper.toDto(book)).thenReturn(bookDto);
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        SearchResponseDto result = search("", page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals(1, result.getAuthors().size());
        verify(authorRepository).findAll(any(Pageable.class));
        verify(catalogCriteria, never()).searchAuthorsByName(any(), any());
        verify(catalogCriteria, never()).searchAuthorsOfMatchingBooks(any(), any());
    }

    @Test
    void searchWithNullQueryReturnsAllBooks() {
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        stubBooks(emptyBookPage(pageable));
        when(authorRepository.findAll(any(Pageable.class))).thenReturn(emptyAuthorPage(pageable));

        SearchResponseDto result = search(null, page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
        assertEquals(0, result.getAuthors().size());
        assertEquals("", capturedBookFilter().query);
        verify(authorRepository).findAll(any(Pageable.class));
    }

    @Test
    void searchWithWhitespaceQueryTreatsAsEmpty() {
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        stubBooks(emptyBookPage(pageable));
        when(authorRepository.findAll(any(Pageable.class))).thenReturn(emptyAuthorPage(pageable));

        SearchResponseDto result = search("   ", page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals("", capturedBookFilter().query);
        verify(authorRepository).findAll(any(Pageable.class));
        verify(catalogCriteria, never()).searchAuthorsByName(any(), any());
    }

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

        stubBooks(bookPageOf(pageable, book));
        stubAuthorsOfBooks(new PageImpl<>(Arrays.asList(author), pageable, 1));
        when(bookMapper.toDto(book)).thenReturn(bookDto);
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        SearchResponseDto result = search(query, page, size, true, false, false, false, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("Library Book", result.getBooks().get(0).getTitle());
        assertEquals(1, result.getAuthors().size());
        assertEquals("A Library Author", result.getAuthors().get(0).getName());
        assertEquals(List.of(BookStatusFilter.IN_LIBRARY), capturedBookFilter().statuses);
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
        verify(catalogCriteria, never()).searchAuthorsByName(any(), any());
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

        stubBooks(bookPageOf(pageable, book));
        stubAuthorsOfBooks(emptyAuthorPage(pageable));
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        SearchResponseDto result = search(query, page, size, false, true, false, false, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("Electronic Book", result.getBooks().get(0).getTitle());
        assertEquals(List.of(BookStatusFilter.ELECTRONIC_RESOURCE), capturedBookFilter().statuses);
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
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

        stubBooks(bookPageOf(pageable, book));
        stubAuthorsOfBooks(emptyAuthorPage(pageable));
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        SearchResponseDto result = search(query, page, size, false, false, true, false, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("Free Text Book", result.getBooks().get(0).getTitle());
        BookCatalogFilter filter = capturedBookFilter();
        assertTrue(filter.freeText);
        assertFalse(filter.audio);
        assertTrue(filter.statuses.isEmpty());
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
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

        stubBooks(bookPageOf(pageable, book));
        stubAuthorsOfBooks(emptyAuthorPage(pageable));
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        SearchResponseDto result = search(query, page, size, false, false, false, true, null);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("LibriVox Book", result.getBooks().get(0).getTitle());
        assertTrue(capturedBookFilter().audio);
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
    }

    @Test
    void searchWithMultipleFiltersPassesAllTrueToRepository() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        stubBooks(emptyBookPage(pageable));
        stubAuthorsOfBooks(emptyAuthorPage(pageable));

        SearchResponseDto result = search(query, page, size, true, true, false, false, null);

        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
        assertEquals(List.of(BookStatusFilter.IN_LIBRARY, BookStatusFilter.ELECTRONIC_RESOURCE),
                capturedBookFilter().statuses);
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
    }

    @Test
    void searchWithFilterActive_authorsAreFromFilteredBooks_notNameSearch() {
        String query = "bible";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        Author authorOfFilteredBook = new Author();
        authorOfFilteredBook.setId(10L);
        authorOfFilteredBook.setName("Augustine of Hippo");
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(10L);
        authorDto.setName("Augustine of Hippo");

        stubBooks(emptyBookPage(pageable));
        stubAuthorsOfBooks(new PageImpl<>(Arrays.asList(authorOfFilteredBook), pageable, 1));
        when(authorMapper.toDto(authorOfFilteredBook)).thenReturn(authorDto);

        SearchResponseDto result = search(query, page, size, true, false, false, false, null);

        assertNotNull(result);
        assertEquals(1, result.getAuthors().size());
        assertEquals("Augustine of Hippo", result.getAuthors().get(0).getName());
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
        verify(catalogCriteria, never()).searchAuthorsByName(any(), any());
    }

    @Test
    void searchPaginationBehavior() {
        String query = "test";
        int page = 1, size = 10;
        Pageable pageable = PageRequest.of(page, size);

        List<Book> bookList = Arrays.asList(new Book(), new Book());
        Page<Book> bookPage = new PageImpl<>(bookList, pageable, 25);

        List<Author> authorList = Arrays.asList(new Author());
        Page<Author> authorPage = new PageImpl<>(authorList, pageable, 11);

        stubBooks(bookPage);
        stubAuthorsByName(query, authorPage);
        when(bookMapper.toDto(any(Book.class))).thenReturn(new BookDto());
        when(authorMapper.toDto(any(Author.class))).thenReturn(new AuthorDto());

        SearchResponseDto result = search(query, page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals(3, result.getBookPage().getTotalPages());
        assertEquals(25, result.getBookPage().getTotalElements());
        assertEquals(1, result.getBookPage().getCurrentPage());
        assertEquals(10, result.getBookPage().getPageSize());

        assertEquals(2, result.getAuthorPage().getTotalPages());
        assertEquals(11, result.getAuthorPage().getTotalElements());
        assertEquals(1, result.getAuthorPage().getCurrentPage());
        assertEquals(10, result.getAuthorPage().getPageSize());
        verify(catalogCriteria).searchAuthorsByName(eq(query), any(Pageable.class));
    }

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

        stubBooks(bookPageOf(pageable, book));
        stubAuthorsOfBooks(emptyAuthorPage(pageable));
        when(bookMapper.toDto(book)).thenReturn(bookDto);

        SearchResponseDto result = search(query, page, size, false, false, false, false, labels);

        assertNotNull(result);
        assertEquals(1, result.getBooks().size());
        assertEquals("Fiction Book", result.getBooks().get(0).getTitle());
        BookCatalogFilter filter = capturedBookFilter();
        assertEquals(labels, filter.labels);
        assertTrue(filter.statuses.isEmpty());
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
        verify(catalogCriteria, never()).searchAuthorsByName(any(), any());
    }

    @Test
    void searchWithLabels_andInLibraryFilter_passesAllParamsToRepository() {
        String query = "test";
        int page = 0, size = 20;
        List<String> labels = Arrays.asList("theology");
        Pageable pageable = PageRequest.of(page, size);

        stubBooks(emptyBookPage(pageable));
        stubAuthorsOfBooks(emptyAuthorPage(pageable));

        SearchResponseDto result = search(query, page, size, true, false, false, false, labels);

        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
        BookCatalogFilter filter = capturedBookFilter();
        assertEquals(labels, filter.labels);
        assertEquals(List.of(BookStatusFilter.IN_LIBRARY), filter.statuses);
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
    }

    @Test
    void searchWithNullLabels_behavesLikeNoLabels() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        stubBooks(emptyBookPage(pageable));
        stubAuthorsByName(query, emptyAuthorPage(pageable));

        SearchResponseDto result = search(query, page, size, false, false, false, false, null);

        assertNotNull(result);
        assertEquals(0, result.getBooks().size());
        assertTrue(capturedBookFilter().labels.isEmpty());
        verify(catalogCriteria).searchAuthorsByName(eq(query), any(Pageable.class));
        verify(catalogCriteria, never()).searchAuthorsOfMatchingBooks(any(), any());
    }

    @Test
    void searchWithNotActiveStatusOff_passesFalseToRepository() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        stubBooks(emptyBookPage(pageable));
        stubAuthorsByName(query, emptyAuthorPage(pageable));

        searchService.search(query, page, page, size, false, false, false, false,
                false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false, false, null, null, null, null, null);

        BookCatalogFilter filter = capturedBookFilter();
        assertTrue(filter.statuses.isEmpty());
        assertFalse(filter.freeText);
        assertFalse(filter.audio);
        assertFalse(filter.mostRecent);
        verify(catalogCriteria).searchAuthorsByName(eq(query), any(Pageable.class));
        verify(catalogCriteria, never()).searchAuthorsOfMatchingBooks(any(), any());
    }

    @Test
    void searchWithNotActiveStatusOn_passesTrueAndUsesAuthorFilterQuery() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        stubBooks(emptyBookPage(pageable));
        stubAuthorsOfBooks(emptyAuthorPage(pageable));

        SearchResponseDto result = searchService.search(query, page, page, size, false, false, false, false,
                false, false, false, false, false, true, false,
                false, false, false, false, false, false, false, false, false, false, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(List.of(
                BookStatusFilter.LOST,
                BookStatusFilter.WITHDRAWN,
                BookStatusFilter.ON_ORDER,
                BookStatusFilter.REQUESTED), capturedBookFilter().statuses);
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
        verify(catalogCriteria, never()).searchAuthorsByName(any(), any());
    }

    @Test
    void searchWithMostRecentFilter_computesUtcCutoffFromMaxDate() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime max = LocalDateTime.of(2026, 8, 26, 15, 30);
        LocalDateTime expectedCutoff = LocalDateTime.of(2026, 8, 25, 0, 0);
        List<Long> tempIds = List.of(99L);

        when(bookRepository.findMaxDateAddedToLibrary()).thenReturn(max);
        when(bookRepository.findBookIdsWithTemporaryTitles()).thenReturn(tempIds);
        stubBooks(emptyBookPage(pageable));
        stubAuthorsOfBooks(emptyAuthorPage(pageable));

        SearchResponseDto result = searchService.search(query, page, page, size, false, false, false, false,
                true, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false, false, null, null, null, null, null);

        assertNotNull(result);
        BookCatalogFilter filter = capturedBookFilter();
        assertTrue(filter.mostRecent);
        assertEquals(expectedCutoff, filter.mostRecentCutoff);
        assertEquals(tempIds, filter.mostRecentTempTitleIds);
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
    }

    @Test
    void searchWithWithoutLocFilter_passesTrueToRepository() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);

        stubBooks(emptyBookPage(pageable));
        stubAuthorsOfBooks(emptyAuthorPage(pageable));

        SearchResponseDto result = searchService.search(query, page, page, size, false, false, false, false,
                false, true, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false, false, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(List.of(BookStatusFilter.WITHOUT_LOC), capturedBookFilter().statuses);
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
    }

    @Test
    void searchWithReadingDifficulty_passesOrListAndSwitchesAuthorsToMatchingBooks() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);
        List<ReadingDifficulty> difficulties = List.of(ReadingDifficulty.CHILDREN, ReadingDifficulty.UNSET);

        stubBooks(emptyBookPage(pageable));
        stubAuthorsOfBooks(emptyAuthorPage(pageable));

        SearchResponseDto result = searchService.search(query, page, page, size, false, false, false, false,
                false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false, false, null, null, difficulties, null, null);

        assertNotNull(result);
        assertEquals(difficulties, capturedBookFilter().readingDifficulties);
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
        verify(catalogCriteria, never()).searchAuthorsByName(any(), any());
    }

    @Test
    void searchWithStatusFilters_orsSelectedValuesAndSwitchesAuthorsToMatchingBooks() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);
        List<BookStatusFilter> statuses = List.of(
                BookStatusFilter.IN_LIBRARY, BookStatusFilter.REQUESTED);

        stubBooks(emptyBookPage(pageable));
        stubAuthorsOfBooks(emptyAuthorPage(pageable));

        SearchResponseDto result = searchService.search(query, page, page, size, false, false, false, false,
                false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false, false, null, statuses, null, null, null);

        assertNotNull(result);
        assertEquals(statuses, capturedBookFilter().statuses);
        verify(catalogCriteria).searchAuthorsOfMatchingBooks(any(), any());
        verify(catalogCriteria, never()).searchAuthorsByName(any(), any());
    }

    @Test
    void searchWithOnlyFavoriteLists_usesAuthorIdQueryNotMatchingBooks() {
        String query = "test";
        int page = 0, size = 20;
        Pageable pageable = PageRequest.of(page, size);
        when(favoriteService.resolveIds(7L, List.of("mine")))
                .thenReturn(new FavoriteService.FavoriteIdSets(List.of(3L), List.of(9L)));
        stubBooks(emptyBookPage(pageable));
        when(catalogCriteria.searchAuthorsByIdsAndName(eq(List.of(9L)), eq(query), any(Pageable.class)))
                .thenReturn(emptyAuthorPage(pageable));

        searchService.search(query, page, page, size, false, false, false, false,
                false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false, false,
                null, null, null, 7L, List.of("mine"));

        BookCatalogFilter filter = capturedBookFilter();
        assertTrue(filter.favoriteBooks);
        assertEquals(List.of(3L), filter.favoriteBookIds);
        assertTrue(filter.favoriteAuthors);
        assertEquals(List.of(9L), filter.favoriteAuthorIds);
        verify(catalogCriteria).searchAuthorsByIdsAndName(eq(List.of(9L)), eq(query), any(Pageable.class));
        verify(catalogCriteria, never()).searchAuthorsOfMatchingBooks(any(), any());
        verify(catalogCriteria, never()).searchAuthorsByName(any(), any());
        verify(authorRepository, never()).findAll(any(Pageable.class));
    }
}
