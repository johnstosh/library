/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.User;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.AuthorSummaryDto;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.PhotoAddFromGooglePhotosResponse;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.AuthorService;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.GooglePhotosService;
import com.muczynski.library.service.GrokipediaLookupService;
import com.muczynski.library.service.PhotoService;
import com.muczynski.library.dto.GrokipediaLookupResultDto;
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

    @Autowired
    private GrokipediaLookupService grokipediaLookupService;

    @Autowired
    private UserRepository userRepository;

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

    @GetMapping("/summaries")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<AuthorSummaryDto>> getAllAuthorSummaries() {
        try {
            List<AuthorSummaryDto> summaries = authorService.getAllAuthorSummaries();
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve author summaries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/by-ids")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<AuthorDto>> getAuthorsByIds(@RequestBody List<Long> ids) {
        try {
            List<AuthorDto> authors = authorService.getAuthorsByIds(ids);
            return ResponseEntity.ok(authors);
        } catch (Exception e) {
            logger.warn("Failed to retrieve authors by IDs {}: {}", ids, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/without-description")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAuthorsWithoutDescription() {
        try {
            List<AuthorSummaryDto> summaries = authorService.getSummariesWithoutDescription();
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve authors without description: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/zero-books")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAuthorsWithZeroBooks() {
        try {
            List<AuthorSummaryDto> summaries = authorService.getSummariesWithZeroBooks();
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve authors with zero books: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/most-recent-day")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAuthorsFromMostRecentDay() {
        try {
            List<AuthorSummaryDto> summaries = authorService.getSummariesFromMostRecentDay();
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve authors from most recent day: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/without-grokipedia")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAuthorsWithoutGrokipedia() {
        try {
            List<AuthorSummaryDto> summaries = authorService.getSummariesWithoutGrokipedia();
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve authors without grokipedia: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/photos")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<PhotoDto>> getPhotosByAuthorId(@PathVariable Long id) {
        try {
            List<PhotoDto> photos = photoService.getPhotosByAuthorId(id);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            logger.warn("Failed to retrieve photos for author ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/books")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<BookDto>> getBooksByAuthorId(@PathVariable Long id) {
        try {
            List<BookDto> books = bookService.getBooksByAuthorId(id);
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books for author ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<AuthorDto> getAuthorById(@PathVariable Long id) {
        try {
            AuthorDto author = authorService.getAuthorById(id);
            return author != null ? ResponseEntity.ok(author) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.warn("Failed to retrieve author by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/photos")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<PhotoDto> addPhotoToAuthor(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            PhotoDto newPhoto = photoService.addPhotoToAuthor(id, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(newPhoto);
        } catch (Exception e) {
            logger.warn("Failed to add photo to author ID {} with file {}: {}", id, file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{authorId}/photos/from-google-photos")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<PhotoAddFromGooglePhotosResponse> addPhotosFromGooglePhotos(@PathVariable Long authorId, @RequestBody Map<String, Object> request) {
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

            PhotoAddFromGooglePhotosResponse response = new PhotoAddFromGooglePhotosResponse();
            response.setSavedCount(savedPhotos.size());
            response.setFailedCount(failedPhotos.size());
            response.setSavedPhotos(savedPhotos);
            response.setFailedPhotos(failedPhotos);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Failed to add photos from Google Photos to author {}: {}", authorId, e.getMessage(), e);
            PhotoAddFromGooglePhotosResponse response = new PhotoAddFromGooglePhotosResponse();
            response.setSavedCount(0);
            response.setFailedCount(0);
            response.setSavedPhotos(Collections.emptyList());
            response.setFailedPhotos(Collections.singletonList(Map.of("error", e.getMessage())));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<AuthorDto> createAuthor(@Valid @RequestBody AuthorDto authorDto) {
        try {
            AuthorDto created = authorService.createAuthor(authorDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.warn("Failed to create author with DTO {}: {}", authorDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<AuthorDto> updateAuthor(@PathVariable Long id, @Valid @RequestBody AuthorDto authorDto) {
        try {
            AuthorDto updated = authorService.updateAuthor(id, authorDto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.warn("Failed to update author ID {} with DTO {}: {}", id, authorDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> deleteAuthor(@PathVariable Long id) {
        try {
            authorService.deleteAuthor(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.warn("Failed to delete author ID {}: {}", id, e.getMessage(), e);
            if (e.getMessage().contains("associated books")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Collections.singletonMap("message", e.getMessage()));
            }
            throw e;
        }
    }

    @PostMapping("/delete-bulk")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> deleteBulkAuthors(@RequestBody List<Long> authorIds) {
        try {
            authorService.deleteBulkAuthors(authorIds);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to bulk delete authors {}: {}", authorIds, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/grokipedia-lookup-bulk")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<GrokipediaLookupResultDto>> grokipediaLookupBulk(@RequestBody List<Long> authorIds) {
        logger.info("Looking up Grokipedia URLs for {} authors", authorIds.size());
        List<GrokipediaLookupResultDto> results = grokipediaLookupService.lookupAuthors(authorIds);
        return ResponseEntity.ok(results);
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
    public ResponseEntity<Void> rotatePhotoCW(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.rotateAuthorPhoto(authorId, photoId, true);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to rotate photo ID {} clockwise for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{authorId}/photos/{photoId}/rotate-ccw")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> rotatePhotoCCW(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.rotateAuthorPhoto(authorId, photoId, false);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to rotate photo ID {} counter-clockwise for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{authorId}/photos/{photoId}/move-left")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> moveAuthorPhotoLeft(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.moveAuthorPhotoLeft(authorId, photoId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to move photo ID {} left for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{authorId}/photos/{photoId}/move-right")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> moveAuthorPhotoRight(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.moveAuthorPhotoRight(authorId, photoId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Failed to move photo ID {} right for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
