/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Favorite;
import com.muczynski.library.domain.FavoriteItemType;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.FavoriteItemDto;
import com.muczynski.library.dto.FavoriteSummaryDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(bookRepository.existsById(9L)).thenReturn(true);
        when(bookRepository.getReferenceById(9L)).thenReturn(book);
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
        verify(bookRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void replaceItem_RejectsLibrarianListsForPatrons() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(bookRepository.existsById(9L)).thenReturn(true);
        when(bookRepository.getReferenceById(9L)).thenReturn(book);

        FavoriteUpdateDto update = new FavoriteUpdateDto(
                FavoriteItemType.BOOK, 9L, List.of("Needs Review"));

        assertThrows(LibraryException.class, () -> favoriteService.replaceItem(1L, update, false));
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void getItem_OmitsLibrarianListsForPatrons() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.existsById(9L)).thenReturn(true);
        when(favoriteRepository.findListNamesByUserIdAndBookId(1L, 9L)).thenReturn(List.of());
        when(favoriteRepository.findDistinctListNamesByUserId(1L)).thenReturn(List.of("Needs Review", "Mine"));

        FavoriteItemDto result = favoriteService.getItem(1L, FavoriteItemType.BOOK, 9L, false);

        assertEquals(List.of("Have Read", "Want to Read", "Want to Recommend", "Mine"), result.getAvailableLists());
        verify(bookRepository, never()).findById(any());
        verify(favoriteRepository, never()).findByUser_IdAndBook_Id(any(), any());
    }

    @Test
    void getItem_ReturnsSelectedListNamesWithoutLoadingFavoriteEntities() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.existsById(9L)).thenReturn(true);
        when(favoriteRepository.findListNamesByUserIdAndBookId(1L, 9L)).thenReturn(List.of("Have Read", "Shelf"));
        when(favoriteRepository.findDistinctListNamesByUserId(1L)).thenReturn(List.of("Have Read", "Shelf"));

        FavoriteItemDto result = favoriteService.getItem(1L, FavoriteItemType.BOOK, 9L, true);

        assertEquals(List.of("Have Read", "Shelf"), result.getSelectedLists());
        assertTrue(result.getAvailableLists().contains("Needs Review"));
        verify(bookRepository, never()).findById(any());
    }

    @Test
    void getSummary_UsesMembershipProjectionAndIncludesAvailableLists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(favoriteRepository.findMembershipRowsByUserId(1L)).thenReturn(List.of(
                new Object[]{"Have Read", 9L, null},
                new Object[]{"Shelf", 9L, null},
                new Object[]{"Have Read", null, 4L}
        ));

        FavoriteSummaryDto result = favoriteService.getSummary(1L, false);

        assertEquals(List.of(9L), result.getFavoriteBookIds());
        assertEquals(List.of(4L), result.getFavoriteAuthorIds());
        assertEquals("Have Read", result.getLists().get(0).getListName());
        assertEquals(List.of(9L), result.getLists().get(0).getBookIds());
        assertEquals(List.of(4L), result.getLists().get(0).getAuthorIds());
        assertEquals("Shelf", result.getLists().get(1).getListName());
        assertTrue(result.getAvailableLists().contains("Want to Read"));
        assertTrue(result.getAvailableLists().contains("Shelf"));
        assertFalse(result.getAvailableLists().contains("Needs Review"));
        verify(favoriteRepository, never()).findByUser_Id(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getSummary_IncludesLibrarianListsForLibrarians() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(favoriteRepository.findMembershipRowsByUserId(1L)).thenReturn(List.of());

        FavoriteSummaryDto result = favoriteService.getSummary(1L, true);

        assertTrue(result.getAvailableLists().contains("Needs Review"));
        assertTrue(result.getAvailableLists().contains("Need to Locate"));
    }

    @Test
    void resolveIds_UsesMembershipProjection() {
        when(favoriteRepository.findMembershipRowsByUserId(1L)).thenReturn(List.of(
                new Object[]{"Have Read", 9L, null},
                new Object[]{"Shelf", 11L, null},
                new Object[]{"Have Read", null, 4L}
        ));

        FavoriteService.FavoriteIdSets ids = favoriteService.resolveIds(1L, List.of("Have Read"));

        assertEquals(List.of(9L), ids.bookIds());
        assertEquals(List.of(4L), ids.authorIds());
        verify(favoriteRepository, never()).findByUser_Id(any());
    }
}
