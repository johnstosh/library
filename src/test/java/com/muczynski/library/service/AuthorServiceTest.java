/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.mapper.AuthorMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
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

    @Mock
    private BookRepository bookRepository;

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

}
