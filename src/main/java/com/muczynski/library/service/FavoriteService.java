/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Favorite;
import com.muczynski.library.domain.FavoriteItemType;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.FavoriteItemDto;
import com.muczynski.library.dto.FavoriteListCountDto;
import com.muczynski.library.dto.FavoriteListMembershipDto;
import com.muczynski.library.dto.FavoriteSummaryDto;
import com.muczynski.library.dto.FavoriteUpdateDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.exception.ResourceNotFoundException;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.FavoriteRepository;
import com.muczynski.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    public static final List<String> PATRON_LISTS = List.of(
            "Have Read",
            "Want to Read",
            "Want to Recommend"
    );

    public static final List<String> LIBRARIAN_LISTS = List.of(
            "Needs Review",
            "Need to Locate"
    );

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public record FavoriteIdSets(List<Long> bookIds, List<Long> authorIds) {
    }

    @Transactional(readOnly = true)
    public FavoriteSummaryDto getSummary(Long userId) {
        requireUser(userId);
        Set<Long> bookIds = new LinkedHashSet<>();
        Set<Long> authorIds = new LinkedHashSet<>();
        Map<String, FavoriteListMembershipDto> byName = new LinkedHashMap<>();
        for (Favorite favorite : favoriteRepository.findByUser_Id(userId)) {
            FavoriteListMembershipDto row = byName.computeIfAbsent(
                    favorite.getListName(),
                    name -> new FavoriteListMembershipDto(name, new ArrayList<>(), new ArrayList<>()));
            if (favorite.getBook() != null) {
                bookIds.add(favorite.getBook().getId());
                if (!row.getBookIds().contains(favorite.getBook().getId())) {
                    row.getBookIds().add(favorite.getBook().getId());
                }
            }
            if (favorite.getAuthor() != null) {
                authorIds.add(favorite.getAuthor().getId());
                if (!row.getAuthorIds().contains(favorite.getAuthor().getId())) {
                    row.getAuthorIds().add(favorite.getAuthor().getId());
                }
            }
        }
        List<FavoriteListMembershipDto> lists = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String name : builtInListOrder()) {
            FavoriteListMembershipDto row = byName.get(name);
            if (row != null) {
                lists.add(row);
                seen.add(name.toLowerCase(Locale.ROOT));
            }
        }
        byName.values().stream()
                .filter(row -> !seen.contains(row.getListName().toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(FavoriteListMembershipDto::getListName, String.CASE_INSENSITIVE_ORDER))
                .forEach(lists::add);
        return new FavoriteSummaryDto(new ArrayList<>(bookIds), new ArrayList<>(authorIds), lists);
    }

    @Transactional(readOnly = true)
    public FavoriteIdSets resolveIds(Long userId, List<String> listNames) {
        Set<String> wanted = listNames == null ? Set.of() : listNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<Long> bookIds = new LinkedHashSet<>();
        Set<Long> authorIds = new LinkedHashSet<>();
        if (wanted.isEmpty()) {
            return new FavoriteIdSets(List.of(), List.of());
        }
        for (Favorite favorite : favoriteRepository.findByUser_Id(userId)) {
            if (favorite.getListName() == null
                    || !wanted.contains(favorite.getListName().toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (favorite.getBook() != null) {
                bookIds.add(favorite.getBook().getId());
            }
            if (favorite.getAuthor() != null) {
                authorIds.add(favorite.getAuthor().getId());
            }
        }
        return new FavoriteIdSets(new ArrayList<>(bookIds), new ArrayList<>(authorIds));
    }

    private List<String> builtInListOrder() {
        List<String> names = new ArrayList<>(PATRON_LISTS);
        names.addAll(LIBRARIAN_LISTS);
        return names;
    }

    @Transactional(readOnly = true)
    public FavoriteItemDto getItem(Long userId, FavoriteItemType itemType, Long itemId, boolean librarian) {
        requireUser(userId);
        requireItem(itemType, itemId);
        List<String> selected = selectedListNames(userId, itemType, itemId);
        return new FavoriteItemDto(itemType, itemId, selected, availableLists(userId, librarian));
    }

    public FavoriteItemDto replaceItem(Long userId, FavoriteUpdateDto update, boolean librarian) {
        if (update == null || update.getItemType() == null || update.getItemId() == null) {
            throw new LibraryException("itemType and itemId are required");
        }
        User user = requireUser(userId);
        FavoriteItemType itemType = update.getItemType();
        Long itemId = update.getItemId();
        Book book = itemType == FavoriteItemType.BOOK ? requireBook(itemId) : null;
        Author author = itemType == FavoriteItemType.AUTHOR ? requireAuthor(itemId) : null;

        List<String> listNames = normalizeListNames(update.getListNames(), librarian);

        if (itemType == FavoriteItemType.BOOK) {
            favoriteRepository.deleteByUser_IdAndBook_Id(userId, itemId);
        } else {
            favoriteRepository.deleteByUser_IdAndAuthor_Id(userId, itemId);
        }
        favoriteRepository.flush();

        for (String listName : listNames) {
            Favorite favorite = new Favorite();
            favorite.setUser(user);
            favorite.setBook(book);
            favorite.setAuthor(author);
            favorite.setListName(listName);
            favoriteRepository.save(favorite);
        }

        return new FavoriteItemDto(itemType, itemId, listNames, availableLists(userId, librarian));
    }

    @Transactional(readOnly = true)
    public List<FavoriteListCountDto> getListCounts() {
        Map<String, FavoriteListCountDto> byName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Object[] row : favoriteRepository.countBooksGroupedByListName()) {
            String name = (String) row[0];
            long count = ((Number) row[1]).longValue();
            byName.computeIfAbsent(name, n -> new FavoriteListCountDto(n, 0, 0)).setBookCount(count);
        }
        for (Object[] row : favoriteRepository.countAuthorsGroupedByListName()) {
            String name = (String) row[0];
            long count = ((Number) row[1]).longValue();
            FavoriteListCountDto dto = byName.computeIfAbsent(name, n -> new FavoriteListCountDto(n, 0, 0));
            dto.setListName(name);
            dto.setAuthorCount(count);
        }
        return byName.values().stream()
                .sorted(Comparator
                        .comparingLong((FavoriteListCountDto d) -> d.getBookCount() + d.getAuthorCount())
                        .reversed()
                        .thenComparing(FavoriteListCountDto::getListName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<String> availableLists(Long userId, boolean librarian) {
        LinkedHashSet<String> lists = new LinkedHashSet<>(PATRON_LISTS);
        if (librarian) {
            lists.addAll(LIBRARIAN_LISTS);
        }
        Set<String> builtInLower = lists.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Set<String> librarianLower = LIBRARIAN_LISTS.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<String> custom = favoriteRepository.findDistinctListNamesByUserId(userId).stream()
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> !builtInLower.contains(name.toLowerCase(Locale.ROOT)))
                .filter(name -> librarian || !librarianLower.contains(name.toLowerCase(Locale.ROOT)))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        lists.addAll(custom);
        return new ArrayList<>(lists);
    }

    public static boolean isLibrarianOnlyList(String listName) {
        if (listName == null) {
            return false;
        }
        String lower = listName.toLowerCase(Locale.ROOT);
        return LIBRARIAN_LISTS.stream().anyMatch(name -> name.toLowerCase(Locale.ROOT).equals(lower));
    }

    private List<String> selectedListNames(Long userId, FavoriteItemType itemType, Long itemId) {
        List<Favorite> rows = itemType == FavoriteItemType.BOOK
                ? favoriteRepository.findByUser_IdAndBook_Id(userId, itemId)
                : favoriteRepository.findByUser_IdAndAuthor_Id(userId, itemId);
        return rows.stream().map(Favorite::getListName).collect(Collectors.toList());
    }

    private List<String> normalizeListNames(List<String> raw, boolean librarian) {
        if (raw == null) {
            return List.of();
        }
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        for (String name : raw) {
            if (name == null) {
                continue;
            }
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!librarian && isLibrarianOnlyList(trimmed)) {
                throw new LibraryException("List '" + trimmed + "' is only available to librarians");
            }
            unique.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
        }
        return new ArrayList<>(unique.values());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private Book requireBook(Long itemId) {
        return bookRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", itemId));
    }

    private Author requireAuthor(Long itemId) {
        return authorRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", itemId));
    }

    private void requireItem(FavoriteItemType itemType, Long itemId) {
        if (itemType == null || itemId == null) {
            throw new LibraryException("itemType and itemId are required");
        }
        if (itemType == FavoriteItemType.BOOK) {
            requireBook(itemId);
        } else {
            requireAuthor(itemId);
        }
    }
}
