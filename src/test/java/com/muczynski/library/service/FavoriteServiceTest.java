/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Favorite;
import com.muczynski.library.domain.FavoriteItemType;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.FavoriteItemDto;
import com.muczynski.library.dto.FavoriteUpdateDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.FavoriteRepository;
import com.muczynski.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        book = new Book();
        book.setId(9L);
    }

    @Test
    void replaceItem_SavesCheckedListsAndReturnsLibrarianLists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(9L)).thenReturn(Optional.of(book));
        when(favoriteRepository.findDistinctListNamesByUserId(1L)).thenReturn(List.of("Have Read", "Shelf"));

        FavoriteUpdateDto update = new FavoriteUpdateDto(
                FavoriteItemType.BOOK, 9L, List.of("Have Read", "Needs Review", "Shelf"));

        FavoriteItemDto result = favoriteService.replaceItem(1L, update, true);

        verify(favoriteRepository).deleteByUser_IdAndBook_Id(1L, 9L);
        ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertEquals(List.of("Have Read", "Needs Review", "Shelf"), result.getSelectedLists());
        assertTrue(result.getAvailableLists().contains("Need to Locate"));
        assertTrue(result.getAvailableLists().contains("Shelf"));
    }

    @Test
    void replaceItem_RejectsLibrarianListsForPatrons() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(9L)).thenReturn(Optional.of(book));

        FavoriteUpdateDto update = new FavoriteUpdateDto(
                FavoriteItemType.BOOK, 9L, List.of("Needs Review"));

        assertThrows(LibraryException.class, () -> favoriteService.replaceItem(1L, update, false));
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void getItem_OmitsLibrarianListsForPatrons() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(9L)).thenReturn(Optional.of(book));
        when(favoriteRepository.findByUser_IdAndBook_Id(1L, 9L)).thenReturn(List.of());
        when(favoriteRepository.findDistinctListNamesByUserId(1L)).thenReturn(List.of("Needs Review", "Mine"));

        FavoriteItemDto result = favoriteService.getItem(1L, FavoriteItemType.BOOK, 9L, false);

        assertEquals(List.of("Have Read", "Want to Read", "Want to Recommend", "Mine"), result.getAvailableLists());
    }
}
