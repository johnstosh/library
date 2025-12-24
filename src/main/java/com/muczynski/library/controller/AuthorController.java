/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.service.AuthorService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private static final Logger logger = LoggerFactory.getLogger(AuthorController.class);

    @Autowired
    private AuthorService authorService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private BookService bookService;

    @Autowired
    private GooglePhotosService googlePhotosService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAllAuthors() {
        try {
            List<AuthorDto> authors = authorService.getAllAuthors();
            return ResponseEntity.ok(authors);
        } catch (Exception e) {
            logger.warn("Failed to retrieve all authors: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/without-description")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAuthorsWithoutDescription() {
        try {
            List<AuthorDto> authors = authorService.getAuthorsWithoutDescription();
            return ResponseEntity.ok(authors);
        } catch (Exception e) {
            logger.warn("Failed to retrieve authors without description: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/zero-books")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAuthorsWithZeroBooks() {
        try {
            List<AuthorDto> authors = authorService.getAuthorsWithZeroBooks();
            return ResponseEntity.ok(authors);
        } catch (Exception e) {
            logger.warn("Failed to retrieve authors with zero books: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/photos")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getPhotosByAuthorId(@PathVariable Long id) {
        try {
            List<PhotoDto> photos = photoService.getPhotosByAuthorId(id);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            logger.warn("Failed to retrieve photos for author ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/books")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getBooksByAuthorId(@PathVariable Long id) {
        try {
            List<BookDto> books = bookService.getBooksByAuthorId(id);
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books for author ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAuthorById(@PathVariable Long id) {
        try {
            AuthorDto author = authorService.getAuthorById(id);
            return author != null ? ResponseEntity.ok(author) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.warn("Failed to retrieve author by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/photos")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> addPhotoToAuthor(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            PhotoDto newPhoto = photoService.addPhotoToAuthor(id, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(newPhoto);
        } catch (Exception e) {
            logger.warn("Failed to add photo to author ID {} with file {}: {}", id, file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/{authorId}/photos/from-google-photos")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> addPhotosFromGooglePhotos(@PathVariable Long authorId, @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> photos = (List<Map<String, Object>>) request.get("photos");

            if (photos == null || photos.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No photos provided",
                        "savedCount", 0
                ));
            }

            // Get valid access token for downloading photos
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            String accessToken = googlePhotosService.getValidAccessToken(username);

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
                    PhotoDto savedPhoto = photoService.addAuthorPhotoFromGooglePhotos(authorId, photoBytes, mimeType, permanentId);
                    savedPhotos.add(savedPhoto);
                    logger.info("Added photo from Google Photos to author {}: permanentId={}", authorId, permanentId);

                } catch (Exception e) {
                    logger.error("Failed to add photo {} to author {}: {}", permanentId, authorId, e.getMessage());
                    failedPhotos.add(Map.of(
                            "id", permanentId,
                            "error", e.getMessage()
                    ));
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "savedCount", savedPhotos.size(),
                    "failedCount", failedPhotos.size(),
                    "savedPhotos", savedPhotos,
                    "failedPhotos", failedPhotos
            ));

        } catch (Exception e) {
            logger.error("Failed to add photos from Google Photos to author {}: {}", authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", e.getMessage(),
                    "savedCount", 0
            ));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> createAuthor(@Valid @RequestBody AuthorDto authorDto) {
        try {
            AuthorDto created = authorService.createAuthor(authorDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.warn("Failed to create author with DTO {}: {}", authorDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateAuthor(@PathVariable Long id, @Valid @RequestBody AuthorDto authorDto) {
        try {
            AuthorDto updated = authorService.updateAuthor(id, authorDto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.warn("Failed to update author ID {} with DTO {}: {}", id, authorDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) {
        try {
            authorService.deleteAuthor(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.warn("Failed to delete author ID {}: {}", id, e.getMessage(), e);
            if (e.getMessage().contains("associated books")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        }
    }

    @PostMapping("/delete-authors-with-no-books")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> deleteAuthorsWithNoBooks() {
        try {
            int deletedCount = authorService.deleteAuthorsWithNoBooks();
            logger.info("Deleted {} authors with no books", deletedCount);
            return ResponseEntity.ok(Map.of(
                    "message", "Deleted " + deletedCount + " author(s) with no books",
                    "deletedCount", deletedCount
            ));
        } catch (Exception e) {
            logger.error("Failed to delete authors with no books: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{authorId}/photos/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> deleteAuthorPhoto(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.deleteAuthorPhoto(authorId, photoId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.warn("Failed to delete photo ID {} for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{authorId}/photos/{photoId}/rotate-cw")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> rotatePhotoCW(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.rotateAuthorPhoto(authorId, photoId, true);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to rotate photo ID {} clockwise for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{authorId}/photos/{photoId}/rotate-ccw")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> rotatePhotoCCW(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.rotateAuthorPhoto(authorId, photoId, false);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to rotate photo ID {} counter-clockwise for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{authorId}/photos/{photoId}/move-left")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> moveAuthorPhotoLeft(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.moveAuthorPhotoLeft(authorId, photoId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to move photo ID {} left for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{authorId}/photos/{photoId}/move-right")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> moveAuthorPhotoRight(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.moveAuthorPhotoRight(authorId, photoId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to move photo ID {} right for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
