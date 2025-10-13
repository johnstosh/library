package com.muczynski.library.controller;

import com.muczynski.library.dto.LibraryDto;
import com.muczynski.library.service.LibraryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libraries")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<LibraryDto> createLibrary(@RequestBody LibraryDto libraryDto) {
        LibraryDto createdLibrary = libraryService.createLibrary(libraryDto);
        return new ResponseEntity<>(createdLibrary, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LibraryDto>> getAllLibraries() {
        List<LibraryDto> libraries = libraryService.getAllLibraries();
        return new ResponseEntity<>(libraries, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LibraryDto> getLibraryById(@PathVariable Long id) {
        LibraryDto library = libraryService.getLibraryById(id);
        return library != null ? new ResponseEntity<>(library, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}