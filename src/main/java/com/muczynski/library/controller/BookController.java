/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.PhotoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    @Autowired
    private BookService bookService;

    @Autowired
    private PhotoService photoService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAllBooks() {
        try {
            List<BookDto> books = bookService.getAllBooks();
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            logger.warn("Failed to retrieve all books: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getBookById(@PathVariable Long id) {
        try {
            BookDto book = bookService.getBookById(id);
            return book != null ? ResponseEntity.ok(book) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.warn("Failed to retrieve book by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> createBook(@Valid @RequestBody BookDto bookDto) {
        try {
            BookDto created = bookService.createBook(bookDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.warn("Failed to create book with DTO {}: {}", bookDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateBook(@PathVariable Long id, @Valid @RequestBody BookDto bookDto) {
        try {
            BookDto updated = bookService.updateBook(id, bookDto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.warn("Failed to update book ID {} with DTO {}: {}", id, bookDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        try {
            bookService.deleteBook(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.warn("Failed to delete book ID {}: {}", id, e.getMessage(), e);
            if (e.getMessage().contains("checked out")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Collections.singletonMap("message", e.getMessage()));
            }
            throw e;
        }
    }

    @PostMapping("/{bookId}/photos")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> addPhotoToBook(@PathVariable Long bookId, @RequestParam("file") MultipartFile file) {
        try {
            PhotoDto created = photoService.addPhoto(bookId, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.warn("Failed to add photo to book ID {} with file {}: {}", bookId, file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{bookId}/photos")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getPhotosByBook(@PathVariable Long bookId) {
        try {
            List<PhotoDto> photos = photoService.getPhotosByBookId(bookId);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            logger.warn("Failed to retrieve photos for book ID {}: {}", bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{bookId}/photos/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updatePhoto(@PathVariable Long bookId, @PathVariable Long photoId, @RequestBody PhotoDto photoDto) {
        try {
            PhotoDto updated = photoService.updatePhoto(photoId, photoDto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.warn("Failed to update photo ID {} for book ID {} with DTO {}: {}", photoId, bookId, photoDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{bookId}/photos/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> deletePhoto(@PathVariable Long bookId, @PathVariable Long photoId) {
        try {
            photoService.deletePhoto(photoId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.warn("Failed to delete photo ID {} for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{bookId}/photos/{photoId}/rotate-cw")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> rotatePhotoCW(@PathVariable Long bookId, @PathVariable Long photoId) {
        try {
            photoService.rotatePhoto(photoId, true);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to rotate photo ID {} clockwise for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{bookId}/photos/{photoId}/rotate-ccw")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> rotatePhotoCCW(@PathVariable Long bookId, @PathVariable Long photoId) {
        try {
            photoService.rotatePhoto(photoId, false);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to rotate photo ID {} counter-clockwise for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/book-by-photo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> generateBookByPhoto(@PathVariable Long id) {
        try {
            BookDto updated = bookService.generateTempBook(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.warn("Failed to generate book by photo for ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{bookId}/photos/{photoId}/move-left")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> movePhotoLeft(@PathVariable Long bookId, @PathVariable Long photoId) {
        try {
            photoService.movePhotoLeft(bookId, photoId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to move photo ID {} left for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{bookId}/photos/{photoId}/move-right")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> movePhotoRight(@PathVariable Long bookId, @PathVariable Long photoId) {
        try {
            photoService.movePhotoRight(bookId, photoId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to move photo ID {} right for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
