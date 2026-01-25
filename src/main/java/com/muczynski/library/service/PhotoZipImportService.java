/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Loan;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.dto.PhotoZipImportResultDto;
import com.muczynski.library.dto.PhotoZipImportResultDto.PhotoZipImportItemDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for importing photos from a ZIP file.
 *
 * Filename format:
 * - book-{title}[-{n}].{ext} - Associates photo with a book by title
 * - author-{name}[-{n}].{ext} - Associates photo with an author by name
 * - loan-{title}-{username}[-{n}].{ext} - Associates photo with a loan
 *
 * The optional [-{n}] is for multiple photos of the same entity.
 * The title/name is sanitized (lowercase, special chars replaced with dashes).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PhotoZipImportService {

    private final PhotoService photoService;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final LoanRepository loanRepository;

    // Pattern: type-name[-number].ext
    // Groups: 1=type, 2=name, 3=optional number, 4=extension
    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "^(book|author|loan)-(.+?)(?:-(\\d+))?\\.([a-zA-Z]+)$",
            Pattern.CASE_INSENSITIVE
    );

    // Supported image extensions
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    /**
     * Import photos from a ZIP file.
     *
     * @param zipFile the uploaded ZIP file
     * @return result containing import statistics and details
     */
    public PhotoZipImportResultDto importFromZip(MultipartFile zipFile) throws IOException {
        List<PhotoZipImportItemDto> items = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String filename = getFilenameFromPath(entry.getName());
                PhotoZipImportItemDto item = processEntry(filename, zis);
                items.add(item);

                switch (item.getStatus()) {
                    case "SUCCESS" -> successCount++;
                    case "FAILURE" -> failureCount++;
                    case "SKIPPED" -> skippedCount++;
                }

                zis.closeEntry();
            }
        }

        return PhotoZipImportResultDto.builder()
                .totalFiles(items.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .skippedCount(skippedCount)
                .items(items)
                .build();
    }

    /**
     * Extract just the filename from a path that might include directories.
     */
    private String getFilenameFromPath(String path) {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * Process a single ZIP entry.
     */
    private PhotoZipImportItemDto processEntry(String filename, InputStream inputStream) {
        Matcher matcher = FILENAME_PATTERN.matcher(filename);

        if (!matcher.matches()) {
            log.debug("Skipping file with unrecognized format: {}", filename);
            return PhotoZipImportItemDto.builder()
                    .filename(filename)
                    .status("SKIPPED")
                    .errorMessage("Filename format not recognized. Expected: type-name[-n].ext")
                    .build();
        }

        String type = matcher.group(1).toLowerCase();
        String name = matcher.group(2);
        String extension = matcher.group(4).toLowerCase();

        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            return PhotoZipImportItemDto.builder()
                    .filename(filename)
                    .status("SKIPPED")
                    .entityType(type)
                    .entityName(name)
                    .errorMessage("Unsupported file extension: " + extension)
                    .build();
        }

        // Read the image bytes
        byte[] imageBytes;
        try {
            imageBytes = readAllBytes(inputStream);
        } catch (IOException e) {
            log.error("Failed to read file bytes: {}", filename, e);
            return PhotoZipImportItemDto.builder()
                    .filename(filename)
                    .status("FAILURE")
                    .entityType(type)
                    .entityName(name)
                    .errorMessage("Failed to read file: " + e.getMessage())
                    .build();
        }

        String contentType = getContentType(extension);

        try {
            return switch (type) {
                case "book" -> importBookPhoto(filename, name, imageBytes, contentType);
                case "author" -> importAuthorPhoto(filename, name, imageBytes, contentType);
                case "loan" -> importLoanPhoto(filename, name, imageBytes, contentType);
                default -> PhotoZipImportItemDto.builder()
                        .filename(filename)
                        .status("SKIPPED")
                        .errorMessage("Unknown entity type: " + type)
                        .build();
            };
        } catch (Exception e) {
            log.error("Failed to import photo: {}", filename, e);
            return PhotoZipImportItemDto.builder()
                    .filename(filename)
                    .status("FAILURE")
                    .entityType(type)
                    .entityName(name)
                    .errorMessage("Import failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Import a photo for a book.
     */
    private PhotoZipImportItemDto importBookPhoto(String filename, String sanitizedTitle,
                                                   byte[] imageBytes, String contentType) {
        // Try to find book by title (case-insensitive, partial match)
        String searchTitle = unsanitizeName(sanitizedTitle);
        List<Book> books = bookRepository.findAll().stream()
                .filter(b -> b.getTitle().toLowerCase().contains(searchTitle.toLowerCase()))
                .toList();

        if (books.isEmpty()) {
            // Try exact match with sanitized comparison
            books = bookRepository.findAll().stream()
                    .filter(b -> sanitizeName(b.getTitle()).equalsIgnoreCase(sanitizedTitle))
                    .toList();
        }

        if (books.isEmpty()) {
            return PhotoZipImportItemDto.builder()
                    .filename(filename)
                    .status("FAILURE")
                    .entityType("book")
                    .entityName(searchTitle)
                    .errorMessage("No book found matching: " + searchTitle)
                    .build();
        }

        if (books.size() > 1) {
            log.warn("Multiple books found for '{}', using first match", searchTitle);
        }

        Book book = books.get(0);
        PhotoDto photo = photoService.addPhotoFromBytes(book.getId(), imageBytes, contentType);

        log.info("Imported photo for book '{}' (ID: {})", book.getTitle(), book.getId());

        return PhotoZipImportItemDto.builder()
                .filename(filename)
                .status("SUCCESS")
                .entityType("book")
                .entityName(book.getTitle())
                .entityId(book.getId())
                .photoId(photo.getId())
                .build();
    }

    /**
     * Import a photo for an author.
     */
    private PhotoZipImportItemDto importAuthorPhoto(String filename, String sanitizedName,
                                                     byte[] imageBytes, String contentType) {
        String searchName = unsanitizeName(sanitizedName);
        List<Author> authors = authorRepository.findAll().stream()
                .filter(a -> a.getName().toLowerCase().contains(searchName.toLowerCase()))
                .toList();

        if (authors.isEmpty()) {
            authors = authorRepository.findAll().stream()
                    .filter(a -> sanitizeName(a.getName()).equalsIgnoreCase(sanitizedName))
                    .toList();
        }

        if (authors.isEmpty()) {
            return PhotoZipImportItemDto.builder()
                    .filename(filename)
                    .status("FAILURE")
                    .entityType("author")
                    .entityName(searchName)
                    .errorMessage("No author found matching: " + searchName)
                    .build();
        }

        if (authors.size() > 1) {
            log.warn("Multiple authors found for '{}', using first match", searchName);
        }

        Author author = authors.get(0);
        PhotoDto photo = photoService.addAuthorPhotoFromBytes(author.getId(), imageBytes, contentType);

        log.info("Imported photo for author '{}' (ID: {})", author.getName(), author.getId());

        return PhotoZipImportItemDto.builder()
                .filename(filename)
                .status("SUCCESS")
                .entityType("author")
                .entityName(author.getName())
                .entityId(author.getId())
                .photoId(photo.getId())
                .build();
    }

    /**
     * Import a photo for a loan (checkout card).
     * Format: loan-{title}-{username}[-n].ext
     */
    private PhotoZipImportItemDto importLoanPhoto(String filename, String combined,
                                                   byte[] imageBytes, String contentType) {
        // Try to split title and username
        // The format should be: title-username
        int lastDash = combined.lastIndexOf('-');
        if (lastDash <= 0) {
            return PhotoZipImportItemDto.builder()
                    .filename(filename)
                    .status("FAILURE")
                    .entityType("loan")
                    .entityName(combined)
                    .errorMessage("Invalid loan filename format. Expected: loan-title-username[-n].ext")
                    .build();
        }

        String titlePart = combined.substring(0, lastDash);
        String usernamePart = combined.substring(lastDash + 1);

        String searchTitle = unsanitizeName(titlePart);
        String searchUsername = unsanitizeName(usernamePart);

        // Find loans matching book title and username
        List<Loan> loans = loanRepository.findAll().stream()
                .filter(loan -> {
                    if (loan.getBook() == null || loan.getUser() == null) return false;
                    boolean titleMatches = loan.getBook().getTitle().toLowerCase()
                            .contains(searchTitle.toLowerCase());
                    boolean userMatches = loan.getUser().getUsername().toLowerCase()
                            .contains(searchUsername.toLowerCase());
                    return titleMatches && userMatches;
                })
                .toList();

        if (loans.isEmpty()) {
            return PhotoZipImportItemDto.builder()
                    .filename(filename)
                    .status("FAILURE")
                    .entityType("loan")
                    .entityName(searchTitle + " by " + searchUsername)
                    .errorMessage("No loan found for book '" + searchTitle + "' and user '" + searchUsername + "'")
                    .build();
        }

        Loan loan = loans.get(0);
        PhotoDto photo = photoService.addPhotoToExistingLoan(loan, imageBytes, contentType);

        log.info("Imported photo for loan {} (book: '{}', user: '{}')",
                loan.getId(), loan.getBook().getTitle(), loan.getUser().getUsername());

        return PhotoZipImportItemDto.builder()
                .filename(filename)
                .status("SUCCESS")
                .entityType("loan")
                .entityName(loan.getBook().getTitle() + " - " + loan.getUser().getUsername())
                .entityId(loan.getId())
                .photoId(photo.getId())
                .build();
    }

    /**
     * Convert filename-safe name to search-friendly format.
     * Replaces dashes with spaces and handles common patterns.
     */
    private String unsanitizeName(String sanitized) {
        // Replace dashes with spaces, but preserve intentional dashes
        return sanitized.replace("-", " ").trim();
    }

    /**
     * Sanitize a name for use in filenames.
     * Lowercase, replace spaces and special chars with dashes.
     */
    public static String sanitizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", ""); // Remove leading/trailing dashes
    }

    /**
     * Read all bytes from an input stream without closing it.
     */
    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    /**
     * Get content type from file extension.
     */
    private String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}
