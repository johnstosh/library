package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.mapper.AuthorMapper;
import com.muczynski.library.repository.AuthorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorService {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorMapper authorMapper;

    public AuthorDto createAuthor(AuthorDto authorDto) {
        Author author = authorMapper.toEntity(authorDto);
        Author savedAuthor = authorRepository.save(author);
        return authorMapper.toDto(savedAuthor);
    }

    public List<AuthorDto> getAllAuthors() {
        return authorRepository.findAll().stream()
                .map(authorMapper::toDto)
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
        authorRepository.deleteById(id);
    }

    public void bulkImportAuthors(List<AuthorDto> authorDtos) {
        List<Author> authors = authorDtos.stream()
                .map(authorMapper::toEntity)
                .collect(Collectors.toList());
        authorRepository.saveAll(authors);
    }
}
