/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.BookDto;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Map<String, Object> search(String query, int page, int size) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findByTitleContainingIgnoreCase(query, pageable);
        Page<Author> authorPage = authorRepository.findByNameContainingIgnoreCase(query, pageable);

        List<BookDto> books = bookPage.getContent().stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
        List<AuthorDto> authors = authorPage.getContent().stream()
                .map(authorMapper::toDto)
                .collect(Collectors.toList());

        Map<String, Object> results = new HashMap<>();
        results.put("books", books);
        results.put("authors", authors);

        Map<String, Object> bookPageInfo = new HashMap<>();
        bookPageInfo.put("totalPages", bookPage.getTotalPages());
        bookPageInfo.put("totalElements", bookPage.getTotalElements());
        bookPageInfo.put("currentPage", bookPage.getNumber());
        bookPageInfo.put("pageSize", bookPage.getSize());
        results.put("bookPage", bookPageInfo);

        Map<String, Object> authorPageInfo = new HashMap<>();
        authorPageInfo.put("totalPages", authorPage.getTotalPages());
        authorPageInfo.put("totalElements", authorPage.getTotalElements());
        authorPageInfo.put("currentPage", authorPage.getNumber());
        authorPageInfo.put("pageSize", authorPage.getSize());
        results.put("authorPage", authorPageInfo);

        return results;
    }
}