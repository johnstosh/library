/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.BookStatusFilter;
import com.muczynski.library.domain.ReadingDifficulty;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.BookSummaryDto;
import com.muczynski.library.dto.CountDto;
import com.muczynski.library.dto.BulkDeleteResultDto;
import com.muczynski.library.dto.GenreLookupResultDto;
import com.muczynski.library.dto.NameNormalizationResultDto;
import com.muczynski.library.dto.ReadingDifficultyLookupResultDto;
import com.muczynski.library.dto.SavedBookDto;
import com.muczynski.library.dto.PhotoAddFromGooglePhotosResponse;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.dto.GrokJobDto;
import com.muczynski.library.exception.BookHasNoPhotosException;
import com.muczynski.library.exception.GrokCreditsExhaustedException;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.dto.CheckoutMatchDto;
import com.muczynski.library.service.AskGrok;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.GrokJobService;
import com.muczynski.library.service.ByIds;
import com.muczynski.library.service.CatalogFilterService;
import com.muczynski.library.service.CatalogNameNormalizationService;
import com.muczynski.library.service.CheckoutMatchService;
import com.muczynski.library.service.GooglePhotosService;
import com.muczynski.library.service.GrokipediaLookupService;
import com.muczynski.library.service.PhotoService;
import com.muczynski.library.util.SecurityUtils;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    @Autowired
    private BookService bookService;

    @Autowired
    private CatalogFilterService catalogFilterService;

    @Autowired
    private CatalogNameNormalizationService catalogNameNormalizationService;

    @Autowired
    private CheckoutMatchService checkoutMatchService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private GooglePhotosService googlePhotosService;

    @Autowired
    private AskGrok askGrok;

    @Autowired
    private GrokJobService grokJobService;

    @Autowired
    private GrokipediaLookupService grokipediaLookupService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllBooks() {
        try {
            List<BookDto> books = bookService.getAllBooks(isLibrarian());
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            logger.warn("Failed to retrieve all books: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/without-loc")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBooksWithoutLocNumber() {
        try {
            List<BookSummaryDto> summaries = bookService.getSummariesWithoutLocNumber(isLibrarian());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books without LOC number: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Get book summaries (id + lastModified) from most recent 2 days OR with temporary titles.
     * Returns BookSummaryDto for cache validation - use /books/by-ids to fetch full data.
     */
    @GetMapping("/most-recent-day")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBooksFromMostRecentDay() {
        try {
            List<BookSummaryDto> summaries = bookService.getSummariesFromMostRecentDay(isLibrarian());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books from most recent 2 days: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/by-3letter-loc")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBooksWith3LetterLocStart() {
        try {
            List<BookSummaryDto> summaries = bookService.getSummariesWith3LetterLocStart(isLibrarian());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books with 3-letter LOC start: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Get book summaries for books that have ALL of the specified labels.
     * Labels should be passed as a comma-separated string, e.g. ?labels=fiction,history
     */
    @GetMapping("/by-labels")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBooksByLabels(@RequestParam(required = false) String labels) {
        try {
            List<String> labelList = (labels == null || labels.isBlank())
                    ? List.of()
                    : Arrays.stream(labels.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
            List<BookSummaryDto> summaries = bookService.getSummariesByAllLabels(labelList, isLibrarian());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books by labels '{}': {}", labels, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/without-grokipedia")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBooksWithoutGrokipediaUrl() {
        try {
            List<BookSummaryDto> summaries = bookService.getSummariesWithoutGrokipediaUrl(isLibrarian());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books without Grokipedia URL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBookById(@PathVariable Long id) {
        try {
            BookDto book = bookService.getBookById(id);
            return book != null && (isLibrarian() || !BookStatus.REQUESTED.equals(book.getStatus())) ? ResponseEntity.ok(book) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.warn("Failed to retrieve book by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static List<BookCoverType> parseBindings(String raw) {
        List<BookCoverType> selected = new ArrayList<>();
        for (String part : splitCsv(raw)) {
            try {
                BookCoverType value = BookCoverType.valueOf(part.trim().toUpperCase());
                if (!selected.contains(value)) {
                    selected.add(value);
                }
            } catch (IllegalArgumentException ignored) {
                // Unknown tokens are ignored, matching the other filter parsers.
            }
        }
        return selected;
    }

    private static void applyDesire(CatalogFilterService.BookCatalogFilter filter, String raw) {
        List<Integer> values = new ArrayList<>();
        boolean unset = false;
        for (String part : splitCsv(raw)) {
            if ("unset".equalsIgnoreCase(part)) {
                unset = true;
                continue;
            }
            try {
                int value = Integer.parseInt(part);
                if (value >= 0 && value <= 10 && !values.contains(value)) {
                    values.add(value);
                }
            } catch (NumberFormatException ignored) {
                // Skip tokens that are not a desire value.
            }
        }
        filter.desireValues = values;
        filter.desireUnset = unset;
    }

    private static Long userId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return null;
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isLibrarian() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && SecurityUtils.isLibrarian(authentication);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional
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
    @Transactional
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
    @Transactional
    public ResponseEntity<BulkDeleteResultDto> deleteBulkBooks(@RequestBody List<Long> bookIds) {
        BulkDeleteResultDto result = bookService.deleteBulkBooks(bookIds);
        if (result.getFailedCount() > 0) {
            logger.warn("Bulk delete partially failed: {} deleted, {} failed",
                    result.getDeletedCount(), result.getFailedCount());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/grokipedia-lookup-bulk")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<GrokipediaLookupResultDto>> grokipediaLookupBulk(
            @RequestBody List<Long> bookIds,
            @RequestParam(defaultValue = "false") boolean slow) {
        logger.info("Looking up Grokipedia URLs for {} books (slow={})", bookIds.size(), slow);
        List<GrokipediaLookupResultDto> results = grokipediaLookupService.lookupBooks(bookIds, slow);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional
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
    public ResponseEntity<?> generateBookByPhoto(@PathVariable Long id) {
        try {
            BookDto updated = bookService.generateTempBook(id);
            return ResponseEntity.ok(updated);
        } catch (GrokCreditsExhaustedException | BookHasNoPhotosException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to generate book by photo for ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/book-from-first-photo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> generateBookFromFirstPhoto(@PathVariable Long id) {
        try {
            BookDto updated = bookService.generateBookFromFirstPhoto(id);
            return ResponseEntity.ok(updated);
        } catch (GrokCreditsExhaustedException | BookHasNoPhotosException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to generate book from first photo for ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Preview title and author extracted from the book's first photo.
     * Does not persist the book; the edit form applies the values until Update or Cancel.
     */
    @PutMapping("/{id}/title-author-from-photo")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<BookDto> getTitleAuthorFromPhoto(@PathVariable Long id) {
        try {
            BookDto updated = bookService.getTitleAuthorFromPhoto(id);
            return ResponseEntity.ok(updated);
        } catch (GrokCreditsExhaustedException | BookHasNoPhotosException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to extract title and author from photo for book ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Not {@code @Transactional}: Grok HTTP must not hold a pool connection, and
     * catch-and-return must not leave a controller txn rollback-only (#362).
     */
    @PutMapping("/{id}/book-from-title-author")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<BookDto> getBookFromTitleAuthor(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String authorName = request.get("authorName");
            BookDto updated = bookService.getBookFromTitleAuthor(id, title, authorName);
            return ResponseEntity.ok(updated);
        } catch (GrokCreditsExhaustedException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to generate book metadata from title and author for book ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== Background Grok jobs ====================
    // A Grok call can take minutes; one long fetch can die in the browser ("Failed to fetch")
    // even when the server succeeds. These start the same work in the background and return
    // 202 with a job id; poll GET /api/grok-jobs/{jobId}. The synchronous endpoints above stay.

    @PostMapping("/{id}/book-by-photo/start")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<GrokJobDto> startBookByPhoto(@PathVariable Long id) {
        return GrokJobController.accepted(
                grokJobService.start("book-by-photo", () -> bookService.generateTempBook(id)));
    }

    @PostMapping("/{id}/book-from-first-photo/start")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<GrokJobDto> startBookFromFirstPhoto(@PathVariable Long id) {
        return GrokJobController.accepted(
                grokJobService.start("book-from-first-photo", () -> bookService.generateBookFromFirstPhoto(id)));
    }

    @PostMapping("/{id}/title-author-from-photo/start")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<GrokJobDto> startTitleAuthorFromPhoto(@PathVariable Long id) {
        return GrokJobController.accepted(
                grokJobService.start("title-author-from-photo", () -> bookService.getTitleAuthorFromPhoto(id)));
    }

    @PostMapping("/{id}/book-from-title-author/start")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<GrokJobDto> startBookFromTitleAuthor(@PathVariable Long id,
                                                               @RequestBody Map<String, String> request) {
        String title = request.get("title");
        String authorName = request.get("authorName");
        return GrokJobController.accepted(
                grokJobService.start("book-from-title-author",
                        () -> bookService.getBookFromTitleAuthor(id, title, authorName)));
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

    @GetMapping("/count")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<CountDto> getBookCount() {
        try {
            return ResponseEntity.ok(new CountDto(bookService.countBooks(isLibrarian())));
        } catch (Exception e) {
            logger.warn("Failed to count books: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/summaries")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllBookSummaries() {
        try {
            List<BookSummaryDto> summaries = bookService.getAllBookSummaries(isLibrarian());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.warn("Failed to retrieve book summaries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Summaries for the Books and Prices pages after every active filter.
     * The browser then loads full rows with /by-ids for these ids only.
     */
    @GetMapping("/filtered-summaries")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFilteredBookSummaries(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String labels,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String readingDifficulty,
            @RequestParam(required = false) String binding,
            @RequestParam(required = false) String favoriteLists,
            @RequestParam(required = false) String desireToPurchase,
            @RequestParam(defaultValue = "90") int priceOlderDays,
            @RequestParam(defaultValue = "false") boolean freeText,
            @RequestParam(defaultValue = "false") boolean audio,
            @RequestParam(defaultValue = "false") boolean mostRecent,
            @RequestParam(defaultValue = "false") boolean withoutGrokipedia,
            @RequestParam(defaultValue = "false") boolean withGrokipedia,
            @RequestParam(defaultValue = "false") boolean withoutGenres,
            @RequestParam(defaultValue = "false") boolean withoutFreeTextUrls,
            @RequestParam(defaultValue = "false") boolean withoutProperPlotOrDescription,
            @RequestParam(defaultValue = "false") boolean titleNotChicago,
            @RequestParam(defaultValue = "false") boolean authorNotCanonical,
            @RequestParam(defaultValue = "false") boolean hasYdlAudio,
            @RequestParam(defaultValue = "false") boolean hasYdlBook,
            @RequestParam(defaultValue = "false") boolean hasYdlEbook,
            @RequestParam(defaultValue = "false") boolean hasEmuAudio,
            @RequestParam(defaultValue = "false") boolean hasEmuBook,
            @RequestParam(defaultValue = "false") boolean hasEmuEbook,
            @RequestParam(defaultValue = "false") boolean hasAclaAudio,
            @RequestParam(defaultValue = "false") boolean hasAclaBook,
            @RequestParam(defaultValue = "false") boolean hasAclaEbook,
            @RequestParam(defaultValue = "false") boolean withPrices,
            @RequestParam(defaultValue = "false") boolean noPrices,
            @RequestParam(defaultValue = "false") boolean priceOlder,
            @RequestParam(defaultValue = "false") boolean lookupErrors,
            Principal principal) {
        try {
            CatalogFilterService.BookCatalogFilter filter = new CatalogFilterService.BookCatalogFilter();
            filter.query = q == null ? "" : q;
            filter.labels = splitCsv(labels);
            filter.statuses = BookStatusFilter.parseFilterValues(status);
            filter.readingDifficulties = ReadingDifficulty.parseFilterValues(readingDifficulty);
            filter.bindings = parseBindings(binding);
            filter.favoriteLists = splitCsv(favoriteLists);
            applyDesire(filter, desireToPurchase);
            filter.priceOlderDays = priceOlderDays;
            filter.freeText = freeText;
            filter.audio = audio;
            filter.mostRecent = mostRecent;
            filter.withoutGrokipedia = withoutGrokipedia;
            filter.withGrokipedia = withGrokipedia;
            filter.withoutGenres = withoutGenres;
            filter.withoutFreeTextUrls = withoutFreeTextUrls;
            filter.withoutProperPlotOrDescription = withoutProperPlotOrDescription;
            filter.titleNotChicago = titleNotChicago;
            filter.authorNotCanonical = authorNotCanonical;
            filter.ydlAudio = hasYdlAudio;
            filter.ydlBook = hasYdlBook;
            filter.ydlEbook = hasYdlEbook;
            filter.emuAudio = hasEmuAudio;
            filter.emuBook = hasEmuBook;
            filter.emuEbook = hasEmuEbook;
            filter.aclaAudio = hasAclaAudio;
            filter.aclaBook = hasAclaBook;
            filter.aclaEbook = hasAclaEbook;
            filter.withPrices = withPrices;
            filter.noPrices = noPrices;
            filter.priceOlder = priceOlder;
            filter.lookupErrors = lookupErrors;
            return ResponseEntity.ok(catalogFilterService.bookSummaries(filter, userId(principal), isLibrarian()));
        } catch (Exception e) {
            logger.warn("Failed to retrieve filtered book summaries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/by-ids")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBooksByIds(@RequestBody List<Long> ids) {
        if (ByIds.exceedsBatch(ids)) {
            logger.warn("Rejected /books/by-ids batch of {} ids; limit is {}", ids.size(), ByIds.MAX_BATCH);
            return ResponseEntity.badRequest().body("At most " + ByIds.MAX_BATCH + " book ids");
        }
        try {
            List<BookDto> books = bookService.getBooksByIds(ids, isLibrarian());
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            logger.warn("Failed to retrieve books by IDs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Active books for the checkout form. At most ten matches. A title, author,
     * or call number shorter than three characters is ignored, and the response
     * is empty when every field is shorter than that.
     */
    @GetMapping("/checkout-matches")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CheckoutMatchDto>> checkoutMatches(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String locNumber) {
        return ResponseEntity.ok(checkoutMatchService.matches(title, author, locNumber));
    }

    @PostMapping("/suggest-loc")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> suggestLocNumber(@RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String author = request.get("author");

            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));
            }

            String suggestion = askGrok.suggestLocNumber(title, author);
            return ResponseEntity.ok(Map.of("suggestion", suggestion));
        } catch (GrokCreditsExhaustedException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to get LOC suggestion for title '{}': {}",
                    request.get("title"), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Not {@code @Transactional}: Grok HTTP must not hold a pool connection, and
     * service catch-and-return must not leave a controller txn rollback-only (#361).
     */
    @PostMapping("/{id}/lookup-genres")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<GenreLookupResultDto> lookupGenresForBook(@PathVariable Long id) {
        logger.info("Looking up genres for book ID {}", id);
        GenreLookupResultDto result = bookService.lookupGenresForBook(id);
        return ResponseEntity.ok(result);
    }

    /** Not {@code @Transactional}: same isolation as {@link #lookupGenresForBook}. */
    @PostMapping("/lookup-genres-bulk")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<GenreLookupResultDto>> lookupGenresBulk(@RequestBody List<Long> bookIds) {
        logger.info("Looking up genres for {} books", bookIds.size());
        List<GenreLookupResultDto> results = bookService.lookupGenresForBooks(bookIds);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/lookup-reading-difficulty-bulk")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @Transactional
    public ResponseEntity<List<ReadingDifficultyLookupResultDto>> lookupReadingDifficultyBulk(
            @RequestBody List<Long> bookIds) {
        logger.info("Filling reading difficulty for {} books", bookIds.size());
        List<ReadingDifficultyLookupResultDto> results = bookService.lookupReadingDifficultyForBooks(bookIds);
        return ResponseEntity.ok(results);
    }

    /**
     * Rewrites selected books' titles into Chicago title case. A title that
     * already conforms is left unchanged. A title that would collide with
     * another book is reported and skipped.
     */
    @PostMapping("/normalize-titles-bulk")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<NameNormalizationResultDto>> normalizeTitlesBulk(@RequestBody List<Long> bookIds) {
        logger.info("Normalizing titles for {} books", bookIds == null ? 0 : bookIds.size());
        return ResponseEntity.ok(catalogNameNormalizationService.normalizeTitles(bookIds));
    }

    /**
     * Rewrites selected books' authors into canonical form. Renames the author
     * row when every one of that author's books is selected; otherwise relinks
     * only the selected books. Photos on the author row are left in place.
     */
    @PostMapping("/normalize-authors-bulk")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<List<NameNormalizationResultDto>> normalizeAuthorsBulk(@RequestBody List<Long> bookIds) {
        logger.info("Normalizing authors for {} books", bookIds == null ? 0 : bookIds.size());
        return ResponseEntity.ok(catalogNameNormalizationService.normalizeAuthors(bookIds));
    }
}
