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

    /**
     * Search books and authors with OR-combined type filters.
     *
     * @param query          title search text (empty = match all)
     * @param page           zero-based page number
     * @param size           results per page
     * @param filterInLibrary include books with a LOC call number (physical collection)
     * @param filterElectronic include books with electronicResource = true
     * @param filterFreeText  include books with a free online text URL
     * @param filterAudio     include books whose free text URL contains "librivox"
     * @param labels          label tags that books must ALL have (null/empty = no label filter)
     */
    @Transactional(readOnly = true)
    public SearchResponseDto search(String query, int page, int size,
            boolean filterInLibrary, boolean filterElectronic,
            boolean filterFreeText, boolean filterAudio,
            List<String> labels) {

        String trimmedQuery = (query == null) ? "" : query.trim();
        Pageable pageable = PageRequest.of(page, size);
        boolean hasLabels = labels != null && !labels.isEmpty();
        long labelCount = hasLabels ? labels.size() : 0;

        Page<Book> bookPage;
        if (hasLabels) {
            bookPage = bookRepository.findWithFiltersAndLabels(
                    trimmedQuery, filterInLibrary, filterElectronic, filterFreeText, filterAudio,
                    labels, labelCount, pageable);
        } else {
            bookPage = bookRepository.findWithFilters(
                    trimmedQuery, filterInLibrary, filterElectronic, filterFreeText, filterAudio,
                    pageable);
        }

        Page<Author> authorPage;
        if (!trimmedQuery.isEmpty()) {
            authorPage = authorRepository.findByNameContainingIgnoreCase(trimmedQuery, pageable);
        } else {
            authorPage = authorRepository.findAll(pageable);
        }

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
                bookPage.getSize());

        PageInfoDto authorPageInfo = new PageInfoDto(
                authorPage.getTotalPages(),
                authorPage.getTotalElements(),
                authorPage.getNumber(),
                authorPage.getSize());

        return new SearchResponseDto(books, authors, bookPageInfo, authorPageInfo);
    }
}
