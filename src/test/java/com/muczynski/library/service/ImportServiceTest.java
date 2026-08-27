/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.BookAvailabilityStatsDto;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private ImportService importService;

    @Test
    void getAvailabilityStats_MapsRepositoryCountsToNamedFields() {
        when(bookRepository.countByElectronicResourceTrue()).thenReturn(3L);
        when(bookRepository.countWithCallNumber()).thenReturn(8L);
        when(bookRepository.countWithFreeOnlineText()).thenReturn(7L);
        when(bookRepository.countWithFreeOnlineAudio()).thenReturn(2L);
        when(bookRepository.countByStatus(BookStatus.WITHDRAWN)).thenReturn(1L);
        when(bookRepository.countAvailableAtYdl()).thenReturn(5L);
        when(bookRepository.countByYdlPaperAvailableTrue()).thenReturn(2L);
        when(bookRepository.countByYdlEbookAvailableTrue()).thenReturn(4L);
        when(bookRepository.countByYdlAudioAvailableTrue()).thenReturn(1L);
        when(bookRepository.countAvailableAtEmu()).thenReturn(6L);
        when(bookRepository.countByEmuPaperAvailableTrue()).thenReturn(3L);
        when(bookRepository.countByEmuEbookAvailableTrue()).thenReturn(2L);
        when(bookRepository.countByEmuAudioAvailableTrue()).thenReturn(1L);

        BookAvailabilityStatsDto stats = importService.getAvailabilityStats();

        assertEquals(3L, stats.getElectronicResource());
        assertEquals(8L, stats.getHasCallNumber());
        assertEquals(7L, stats.getHasFreeOnlineText());
        assertEquals(2L, stats.getHasFreeOnlineAudio());
        assertEquals(1L, stats.getWithdrawn());
        assertEquals(5L, stats.getAvailableAtYdl());
        assertEquals(2L, stats.getYdlPaper());
        assertEquals(4L, stats.getYdlEbook());
        assertEquals(1L, stats.getYdlAudio());
        assertEquals(6L, stats.getAvailableAtEmu());
        assertEquals(3L, stats.getEmuPaper());
        assertEquals(2L, stats.getEmuEbook());
        assertEquals(1L, stats.getEmuAudio());
        verify(bookRepository, never()).findAll();
    }
}
