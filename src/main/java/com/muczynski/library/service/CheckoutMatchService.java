/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.dto.CheckoutMatchDto;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.BookRepository.CheckoutMatchProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Checkout-form search. Returns at most {@link #MATCH_LIMIT} active books,
 * ranked the way the form used to rank them in the browser: a field that
 * contains the typed text scores 10, and an exact match scores 20 more.
 * A book is kept when any searched field matches. Fields shorter than
 * {@link #MIN_CHARS} are ignored, and a search with no usable field returns nothing.
 */
@Service
public class CheckoutMatchService {

    public static final int MATCH_LIMIT = 10;
    public static final int MIN_CHARS = 3;

    private final BookRepository bookRepository;

    public CheckoutMatchService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public List<CheckoutMatchDto> matches(String title, String author, String locNumber) {
        String titleTerm = normalize(title);
        String authorTerm = normalize(author);
        String typedLoc = normalize(locNumber);
        String locTerm = typedLoc.replaceAll("\\s+", "");
        int useTitle = titleTerm.length() >= MIN_CHARS ? 1 : 0;
        int useAuthor = authorTerm.length() >= MIN_CHARS ? 1 : 0;
        int useLoc = typedLoc.length() >= MIN_CHARS && !locTerm.isEmpty() ? 1 : 0;
        if (useTitle == 0 && useAuthor == 0 && useLoc == 0) {
            return List.of();
        }
        return bookRepository.findCheckoutMatches(
                        useTitle, useTitle == 1 ? titleTerm : "",
                        useAuthor, useAuthor == 1 ? authorTerm : "",
                        useLoc, useLoc == 1 ? locTerm : "",
                        MATCH_LIMIT)
                .stream()
                .map(CheckoutMatchService::toDto)
                .toList();
    }

    private static CheckoutMatchDto toDto(CheckoutMatchProjection row) {
        CheckoutMatchDto dto = new CheckoutMatchDto();
        dto.setId(row.getId());
        dto.setTitle(row.getTitle());
        dto.setAuthor(row.getAuthor());
        dto.setLocNumber(row.getLocNumber());
        dto.setStatus(row.getStatus());
        return dto;
    }

    /** Trimmed lowercase text. The character gate uses this length. */
    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
