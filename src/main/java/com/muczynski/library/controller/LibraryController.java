package com.muczynski.library.controller;

import com.muczynski.library.dto.LibraryDto;
import com.muczynski.library.service.LibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libraries")
public class LibraryController {

    private static final Logger logger = LoggerFactory.getLogger(LibraryController.class);

    @Autowired
    private LibraryService libraryService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAllLibraries() {
        try {
            List<LibraryDto> libraries = libraryService.getAllLibraries();
            return ResponseEntity.ok(libraries);
        } catch (Exception e) {
            logger.debug("Failed to retrieve all libraries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getLibraryById(@PathVariable Long id) {
        try {
            LibraryDto library = libraryService.getLibraryById(id);
            return library != null ? ResponseEntity.ok(library) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.debug("Failed to retrieve library by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> createLibrary(@RequestBody LibraryDto libraryDto) {
        try {
            LibraryDto created = libraryService.createLibrary(libraryDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.debug("Failed to create library with DTO {}: {}", libraryDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateLibrary(@PathVariable Long id, @RequestBody LibraryDto libraryDto) {
        try {
            LibraryDto updated = libraryService.updateLibrary(id, libraryDto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.debug("Failed to update library ID {} with DTO {}: {}", id, libraryDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> deleteLibrary(@PathVariable Long id) {
        try {
            libraryService.deleteLibrary(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.debug("Failed to delete library ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
