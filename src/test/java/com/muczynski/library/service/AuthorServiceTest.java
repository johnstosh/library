package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.mapper.AuthorMapper;
import com.muczynski.library.repository.AuthorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private AuthorMapper authorMapper;

    @InjectMocks
    private AuthorService authorService;

    @Test
    void createAuthor() {
        AuthorDto authorDto = new AuthorDto();
        Author author = new Author();
        when(authorMapper.toEntity(authorDto)).thenReturn(author);
        when(authorRepository.save(author)).thenReturn(author);
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        AuthorDto result = authorService.createAuthor(authorDto);

        assertEquals(authorDto, result);
        verify(authorRepository).save(author);
    }

    @Test
    void getAllAuthors() {
        when(authorRepository.findAll()).thenReturn(Collections.singletonList(new Author()));
        when(authorMapper.toDto(any(Author.class))).thenReturn(new AuthorDto());

        assertEquals(1, authorService.getAllAuthors().size());
    }

    @Test
    void getAuthorById() {
        Author author = new Author();
        AuthorDto authorDto = new AuthorDto();
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorMapper.toDto(author)).thenReturn(authorDto);

        assertEquals(authorDto, authorService.getAuthorById(1L));
    }

    @Test
    void bulkImportAuthors() {
        authorService.bulkImportAuthors(Collections.singletonList(new AuthorDto()));
        verify(authorRepository).saveAll(any());
    }
}