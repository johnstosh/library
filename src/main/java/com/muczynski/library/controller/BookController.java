package com.muczynski.library.controller;

import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private PhotoService photoService;

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<BookDto> createBook(@RequestBody BookDto bookDto) {
        BookDto created = bookService.createBook(bookDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<BookDto>> getAllBooks() {
        List<BookDto> books = bookService.getAllBooks();
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBookById(@PathVariable Long id) {
        BookDto book = bookService.getBookById(id);
        return book != null ? ResponseEntity.ok(book) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<BookDto> updateBook(@PathVariable Long id, @RequestBody BookDto bookDto) {
        BookDto updated = bookService.updateBook(id, bookDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        try {
            bookService.deleteBook(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("associated loans")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        }
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> bulkImportBooks(@RequestBody List<BookDto> bookDtos) {
        bookService.bulkImportBooks(bookDtos);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{bookId}/photos")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<PhotoDto> addPhotoToBook(@PathVariable Long bookId, @RequestParam("file") MultipartFile file) {
        PhotoDto created = photoService.addPhoto(bookId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{bookId}/photos")
    public ResponseEntity<List<PhotoDto>> getPhotosByBook(@PathVariable Long bookId) {
        List<PhotoDto> photos = photoService.getPhotosByBookId(bookId);
        return ResponseEntity.ok(photos);
    }

    @PutMapping("/{bookId}/photos/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<PhotoDto> updatePhoto(@PathVariable Long bookId, @PathVariable Long photoId, @RequestBody PhotoDto photoDto) {
        PhotoDto updated = photoService.updatePhoto(photoId, photoDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{bookId}/photos/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long bookId, @PathVariable Long photoId) {
        photoService.deletePhoto(photoId);
        return ResponseEntity.noContent().build();
    }
}
