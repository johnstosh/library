/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.CatalogToolCandidatesDto;
import com.muczynski.library.dto.CatalogToolPreviewDto;
import com.muczynski.library.freetext.FreeTextLookupService;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.CatalogMaintenanceActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogToolFreeTextSearchTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private CatalogMaintenanceActionRepository actionRepository;
    @Mock
    private GrokipediaLookupService grokipediaLookupService;
    @Mock
    private FreeTextLookupService freeTextLookupService;

    private CatalogToolMaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new CatalogToolMaintenanceService(
                bookRepository,
                authorRepository,
                actionRepository,
                grokipediaLookupService,
                freeTextLookupService,
                new ObjectMapper());
    }

    @Test
    void candidateIdsKeepRepositoryOrderAndDropBlankAndTemporaryTitles() {
        List<BookRepository.FreeTextCandidateRow> rows = new ArrayList<>();
        rows.add(candidate(1L, "2024-01-02 photo.jpg", null));
        rows.add(candidate(2L, "   ", null));
        for (int i = 0; i < 101; i++) {
            rows.add(candidate(1000L + i, "Title " + i, null));
        }
        when(bookRepository.findFreeTextCandidates()).thenReturn(rows);

        CatalogToolCandidatesDto dto = service.freeTextCandidateIds(100);

        assertEquals(100, dto.getBookIds().size());
        assertEquals(1000L, dto.getBookIds().get(0));
        assertEquals(1099L, dto.getBookIds().get(99));
        assertFalse(dto.getBookIds().contains(1L));
        assertFalse(dto.getBookIds().contains(2L));
        assertFalse(dto.getBookIds().contains(1100L));
    }

    @Test
    void candidateIdsRejectsALimitOutsideTheFourSizes() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.freeTextCandidateIds(50));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Search 100, 200, 400, or 800 books.", ex.getReason());
        verify(bookRepository, never()).findFreeTextCandidates();
    }

    @Test
    void searchStoresANewLinkAndKeepsTheOldOne() {
        when(bookRepository.findFreeTextSearchRows(List.of(9L))).thenReturn(List.of(
                row(9L, "The Hobbit", List.of("There and Back Again"), "https://a.example/old",
                        "J. R. R. Tolkien", List.of("Tolkien"))));
        when(freeTextLookupService.discoverUrls(any(), any()))
                .thenReturn(new FreeTextLookupService.Discovery(
                        "https://b.example/new", "Provider", List.of("Provider")));

        CatalogToolPreviewDto result = service.searchFreeText(List.of(9L));

        assertEquals(1, result.getChangeCount());
        assertEquals("https://b.example/new", result.getChanges().get(0).getAdded());
        assertEquals("https://a.example/old", result.getChanges().get(0).getBefore());
        verify(freeTextLookupService).discoverUrls(
                eq(List.of("The Hobbit", "There and Back Again")),
                eq(List.of("J. R. R. Tolkien", "Tolkien")));
        verify(freeTextLookupService).recordFreeTextSearch(
                9L, "https://a.example/old https://b.example/new", true);
        verify(actionRepository, never()).save(any());
    }

    @Test
    void searchStampsAMissWithoutChangingTheUrl() {
        when(bookRepository.findFreeTextSearchRows(List.of(9L))).thenReturn(List.of(
                row(9L, "The Hobbit", List.of(), "https://a.example/old", "J. R. R. Tolkien", List.of())));
        when(freeTextLookupService.discoverUrls(any(), any()))
                .thenReturn(new FreeTextLookupService.Discovery(null, null, List.of("Provider")));

        CatalogToolPreviewDto result = service.searchFreeText(List.of(9L));

        assertTrue(result.getChanges().isEmpty());
        verify(freeTextLookupService).recordFreeTextSearch(eq(9L), isNull(), eq(false));
    }

    @Test
    void searchSkipsATemporaryTitleWithoutStampingIt() {
        when(bookRepository.findFreeTextSearchRows(List.of(4L))).thenReturn(List.of(
                row(4L, "2024-01-02 photo.jpg", List.of("Real Title"), null, "Author", List.of())));

        CatalogToolPreviewDto result = service.searchFreeText(List.of(4L));

        assertTrue(result.getChanges().isEmpty());
        assertTrue(result.getSkipped().isEmpty());
        verify(freeTextLookupService, never()).discoverUrls(any(), any());
        verify(freeTextLookupService, never()).recordFreeTextSearch(anyLong(), nullable(String.class), anyBoolean());
    }

    @Test
    void searchStampsAFailedBookAndReportsItSkipped() {
        when(bookRepository.findFreeTextSearchRows(List.of(9L))).thenReturn(List.of(
                row(9L, "The Hobbit", List.of(), null, "Author", List.of())));
        when(freeTextLookupService.discoverUrls(any(), any())).thenThrow(new RuntimeException("down"));

        CatalogToolPreviewDto result = service.searchFreeText(List.of(9L));

        assertTrue(result.getChanges().isEmpty());
        assertEquals(1, result.getSkipped().size());
        assertTrue(result.getSkipped().get(0).getReason().contains("down"));
        verify(freeTextLookupService).recordFreeTextSearch(eq(9L), isNull(), eq(false));
    }

    private static BookRepository.FreeTextCandidateRow candidate(Long id, String title, LocalDateTime searched) {
        return new BookRepository.FreeTextCandidateRow() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public LocalDateTime getFreeTextLastSearched() {
                return searched;
            }
        };
    }

    private static BookRepository.FreeTextSearchRow row(Long id, String title, List<String> alternateTitles,
            String freeTextUrl, String authorName, List<String> authorAlternateNames) {
        return new BookRepository.FreeTextSearchRow() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public List<String> getAlternateTitles() {
                return alternateTitles;
            }

            @Override
            public String getFreeTextUrl() {
                return freeTextUrl;
            }

            @Override
            public String getAuthorName() {
                return authorName;
            }

            @Override
            public List<String> getAuthorAlternateNames() {
                return authorAlternateNames;
            }
        };
    }
}
