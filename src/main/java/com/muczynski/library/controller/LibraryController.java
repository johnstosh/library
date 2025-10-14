package com.muczynski.library.controller;

import com.muczynski.library.dto.LibraryDto;
import com.muczynski.library.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libraries")
public class LibraryController {

    @Autowired
    private LibraryService libraryService;

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<LibraryDto> createLibrary(@RequestBody LibraryDto libraryDto) {
        LibraryDto created = libraryService.createLibrary(libraryDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<LibraryDto>> getAllLibraries() {
        List<LibraryDto> libraries = libraryService.getAllLibraries();
        return ResponseEntity.ok(libraries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LibraryDto> getLibraryById(@PathVariable Long id) {
        LibraryDto library = libraryService.getLibraryById(id);
        return library != null ? ResponseEntity.ok(library) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<LibraryDto> updateLibrary(@PathVariable Long id, @RequestBody LibraryDto libraryDto) {
        LibraryDto updated = libraryService.updateLibrary(id, libraryDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> deleteLibrary(@PathVariable Long id) {
        libraryService.deleteLibrary(id);
        return ResponseEntity.noContent().build();
    }
}
