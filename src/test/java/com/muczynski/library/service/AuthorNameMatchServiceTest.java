/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.repository.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthorNameMatchServiceTest {

    private AuthorRepository authorRepository;
    private AuthorNameMatchService service;

    @BeforeEach
    void setUp() {
        authorRepository = mock(AuthorRepository.class);
        service = new AuthorNameMatchService(authorRepository);
    }

    @Test
    void fewerThanThreeCharactersDoesNotQuery() {
        assertTrue(service.matches(null).isEmpty());
        assertTrue(service.matches("  au ").isEmpty());
        verifyNoInteractions(authorRepository);
    }

    @Test
    void threeCharactersQueryTheFoldedNameWithTheCheckoutLimit() {
        when(authorRepository.findNameMatches("aug", AuthorNameMatchService.MATCH_LIMIT))
                .thenReturn(List.of());

        assertTrue(service.matches(" Aug ").isEmpty());

        verify(authorRepository).findNameMatches("aug", AuthorNameMatchService.MATCH_LIMIT);
    }
}
