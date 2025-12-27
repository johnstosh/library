/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.LibraryDto;
import com.muczynski.library.dto.LibraryStatisticsDto;
import com.muczynski.library.mapper.LibraryMapper;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LibraryRepository;
import com.muczynski.library.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LibraryService {

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMapper libraryMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

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
        Library library = libraryRepository.findById(id).orElseThrow(() -> new LibraryException("Library not found: " + id));
        Library updatedLibrary = libraryMapper.toEntity(libraryDto);
        updatedLibrary.setId(id);
        Library savedLibrary = libraryRepository.save(updatedLibrary);
        return libraryMapper.toDto(savedLibrary);
    }

    public void deleteLibrary(Long id) {
        if (!libraryRepository.existsById(id)) {
            throw new LibraryException("Library not found: " + id);
        }
        libraryRepository.deleteById(id);
    }

    /**
     * Get the default library, creating it if it doesn't exist
     * This ensures there's always at least one library available
     */
    public Library getOrCreateDefaultLibrary() {
        List<Library> libraries = libraryRepository.findAll();
        if (!libraries.isEmpty()) {
            return libraries.get(0);
        }

        // Create default library
        Library library = new Library();
        library.setName("St. Martin de Porres");
        library.setHostname("library.muczynskifamily.com");
        return libraryRepository.save(library);
    }

    /**
     * Get statistics for all libraries
     */
    public List<LibraryStatisticsDto> getLibraryStatistics() {
        List<Library> libraries = libraryRepository.findAll();
        List<LibraryStatisticsDto> statistics = new ArrayList<>();

        for (Library library : libraries) {
            Long bookCount = bookRepository.countByLibraryId(library.getId());
            Long activeLoansCount = loanRepository.countByBookLibraryIdAndReturnDateIsNull(library.getId());

            LibraryStatisticsDto stats = new LibraryStatisticsDto(
                library.getId(),
                library.getName(),
                bookCount,
                activeLoansCount
            );
            statistics.add(stats);
        }

        return statistics;
    }
}
