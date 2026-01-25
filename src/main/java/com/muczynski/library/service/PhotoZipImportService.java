/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.dto.PhotoZipImportResultDto;
import com.muczynski.library.dto.PhotoZipImportResultDto.PhotoZipImportItemDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for importing photos from a ZIP file with merge/deduplication support.
 *
 * Filename format:
 * - book-{title}[-{n}].{ext} - Associates photo with a book by title
 * - author-{name}[-{n}].{ext} - Associates photo with an author by name
 * - loan-{title}-{username}[-{n}].{ext} - Associates photo with a loan
 *
 * The optional [-{n}] is the 1-based photo order (position) for the entity.
 * The title/name is sanitized (lowercase, special chars replaced with dashes).
 *
 * Merge behavior (for book and author photos):
 * - Uses SHA-256 checksum for deduplication
 * - If photo exists at same order with same checksum: skip (duplicate)
 * - If photo exists at same order with different checksum: replace existing
 * - If no photo exists at that order: add new photo
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
    private final PhotoRepository photoRepository;

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
     * Compute SHA-256 checksum of image bytes for deduplication.
     */
    private String computeChecksum(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(imageBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return null;
        }
    }

    /**
     * Import photos from a ZIP file.
     *
     * @param zipFile the uploaded ZIP file
     * @return result containing import statistics and details
     */
    public PhotoZipImportResultDto importFromZip(MultipartFile zipFile) throws IOException {
        return importFromZipStream(zipFile.getInputStream());
    }

    /**
     * Import photos from a ZIP input stream.
     * This method processes the ZIP as it streams in, without buffering the entire file.
     * Supports files of any size.
     *
     * @param inputStream the ZIP input stream
     * @return result containing import statistics and details
     */
    public PhotoZipImportResultDto importFromZipStream(InputStream inputStream) throws IOException {
        List<PhotoZipImportItemDto> items = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;

        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
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
        String numberStr = matcher.group(3); // Optional photo order number
        String extension = matcher.group(4).toLowerCase();

        // Parse photo order: if number is present, use it; otherwise default to 0 (first photo)
        // Photo order in filename is 1-based (for user friendliness), but stored as 0-based
        int photoOrder = 0;
        if (numberStr != null && !numberStr.isEmpty()) {
            photoOrder = Integer.parseInt(numberStr) - 1; // Convert to 0-based
            if (photoOrder < 0) photoOrder = 0;
        }

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
                case "book" -> importBookPhoto(filename, name, imageBytes, contentType, photoOrder);
                case "author" -> importAuthorPhoto(filename, name, imageBytes, contentType, photoOrder);
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
     * Import a photo for a book with merge/deduplication support.
     * - If photo exists at same order with same checksum: skip (duplicate)
     * - If photo exists at same order with different checksum: replace
     * - If no photo exists at that order: add new photo
     */
    private PhotoZipImportItemDto importBookPhoto(String filename, String sanitizedTitle,
                                                   byte[] imageBytes, String contentType, int photoOrder) {
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

        // Compute checksum for deduplication
        String newChecksum = computeChecksum(imageBytes);

        // Check if photo already exists at this order for the book
        List<Photo> existingPhotos = photoRepository.findByBookIdAndPhotoOrderOrderByIdAsc(book.getId(), photoOrder);
        if (!existingPhotos.isEmpty()) {
            Photo existingPhoto = existingPhotos.get(0);
            String existingChecksum = existingPhoto.getImageChecksum();

            if (newChecksum != null && newChecksum.equals(existingChecksum)) {
                // Same photo already exists - skip
                log.info("Skipping duplicate photo for book '{}' at order {} (same checksum)", book.getTitle(), photoOrder);
                return PhotoZipImportItemDto.builder()
                        .filename(filename)
                        .status("SKIPPED")
                        .entityType("book")
                        .entityName(book.getTitle())
                        .entityId(book.getId())
                        .photoId(existingPhoto.getId())
                        .errorMessage("Duplicate photo (same checksum)")
                        .build();
            } else {
                // Different photo at same order - replace
                log.info("Replacing photo for book '{}' at order {} (different checksum)", book.getTitle(), photoOrder);
                existingPhoto.setImage(imageBytes);
                existingPhoto.setContentType(contentType);
                existingPhoto.setImageChecksum(newChecksum);
                photoRepository.save(existingPhoto);
                return PhotoZipImportItemDto.builder()
                        .filename(filename)
                        .status("SUCCESS")
                        .entityType("book")
                        .entityName(book.getTitle())
                        .entityId(book.getId())
                        .photoId(existingPhoto.getId())
                        .build();
            }
        }

        // No existing photo at this order - add new
        PhotoDto photo = photoService.addPhotoFromBytes(book.getId(), imageBytes, contentType);

        log.info("Imported photo for book '{}' (ID: {}) at order {}", book.getTitle(), book.getId(), photoOrder);

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
     * Import a photo for an author with merge/deduplication support.
     * - If photo exists at same order with same checksum: skip (duplicate)
     * - If photo exists at same order with different checksum: replace
     * - If no photo exists at that order: add new photo
     */
    private PhotoZipImportItemDto importAuthorPhoto(String filename, String sanitizedName,
                                                     byte[] imageBytes, String contentType, int photoOrder) {
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

        // Compute checksum for deduplication
        String newChecksum = computeChecksum(imageBytes);

        // Check if photo already exists at this order for the author (author photos have book=null)
        List<Photo> existingPhotos = photoRepository.findByAuthorIdAndBookIsNullAndPhotoOrderOrderByIdAsc(author.getId(), photoOrder);
        if (!existingPhotos.isEmpty()) {
            Photo existingPhoto = existingPhotos.get(0);
            String existingChecksum = existingPhoto.getImageChecksum();

            if (newChecksum != null && newChecksum.equals(existingChecksum)) {
                // Same photo already exists - skip
                log.info("Skipping duplicate photo for author '{}' at order {} (same checksum)", author.getName(), photoOrder);
                return PhotoZipImportItemDto.builder()
                        .filename(filename)
                        .status("SKIPPED")
                        .entityType("author")
                        .entityName(author.getName())
                        .entityId(author.getId())
                        .photoId(existingPhoto.getId())
                        .errorMessage("Duplicate photo (same checksum)")
                        .build();
            } else {
                // Different photo at same order - replace
                log.info("Replacing photo for author '{}' at order {} (different checksum)", author.getName(), photoOrder);
                existingPhoto.setImage(imageBytes);
                existingPhoto.setContentType(contentType);
                existingPhoto.setImageChecksum(newChecksum);
                photoRepository.save(existingPhoto);
                return PhotoZipImportItemDto.builder()
                        .filename(filename)
                        .status("SUCCESS")
                        .entityType("author")
                        .entityName(author.getName())
                        .entityId(author.getId())
                        .photoId(existingPhoto.getId())
                        .build();
            }
        }

        // No existing photo at this order - add new
        PhotoDto photo = photoService.addAuthorPhotoFromBytes(author.getId(), imageBytes, contentType);

        log.info("Imported photo for author '{}' (ID: {}) at order {}", author.getName(), author.getId(), photoOrder);

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
