/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.PageInfoDto;
import com.muczynski.library.dto.SearchResponseDto;
import com.muczynski.library.mapper.AuthorMapper;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private AuthorMapper authorMapper;

    @Transactional(readOnly = true)
    public SearchResponseDto search(String query, int page, int size, String searchType) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage;
        switch (searchType) {
            case "ONLINE":
                bookPage = bookRepository.findByTitleContainingIgnoreCaseAndFreeTextUrlIsNotNull(query, pageable);
                break;
            case "IN_LIBRARY":
                bookPage = bookRepository.findByTitleContainingIgnoreCaseAndLocNumberIsNotNull(query, pageable);
                break;
            case "ALL":
            default:
                bookPage = bookRepository.findByTitleContainingIgnoreCase(query, pageable);
                break;
        }
        Page<Author> authorPage = authorRepository.findByNameContainingIgnoreCase(query, pageable);

        List<BookDto> books = bookPage.getContent().stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
        List<AuthorDto> authors = authorPage.getContent().stream()
                .map(authorMapper::toDto)
                .collect(Collectors.toList());

        PageInfoDto bookPageInfo = new PageInfoDto(
                bookPage.getTotalPages(),
                bookPage.getTotalElements(),
                bookPage.getNumber(),
                bookPage.getSize()
        );

        PageInfoDto authorPageInfo = new PageInfoDto(
                authorPage.getTotalPages(),
                authorPage.getTotalElements(),
                authorPage.getNumber(),
                authorPage.getSize()
        );

        return new SearchResponseDto(books, authors, bookPageInfo, authorPageInfo);
    }
}