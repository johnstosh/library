/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.Author;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.AuthorSummaryDto;
import com.muczynski.library.mapper.AuthorMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorService {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorMapper authorMapper;

    @Autowired
    private BookRepository bookRepository;

    public AuthorDto createAuthor(AuthorDto authorDto) {
        Author author = authorMapper.toEntity(authorDto);
        Author savedAuthor = authorRepository.save(author);
        return authorMapper.toDto(savedAuthor);
    }

    public List<AuthorDto> getAllAuthors() {
        return authorRepository.findAll().stream()
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    public AuthorDto getAuthorById(Long id) {
        return authorRepository.findByIdWithBooks(id)
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author, true);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .orElse(null);
    }

    public AuthorDto updateAuthor(Long id, AuthorDto authorDto) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new LibraryException("Author not found: " + id));
        Author updatedAuthor = authorMapper.toEntity(authorDto);
        updatedAuthor.setId(id);
        Author savedAuthor = authorRepository.save(updatedAuthor);
        return authorMapper.toDto(savedAuthor);
    }

    public void deleteAuthor(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new LibraryException("Author not found: " + id);
        }
        long bookCount = bookRepository.countByAuthorId(id);
        if (bookCount > 0) {
            throw new LibraryException("Cannot delete author because it has " + bookCount + " associated books.");
        }
        authorRepository.deleteById(id);
    }

    public void deleteBulkAuthors(List<Long> authorIds) {
        for (Long id : authorIds) {
            deleteAuthor(id);  // Reuse existing delete logic with book count validation
        }
    }

    public int deleteAuthorsWithNoBooks() {
        List<Author> allAuthors = authorRepository.findAll();
        int deletedCount = 0;

        for (Author author : allAuthors) {
            long bookCount = bookRepository.countByAuthorId(author.getId());
            if (bookCount == 0) {
                authorRepository.deleteById(author.getId());
                deletedCount++;
            }
        }

        return deletedCount;
    }

    /**
     * Find or create an author by name
     * @param name Author name
     * @return The existing or newly created author
     */
    public Author findOrCreateAuthor(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "John Doe";
        }

        Author existingAuthor = authorRepository.findByName(name);
        if (existingAuthor != null) {
            return existingAuthor;
        }

        // Create new author
        Author newAuthor = new Author();
        newAuthor.setName(name);
        return authorRepository.save(newAuthor);
    }

    /**
     * Get authors without a brief biography
     */
    public List<AuthorDto> getAuthorsWithoutDescription() {
        return authorRepository.findAll().stream()
                .filter(author -> author.getBriefBiography() == null || author.getBriefBiography().trim().isEmpty())
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    /**
     * Get authors with zero books
     */
    public List<AuthorDto> getAuthorsWithZeroBooks() {
        return authorRepository.findAll().stream()
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    long bookCount = bookRepository.countByAuthorId(author.getId());
                    dto.setBookCount(bookCount);
                    return dto;
                })
                .filter(dto -> dto.getBookCount() == 0)
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    /**
     * Get authors without a Grokipedia URL
     */
    public List<AuthorDto> getAuthorsWithoutGrokipedia() {
        return authorRepository.findAll().stream()
                .filter(author -> author.getGrokipediaUrl() == null || author.getGrokipediaUrl().trim().isEmpty())
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    /**
     * Get authors who have books added on the most recent day
     */
    public List<AuthorDto> getAuthorsFromMostRecentDay() {
        LocalDateTime maxDateTime = bookRepository.findMaxDateAddedToLibrary();
        if (maxDateTime == null) {
            return List.of();
        }
        // Get the date portion only (start of day)
        LocalDateTime startOfDay = maxDateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        // Get all books from the most recent day
        List<Long> authorIds = bookRepository.findByDateAddedToLibraryBetweenOrderByDateAddedDesc(startOfDay, endOfDay).stream()
                .map(book -> book.getAuthor() != null ? book.getAuthor().getId() : null)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // Get authors for these IDs
        return authorRepository.findAllById(authorIds).stream()
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    /**
     * Get all author summaries (id + lastModified) for caching.
     */
    public List<AuthorSummaryDto> getAllAuthorSummaries() {
        return authorRepository.findAll().stream()
                .map(author -> {
                    AuthorSummaryDto dto = new AuthorSummaryDto();
                    dto.setId(author.getId());
                    dto.setLastModified(author.getLastModified());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get authors by IDs for batch fetching.
     */
    public List<AuthorDto> getAuthorsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return authorRepository.findAllById(ids).stream()
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

}
