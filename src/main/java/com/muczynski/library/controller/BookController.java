/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.User;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.BookSummaryDto;
import com.muczynski.library.dto.PhotoAddFromGooglePhotosResponse;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.AskGrok;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.GooglePhotosService;
import com.muczynski.library.service.PhotoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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

    @Autowired
    private GooglePhotosService googlePhotosService;

    @Autowired
    private AskGrok askGrok;

    @Autowired
    private UserRepository userRepository;

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

    @GetMapping("/without-loc")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getBooksWithoutLocNumber() {
        try {
            List<BookDto> books = bookService.getBooksWithoutLocNumber();
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books without LOC number: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/most-recent-day")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getBooksFromMostRecentDay() {
        try {
            List<BookDto> books = bookService.getBooksFromMostRecentDay();
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books from most recent day: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/by-3letter-loc")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getBooksWith3LetterLocStart() {
        try {
            List<BookDto> books = bookService.getBooksWith3LetterLocStart();
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books with 3-letter LOC start: {}", e.getMessage(), e);
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

    @PostMapping("/delete-bulk")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> deleteBulkBooks(@RequestBody List<Long> bookIds) {
        try {
            bookService.deleteBulkBooks(bookIds);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to bulk delete books {}: {}", bookIds, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> cloneBook(@PathVariable Long id) {
        try {
            BookDto cloned = bookService.cloneBook(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
        } catch (Exception e) {
            logger.warn("Failed to clone book ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/{bookId}/photos")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<PhotoDto> addPhotoToBook(@PathVariable Long bookId, @RequestParam("file") MultipartFile file) {
        try {
            PhotoDto created = photoService.addPhoto(bookId, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.warn("Failed to add photo to book ID {} with file {}: {}", bookId, file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{bookId}/photos/from-google-photos")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<PhotoAddFromGooglePhotosResponse> addPhotosFromGooglePhotos(@PathVariable Long bookId, @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> photos = (List<Map<String, Object>>) request.get("photos");

            if (photos == null || photos.isEmpty()) {
                PhotoAddFromGooglePhotosResponse response = new PhotoAddFromGooglePhotosResponse();
                response.setSavedCount(0);
                response.setFailedCount(0);
                response.setSavedPhotos(Collections.emptyList());
                response.setFailedPhotos(Collections.singletonList(Map.of("error", "No photos provided")));
                return ResponseEntity.badRequest().body(response);
            }

            // Get valid access token for downloading photos
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // The principal name is the database user ID (not username)
            Long userId = Long.parseLong(authentication.getName());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new LibraryException("User not found"));
            String accessToken = googlePhotosService.getValidAccessToken(user);

            List<PhotoDto> savedPhotos = new ArrayList<>();
            List<Map<String, Object>> failedPhotos = new ArrayList<>();

            for (Map<String, Object> photo : photos) {
                String permanentId = (String) photo.get("id");
                String baseUrl = (String) photo.get("url");
                String mimeType = (String) photo.get("mimeType");

                if (mimeType == null || mimeType.trim().isEmpty()) {
                    mimeType = "image/jpeg";
                }

                try {
                    // Download photo bytes from Google Photos
                    byte[] photoBytes = googlePhotosService.downloadPhotoFromUrl(baseUrl, accessToken);

                    // Save to database with permanent ID
                    PhotoDto savedPhoto = photoService.addPhotoFromGooglePhotos(bookId, photoBytes, mimeType, permanentId);
                    savedPhotos.add(savedPhoto);
                    logger.info("Added photo from Google Photos to book {}: permanentId={}", bookId, permanentId);

                } catch (Exception e) {
                    logger.error("Failed to add photo {} to book {}: {}", permanentId, bookId, e.getMessage());
                    failedPhotos.add(Map.of(
                            "id", permanentId,
                            "error", e.getMessage()
                    ));
                }
            }

            PhotoAddFromGooglePhotosResponse response = new PhotoAddFromGooglePhotosResponse();
            response.setSavedCount(savedPhotos.size());
            response.setFailedCount(failedPhotos.size());
            response.setSavedPhotos(savedPhotos);
            response.setFailedPhotos(failedPhotos);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Failed to add photos from Google Photos to book {}: {}", bookId, e.getMessage(), e);
            PhotoAddFromGooglePhotosResponse response = new PhotoAddFromGooglePhotosResponse();
            response.setSavedCount(0);
            response.setFailedCount(0);
            response.setSavedPhotos(Collections.emptyList());
            response.setFailedPhotos(Collections.singletonList(Map.of("error", e.getMessage())));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{bookId}/photos")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<PhotoDto>> getPhotosByBook(@PathVariable Long bookId) {
        try {
            List<PhotoDto> photos = photoService.getPhotosByBookId(bookId);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            logger.warn("Failed to retrieve photos for book ID {}: {}", bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{bookId}/photos/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<PhotoDto> updatePhoto(@PathVariable Long bookId, @PathVariable Long photoId, @RequestBody PhotoDto photoDto) {
        try {
            PhotoDto updated = photoService.updatePhoto(photoId, photoDto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.warn("Failed to update photo ID {} for book ID {} with DTO {}: {}", photoId, bookId, photoDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{bookId}/photos/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long bookId, @PathVariable Long photoId) {
        try {
            photoService.deletePhoto(photoId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.warn("Failed to delete photo ID {} for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{bookId}/photos/{photoId}/rotate-cw")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> rotatePhotoCW(@PathVariable Long bookId, @PathVariable Long photoId) {
        try {
            photoService.rotatePhoto(photoId, true);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to rotate photo ID {} clockwise for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{bookId}/photos/{photoId}/rotate-ccw")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> rotatePhotoCCW(@PathVariable Long bookId, @PathVariable Long photoId) {
        try {
            photoService.rotatePhoto(photoId, false);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to rotate photo ID {} counter-clockwise for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/book-by-photo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<BookDto> generateBookByPhoto(@PathVariable Long id) {
        try {
            BookDto updated = bookService.generateTempBook(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.warn("Failed to generate book by photo for ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/title-author-from-photo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<BookDto> getTitleAuthorFromPhoto(@PathVariable Long id) {
        try {
            BookDto updated = bookService.getTitleAuthorFromPhoto(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.warn("Failed to extract title and author from photo for book ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/book-from-title-author")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<BookDto> getBookFromTitleAuthor(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String authorName = request.get("authorName");
            BookDto updated = bookService.getBookFromTitleAuthor(id, title, authorName);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.warn("Failed to generate book metadata from title and author for book ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{bookId}/photos/{photoId}/move-left")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> movePhotoLeft(@PathVariable Long bookId, @PathVariable Long photoId) {
        try {
            photoService.movePhotoLeft(bookId, photoId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to move photo ID {} left for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{bookId}/photos/{photoId}/move-right")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> movePhotoRight(@PathVariable Long bookId, @PathVariable Long photoId) {
        try {
            photoService.movePhotoRight(bookId, photoId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to move photo ID {} right for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/summaries")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAllBookSummaries() {
        try {
            List<BookSummaryDto> summaries = bookService.getAllBookSummaries();
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve book summaries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/by-ids")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getBooksByIds(@RequestBody List<Long> ids) {
        try {
            List<BookDto> books = bookService.getBooksByIds(ids);
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books by IDs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/suggest-loc")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> suggestLocNumber(@RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String author = request.get("author");

            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));
            }

            String suggestion = askGrok.suggestLocNumber(title, author);
            return ResponseEntity.ok(Map.of("suggestion", suggestion));
        } catch (Exception e) {
            logger.warn("Failed to get LOC suggestion for title '{}': {}",
                    request.get("title"), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
