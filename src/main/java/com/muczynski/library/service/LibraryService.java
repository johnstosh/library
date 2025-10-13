package com.muczynski.library.service;

import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.LibraryDto;
import com.muczynski.library.mapper.LibraryMapper;
import com.muczynski.library.repository.LibraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LibraryService {

    private final LibraryRepository libraryRepository;
    private final LibraryMapper libraryMapper;

    public LibraryService(LibraryRepository libraryRepository, LibraryMapper libraryMapper) {
        this.libraryRepository = libraryRepository;
        this.libraryMapper = libraryMapper;
    }

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
}