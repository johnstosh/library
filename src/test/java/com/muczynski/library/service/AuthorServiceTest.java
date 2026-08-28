/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.AuthorEnrichmentResultDto;
import com.muczynski.library.dto.BulkDeleteResultDto;
import com.muczynski.library.mapper.AuthorMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private AuthorMapper authorMapper;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AskGrok askGrok;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuthorService authorService;

    @Test
    void createAuthor() {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setName("Test Author");
        authorDto.setGrokipediaUrl("https://grokipedia.example.com/author/1");
        Author author = new Author();
        author.setName("Test Author");
        author.setGrokipediaUrl("https://grokipedia.example.com/author/1");
        when(authorMapper.toEntity(authorDto)).thenReturn(author);
        when(authorRepository.save(author)).thenReturn(author);
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        AuthorDto result = authorService.createAuthor(authorDto);

        assertEquals(authorDto, result);
        verify(authorRepository).save(author);
    }

    @Test
    void getAllAuthors() {
        Author author = new Author();
        author.setId(1L);
        AuthorDto authorDto = new AuthorDto();
        when(authorRepository.findAll()).thenReturn(Collections.singletonList(author));
        when(authorMapper.toDto(any(Author.class))).thenReturn(authorDto);
        when(bookRepository.countByAuthorId(1L)).thenReturn(0L);

        assertEquals(1, authorService.getAllAuthors().size());
    }

    @Test
    void getAuthorById() {
        Author author = new Author();
        author.setId(1L);
        author.setGrokipediaUrl("https://grokipedia.example.com/author/1");
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setGrokipediaUrl("https://grokipedia.example.com/author/1");
        when(authorRepository.findByIdWithBooks(1L)).thenReturn(Optional.of(author));
        when(authorMapper.toDto(author, true)).thenReturn(authorDto);
        when(bookRepository.countByAuthorId(1L)).thenReturn(0L);

        assertEquals(authorDto, authorService.getAuthorById(1L));
    }

    @Test
    void updateAuthor() {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("Updated Author");
        authorDto.setGrokipediaUrl("https://grokipedia.example.com/author/1/updated");
        Author author = new Author();
        author.setId(1L);
        author.setName("Updated Author");
        author.setGrokipediaUrl("https://grokipedia.example.com/author/1/updated");
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorMapper.toEntity(authorDto)).thenReturn(author);
        when(authorRepository.save(author)).thenReturn(author);
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        AuthorDto result = authorService.updateAuthor(1L, authorDto);

        assertEquals(authorDto, result);
        verify(authorRepository).save(author);
    }

    @Test
    void getAuthorsWithoutGrokipedia() {
        Author authorWithUrl = new Author();
        authorWithUrl.setId(1L);
        authorWithUrl.setName("Author With Url");
        authorWithUrl.setGrokipediaUrl("https://grokipedia.example.com/author/1");

        Author authorWithoutUrl = new Author();
        authorWithoutUrl.setId(2L);
        authorWithoutUrl.setName("Author Without Url");
        authorWithoutUrl.setGrokipediaUrl(null);

        Author authorWithEmptyUrl = new Author();
        authorWithEmptyUrl.setId(3L);
        authorWithEmptyUrl.setName("Author Empty Url");
        authorWithEmptyUrl.setGrokipediaUrl("  ");

        when(authorRepository.findAll()).thenReturn(Arrays.asList(authorWithUrl, authorWithoutUrl, authorWithEmptyUrl));

        AuthorDto dtoWithoutUrl = new AuthorDto();
        dtoWithoutUrl.setId(2L);
        dtoWithoutUrl.setName("Author Without Url");

        AuthorDto dtoWithEmptyUrl = new AuthorDto();
        dtoWithEmptyUrl.setId(3L);
        dtoWithEmptyUrl.setName("Author Empty Url");

        when(authorMapper.toDto(authorWithoutUrl)).thenReturn(dtoWithoutUrl);
        when(authorMapper.toDto(authorWithEmptyUrl)).thenReturn(dtoWithEmptyUrl);
        when(bookRepository.countByAuthorId(2L)).thenReturn(3L);
        when(bookRepository.countByAuthorId(3L)).thenReturn(1L);

        List<AuthorDto> result = authorService.getAuthorsWithoutGrokipedia();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(a -> a.getId() == 2L));
        assertTrue(result.stream().anyMatch(a -> a.getId() == 3L));
    }

    @Test
    void generateMissingData_skipsWhenNothingMissing() {
        Author author = fullyPopulatedAuthor();
        AuthorDto dto = new AuthorDto();
        dto.setId(1L);
        dto.setName(author.getName());
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorMapper.toDto(author)).thenReturn(dto);
        when(bookRepository.countByAuthorId(1L)).thenReturn(2L);

        AuthorEnrichmentResultDto result = authorService.generateMissingData(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.isSkipped());
        assertTrue(result.getFilledFields().isEmpty());
        verifyNoInteractions(askGrok);
        verify(authorRepository, never()).save(any());
    }

    @Test
    void generateMissingData_fillsOnlyBlankFields() {
        Author author = new Author();
        author.setId(1L);
        author.setName("Jane Austen");
        author.setNationality("English");
        author.setBiographicalEssay("An existing biography that must not change.");

        AuthorDto dto = new AuthorDto();
        dto.setId(1L);
        dto.setName("Jane Austen");
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.findByAuthorIdOrderByTitleAsc(1L)).thenReturn(List.of());
        when(askGrok.askQuestion(anyString(), anyString())).thenReturn("""
                Here is the catalog card:
                {"dateOfBirth": "1775-12-16", "dateOfDeath": "1817-07-18",
                 "religiousAffiliation": "Anglican", "birthCountry": "England",
                 "nationality": "SHOULD NOT APPLY", "briefBiography": "SHOULD NOT APPLY"}
                """);
        when(authorRepository.save(author)).thenReturn(author);
        when(authorMapper.toDto(author)).thenReturn(dto);
        when(bookRepository.countByAuthorId(1L)).thenReturn(0L);

        AuthorEnrichmentResultDto result = authorService.generateMissingData(1L);

        assertTrue(result.isSuccess());
        assertFalse(result.isSkipped());
        assertEquals(List.of("dateOfBirth", "dateOfDeath", "religiousAffiliation", "birthCountry"), result.getFilledFields());
        assertEquals(LocalDate.of(1775, 12, 16), author.getDateOfBirth());
        assertEquals(LocalDate.of(1817, 7, 18), author.getDateOfDeath());
        assertEquals("Anglican", author.getReligiousAffiliation());
        assertEquals("England", author.getBirthCountry());
        assertEquals("English", author.getNationality());
        assertEquals("An existing biography that must not change.", author.getBiographicalEssay());
        verify(authorRepository).save(author);
    }

    @Test
    void generateMissingData_promptAsksForBiographicalEssay() {
        Author author = new Author();
        author.setId(1L);
        author.setName("Jane Austen");
        AuthorDto dto = new AuthorDto();
        dto.setId(1L);
        dto.setName("Jane Austen");
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.findByAuthorIdOrderByTitleAsc(1L)).thenReturn(List.of());
        when(askGrok.askQuestion(anyString(), anyString())).thenAnswer(invocation -> {
            String question = invocation.getArgument(0);
            assertTrue(question.contains("biographicalEssay"));
            assertFalse(question.contains("briefBiography"));
            return "{\"biographicalEssay\": \"A frank Catholic biography in several paragraphs.\"}";
        });
        when(authorRepository.save(author)).thenReturn(author);
        when(authorMapper.toDto(author)).thenReturn(dto);
        when(bookRepository.countByAuthorId(1L)).thenReturn(0L);

        AuthorEnrichmentResultDto result = authorService.generateMissingData(1L);

        assertTrue(result.isSuccess());
        assertEquals(List.of("biographicalEssay"), result.getFilledFields());
        assertEquals("A frank Catholic biography in several paragraphs.", author.getBiographicalEssay());
    }

    @Test
    void generateMissingData_promptDisambiguatesByBookTitles() {
        Author author = new Author();
        author.setId(1L);
        author.setName("John Smith");
        AuthorDto dto = new AuthorDto();
        dto.setId(1L);
        dto.setName("John Smith");
        Book book = new Book();
        book.setTitle("The Quiet Parish");
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.findByAuthorIdOrderByTitleAsc(1L)).thenReturn(List.of(book));
        when(askGrok.askQuestion(anyString(), anyString())).thenAnswer(invocation -> {
            String question = invocation.getArgument(0);
            assertTrue(question.contains("The Quiet Parish"));
            assertTrue(question.contains("specifically the author of"));
            assertTrue(question.contains("share this name"));
            return "{\"nationality\": \"English\"}";
        });
        when(authorRepository.save(author)).thenReturn(author);
        when(authorMapper.toDto(author)).thenReturn(dto);
        when(bookRepository.countByAuthorId(1L)).thenReturn(1L);

        AuthorEnrichmentResultDto result = authorService.generateMissingData(1L);

        assertTrue(result.isSuccess());
        assertEquals("English", author.getNationality());
    }

    @Test
    void generateMissingData_returnsFailureWhenGrokFails() {
        Author author = new Author();
        author.setId(1L);
        author.setName("Unknown Writer");
        AuthorDto dto = new AuthorDto();
        dto.setId(1L);
        dto.setName("Unknown Writer");
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.findByAuthorIdOrderByTitleAsc(1L)).thenReturn(List.of());
        when(askGrok.askQuestion(anyString(), anyString())).thenThrow(new RuntimeException("xAI timeout"));
        when(authorMapper.toDto(author)).thenReturn(dto);
        when(bookRepository.countByAuthorId(1L)).thenReturn(0L);

        AuthorEnrichmentResultDto result = authorService.generateMissingData(1L);

        assertFalse(result.isSuccess());
        assertFalse(result.isSkipped());
        assertEquals("xAI timeout", result.getErrorMessage());
        verify(authorRepository, never()).save(any());
    }

    @Test
    void generateMissingData_returnsFailureWhenJsonMissing() {
        Author author = new Author();
        author.setId(1L);
        author.setName("Unknown Writer");
        AuthorDto dto = new AuthorDto();
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.findByAuthorIdOrderByTitleAsc(1L)).thenReturn(List.of());
        when(askGrok.askQuestion(anyString(), anyString())).thenReturn("No JSON here");
        when(authorMapper.toDto(author)).thenReturn(dto);
        when(bookRepository.countByAuthorId(1L)).thenReturn(0L);

        AuthorEnrichmentResultDto result = authorService.generateMissingData(1L);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("No valid JSON"));
        verify(authorRepository, never()).save(any());
    }

    @Test
    void getAuthorAvailability_mapsPositiveCountsAndDropsAllFalse() {
        BookRepository.AuthorAvailabilityProjection withFlags = org.mockito.Mockito.mock(BookRepository.AuthorAvailabilityProjection.class);
        when(withFlags.getAuthorId()).thenReturn(1L);
        when(withFlags.getYdlPaperCount()).thenReturn(2L);
        when(withFlags.getYdlEbookCount()).thenReturn(0L);
        when(withFlags.getYdlAudioCount()).thenReturn(1L);
        when(withFlags.getEmuPaperCount()).thenReturn(null);
        when(withFlags.getEmuEbookCount()).thenReturn(3L);
        when(withFlags.getEmuAudioCount()).thenReturn(0L);

        BookRepository.AuthorAvailabilityProjection none = org.mockito.Mockito.mock(BookRepository.AuthorAvailabilityProjection.class);
        when(none.getAuthorId()).thenReturn(2L);
        when(none.getYdlPaperCount()).thenReturn(0L);
        when(none.getYdlEbookCount()).thenReturn(0L);
        when(none.getYdlAudioCount()).thenReturn(0L);
        when(none.getEmuPaperCount()).thenReturn(0L);
        when(none.getEmuEbookCount()).thenReturn(0L);
        when(none.getEmuAudioCount()).thenReturn(0L);

        when(bookRepository.countAvailabilityByAuthor()).thenReturn(List.of(withFlags, none));

        var result = authorService.getAuthorAvailability();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getAuthorId());
        assertTrue(result.get(0).getHasYdlBook());
        assertFalse(result.get(0).getHasYdlEbook());
        assertTrue(result.get(0).getHasYdlAudio());
        assertFalse(result.get(0).getHasEmuBook());
        assertTrue(result.get(0).getHasEmuEbook());
        assertFalse(result.get(0).getHasEmuAudio());
    }

    @Test
    void deleteBulkAuthors_deletesSomeAndRecordsFailures() {
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.countByAuthorId(1L)).thenReturn(0L);

        when(authorRepository.existsById(2L)).thenReturn(true);
        when(bookRepository.countByAuthorId(2L)).thenReturn(3L);
        Author withBooks = new Author();
        withBooks.setId(2L);
        withBooks.setName("Kept Author");
        when(authorRepository.findById(2L)).thenReturn(Optional.of(withBooks));

        BulkDeleteResultDto result = authorService.deleteBulkAuthors(List.of(1L, 2L));

        assertEquals(1, result.getDeletedCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(List.of(1L), result.getDeletedIds());
        assertEquals(2L, result.getFailures().get(0).getId());
        assertEquals("Kept Author", result.getFailures().get(0).getTitle());
        verify(authorRepository).deleteById(1L);
        verify(authorRepository, never()).deleteById(2L);
    }

    private Author fullyPopulatedAuthor() {
        Author author = new Author();
        author.setId(1L);
        author.setName("Complete Author");
        author.setDateOfBirth(LocalDate.of(1900, 1, 1));
        author.setDateOfDeath(LocalDate.of(1980, 1, 1));
        author.setReligiousAffiliation("Catholic");
        author.setBirthCountry("Ireland");
        author.setNationality("Irish");
        author.setBiographicalEssay("A full biography.");
        return author;
    }

}
