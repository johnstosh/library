// (c) Copyright 2025 by Muczynski
package com.muczynski.library.service;

import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.LibraryDto;
import com.muczynski.library.mapper.LibraryMapper;
import com.muczynski.library.repository.LibraryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LibraryService {

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMapper libraryMapper;

    public LibraryDto createLibrary(LibraryDto libraryDto) {
        Library library = libraryMapper.toEntity(libraryDto);
        Library savedLibrary = libraryRepository.save(library);
        return libraryMapper.toDto(savedLibrary);
    }

    public List<LibraryDto> getAllLibraries() {
        return libraryRepository.findAll().stream()
                .map(libraryMapper::toDto)
                .collect(Collectors.toList());
    }

    public LibraryDto getLibraryById(Long id) {
        return libraryRepository.findById(id)
                .map(libraryMapper::toDto)
                .orElse(null);
    }

    public LibraryDto updateLibrary(Long id, LibraryDto libraryDto) {
        Library library = libraryRepository.findById(id).orElseThrow(() -> new RuntimeException("Library not found: " + id));
        Library updatedLibrary = libraryMapper.toEntity(libraryDto);
        updatedLibrary.setId(id);
        Library savedLibrary = libraryRepository.save(updatedLibrary);
        return libraryMapper.toDto(savedLibrary);
    }

    public void deleteLibrary(Long id) {
        if (!libraryRepository.existsById(id)) {
            throw new RuntimeException("Library not found: " + id);
        }
        libraryRepository.deleteById(id);
    }
}
