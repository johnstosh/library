// (c) Copyright 2025 by Muczynski
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.mapper.AuthorMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .map(authorMapper::toDto)
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
        return authorRepository.findById(id)
                .map(authorMapper::toDto)
                .orElse(null);
    }

    public AuthorDto updateAuthor(Long id, AuthorDto authorDto) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new RuntimeException("Author not found: " + id));
        Author updatedAuthor = authorMapper.toEntity(authorDto);
        updatedAuthor.setId(id);
        Author savedAuthor = authorRepository.save(updatedAuthor);
        return authorMapper.toDto(savedAuthor);
    }

    public void deleteAuthor(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new RuntimeException("Author not found: " + id);
        }
        long bookCount = bookRepository.countByAuthorId(id);
        if (bookCount > 0) {
            throw new RuntimeException("Cannot delete author because it has " + bookCount + " associated books.");
        }
        authorRepository.deleteById(id);
    }

}
