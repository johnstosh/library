/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.dto.AuthorNameMatchDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.AuthorRepository.AuthorNameMatchProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Book-form author search. Same rules as {@link CheckoutMatchService}:
 * nothing is queried until {@link #MIN_CHARS} characters, and at most
 * {@link #MATCH_LIMIT} rows come back. A name that contains the typed text
 * scores 10, and an exact canonical or alternate name scores 20 more.
 */
@Service
public class AuthorNameMatchService {

    /** Same character gate as {@link CheckoutMatchService#MIN_CHARS}. */
    public static final int MIN_CHARS = CheckoutMatchService.MIN_CHARS;

    /** Same result cap as {@link CheckoutMatchService#MATCH_LIMIT}. */
    public static final int MATCH_LIMIT = CheckoutMatchService.MATCH_LIMIT;

    private final AuthorRepository authorRepository;

    public AuthorNameMatchService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional(readOnly = true)
    public List<AuthorNameMatchDto> matches(String name) {
        String term = normalize(name);
        if (term.length() < MIN_CHARS) {
            return List.of();
        }
        return authorRepository.findNameMatches(term, MATCH_LIMIT).stream()
                .map(AuthorNameMatchService::toDto)
                .toList();
    }

    private static AuthorNameMatchDto toDto(AuthorNameMatchProjection row) {
        AuthorNameMatchDto dto = new AuthorNameMatchDto();
        dto.setId(row.getId());
        dto.setName(row.getName());
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
