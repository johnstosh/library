/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Authority;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.ChunkUploadResultDto;
import com.muczynski.library.dto.PhotoZipImportResultDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.AuthorityRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.GooglePhotosService;
import com.muczynski.library.photostorage.client.GooglePhotosLibraryClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for photo ZIP export and chunked import functionality.
 * Tests the round-trip capability of exporting photos to ZIP and importing them back
 * via the chunked upload endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhotoZipExportImportTest {

    private static final int CHUNK_SIZE = 64 * 1024; // 64KB chunks for tests

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    // Mock external Google Photos services
    @MockitoBean
    private GooglePhotosService googlePhotosService;

    @MockitoBean
    private GooglePhotosLibraryClient googlePhotosLibraryClient;

    private Book testBook;
    private Book testBook2;
    private Author testAuthor;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test authority
        Authority librarianAuth = new Authority();
        librarianAuth.setName("LIBRARIAN");
        librarianAuth = authorityRepository.save(librarianAuth);

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("{bcrypt}$2a$10$...");
        testUser.setAuthorities(Set.of(librarianAuth));
        testUser = userRepository.save(testUser);

        // Create test author
        testAuthor = new Author();
        testAuthor.setName("Mark Twain");
        testAuthor = authorRepository.save(testAuthor);

        // Create test books
        testBook = new Book();
        testBook.setTitle("The Adventures of Tom Sawyer");
        testBook.setAuthor(testAuthor);
        testBook = bookRepository.save(testBook);

        testBook2 = new Book();
        testBook2.setTitle("Adventures of Huckleberry Finn");
        testBook2.setAuthor(testAuthor);
        testBook2 = bookRepository.save(testBook2);
    }

    @AfterEach
    void tearDown() {
        loanRepository.deleteAll();
        photoRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }

    private byte[] createDummyImage(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Add some variation to make different images
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, (x * y) % 0xFFFFFF);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private String computeChecksum(byte[] imageBytes) throws Exception {
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
    }

    private Photo createBookPhoto(Book book, String caption, int order) throws Exception {
        byte[] imageBytes = createDummyImage(200 + order, 300 + order);
        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(imageBytes);
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photo.setCaption(caption);
        photo.setPhotoOrder(order);
        photo.setImageChecksum(computeChecksum(imageBytes));
        photo.setExportStatus(Photo.ExportStatus.PENDING);
        return photoRepository.save(photo);
    }

    private Photo createAuthorPhoto(Author author, String caption) throws Exception {
        byte[] imageBytes = createDummyImage(250, 350);
        Photo photo = new Photo();
        photo.setAuthor(author);
        photo.setImage(imageBytes);
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photo.setCaption(caption);
        photo.setPhotoOrder(0);
        photo.setImageChecksum(computeChecksum(imageBytes));
        photo.setExportStatus(Photo.ExportStatus.PENDING);
        return photoRepository.save(photo);
    }

    /**
     * Build a ZIP file in memory from entries.
     */
    private byte[] buildZip(ZipEntryData... entries) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (ZipEntryData entry : entries) {
                zos.putNextEntry(new ZipEntry(entry.filename));
                zos.write(entry.data);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private record ZipEntryData(String filename, byte[] data) {}

    /**
     * Send a ZIP byte array via the chunked upload endpoint, splitting into CHUNK_SIZE pieces.
     * Returns the final ChunkUploadResultDto (with complete=true and finalResult).
     */
    private ChunkUploadResultDto importZipChunked(byte[] zipBytes) throws Exception {
        return importZipChunked(zipBytes, CHUNK_SIZE);
    }

    private ChunkUploadResultDto importZipChunked(byte[] zipBytes, int chunkSize) throws Exception {
        String uploadId = UUID.randomUUID().toString();
        ChunkUploadResultDto lastResponse = null;
        int chunkIndex = 0;

        for (int offset = 0; offset < zipBytes.length; offset += chunkSize) {
            int end = Math.min(offset + chunkSize, zipBytes.length);
            byte[] chunk = Arrays.copyOfRange(zipBytes, offset, end);
            boolean isLast = end >= zipBytes.length;

            MvcResult result = mockMvc.perform(put("/api/photos/import-zip-chunk")
                            .header("X-Upload-Id", uploadId)
                            .header("X-Chunk-Index", chunkIndex)
                            .header("X-Total-Size", zipBytes.length)
                            .header("X-Is-Last-Chunk", isLast)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .content(chunk))
                    .andExpect(status().isOk())
                    .andReturn();

            lastResponse = objectMapper.readValue(
                    result.getResponse().getContentAsString(), ChunkUploadResultDto.class);
            chunkIndex++;
        }

        assertNotNull(lastResponse);
        assertTrue(lastResponse.isComplete(), "Final chunk response should be complete");
        assertNotNull(lastResponse.getFinalResult(), "Final chunk should have finalResult");
        return lastResponse;
    }

    // ===========================================
    // ZIP Export Tests
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void downloadPhotosAsZip_returnsZipFile() throws Exception {
        // Create photos
        createBookPhoto(testBook, "Tom Sawyer cover", 0);
        createAuthorPhoto(testAuthor, "Mark Twain portrait");

        MvcResult result = mockMvc.perform(get("/api/photo-export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("library-photos-")))
                .andReturn();

        byte[] zipBytes = result.getResponse().getContentAsByteArray();
        assertTrue(zipBytes.length > 0, "ZIP file should not be empty");

        // Verify ZIP contents
        int fileCount = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                fileCount++;
                String filename = entry.getName();
                assertTrue(
                    filename.startsWith("book-") || filename.startsWith("author-"),
                    "Filename should start with book- or author-: " + filename
                );
                assertTrue(
                    filename.endsWith(".jpg"),
                    "Filename should end with .jpg: " + filename
                );
            }
        }
        assertEquals(2, fileCount, "ZIP should contain 2 files");
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void downloadPhotosAsZip_multiplePhotosPerBook_usesNumberSuffix() throws Exception {
        // Create multiple photos for same book
        createBookPhoto(testBook, "Cover", 0);
        createBookPhoto(testBook, "Back cover", 1);
        createBookPhoto(testBook, "Spine", 2);

        MvcResult result = mockMvc.perform(get("/api/photo-export"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] zipBytes = result.getResponse().getContentAsByteArray();

        // Verify ZIP contains files with number suffixes
        boolean hasBaseName = false;
        boolean hasSuffix2 = false;
        boolean hasSuffix3 = false;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String filename = entry.getName();
                // New format preserves complete book name with spaces
                if (filename.equals("book-The Adventures of Tom Sawyer.jpg")) {
                    hasBaseName = true;
                } else if (filename.equals("book-The Adventures of Tom Sawyer-2.jpg")) {
                    hasSuffix2 = true;
                } else if (filename.equals("book-The Adventures of Tom Sawyer-3.jpg")) {
                    hasSuffix3 = true;
                }
            }
        }

        assertTrue(hasBaseName, "Should have base filename without suffix");
        assertTrue(hasSuffix2, "Should have filename with -2 suffix");
        assertTrue(hasSuffix3, "Should have filename with -3 suffix");
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void downloadPhotosAsZip_noPhotos_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/photo-export"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("No photos")));
    }

    @Test
    @WithMockUser(username = "1")
    void downloadPhotosAsZip_requiresLibrarianAuthority() throws Exception {
        mockMvc.perform(get("/api/photo-export"))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadPhotosAsZip_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/photo-export"))
                .andExpect(status().isUnauthorized());
    }

    // ===========================================
    // Chunked ZIP Import Tests
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importFromZipChunked_importsBookPhotos() throws Exception {
        byte[] zipBytes = buildZip(
                new ZipEntryData("book-the-adventures-of-tom-sawyer.jpg", createDummyImage(100, 100))
        );

        ChunkUploadResultDto result = importZipChunked(zipBytes);
        PhotoZipImportResultDto finalResult = result.getFinalResult();

        assertEquals(1, finalResult.getTotalFiles());
        assertEquals(1, finalResult.getSuccessCount());
        assertEquals(0, finalResult.getFailureCount());
        assertEquals("SUCCESS", finalResult.getItems().get(0).getStatus());
        assertEquals("book", finalResult.getItems().get(0).getEntityType());
        assertEquals("The Adventures of Tom Sawyer", finalResult.getItems().get(0).getEntityName());

        // Verify photo was created
        var photos = photoRepository.findByBookIdOrderByPhotoOrder(testBook.getId());
        assertEquals(1, photos.size(), "Book should have 1 photo");
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importFromZipChunked_importsAuthorPhotos() throws Exception {
        byte[] zipBytes = buildZip(
                new ZipEntryData("author-mark-twain.jpg", createDummyImage(100, 100))
        );

        ChunkUploadResultDto result = importZipChunked(zipBytes);
        PhotoZipImportResultDto finalResult = result.getFinalResult();

        assertEquals(1, finalResult.getSuccessCount());
        assertEquals("author", finalResult.getItems().get(0).getEntityType());
        assertEquals("Mark Twain", finalResult.getItems().get(0).getEntityName());

        // Verify photo was created
        var photos = photoRepository.findByAuthorId(testAuthor.getId());
        assertEquals(1, photos.size(), "Author should have 1 photo");
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importFromZipChunked_skipsUnrecognizedFilenames() throws Exception {
        byte[] imageBytes = createDummyImage(100, 100);
        byte[] zipBytes = buildZip(
                new ZipEntryData("book-the-adventures-of-tom-sawyer.jpg", imageBytes),
                new ZipEntryData("random-photo.jpg", imageBytes),
                new ZipEntryData("readme.txt", "Hello".getBytes())
        );

        ChunkUploadResultDto result = importZipChunked(zipBytes);
        PhotoZipImportResultDto finalResult = result.getFinalResult();

        assertEquals(3, finalResult.getTotalFiles());
        assertEquals(1, finalResult.getSuccessCount());
        assertEquals(2, finalResult.getSkippedCount());
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importFromZipChunked_failsForUnknownEntity() throws Exception {
        byte[] zipBytes = buildZip(
                new ZipEntryData("book-nonexistent-book-title.jpg", createDummyImage(100, 100))
        );

        ChunkUploadResultDto result = importZipChunked(zipBytes);
        PhotoZipImportResultDto finalResult = result.getFinalResult();

        assertEquals(1, finalResult.getFailureCount());
        assertEquals("FAILURE", finalResult.getItems().get(0).getStatus());
        assertTrue(finalResult.getItems().get(0).getErrorMessage().contains("No book found"));
    }

    @Test
    @WithMockUser(username = "1")
    void importFromZipChunked_requiresLibrarianAuthority() throws Exception {
        byte[] zipBytes = buildZip(
                new ZipEntryData("book-test.jpg", createDummyImage(100, 100))
        );

        mockMvc.perform(put("/api/photos/import-zip-chunk")
                        .header("X-Upload-Id", UUID.randomUUID().toString())
                        .header("X-Chunk-Index", 0)
                        .header("X-Total-Size", zipBytes.length)
                        .header("X-Is-Last-Chunk", true)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(zipBytes))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importFromZipChunked_multipleChunks_processesCorrectly() throws Exception {
        byte[] imageBytes = createDummyImage(100, 100);
        byte[] zipBytes = buildZip(
                new ZipEntryData("book-the-adventures-of-tom-sawyer.jpg", imageBytes),
                new ZipEntryData("author-mark-twain.jpg", imageBytes)
        );

        // Use very small chunks to force multiple round-trips
        ChunkUploadResultDto result = importZipChunked(zipBytes, zipBytes.length / 5 + 1);
        PhotoZipImportResultDto finalResult = result.getFinalResult();

        assertEquals(2, finalResult.getTotalFiles());
        assertEquals(2, finalResult.getSuccessCount());
        assertEquals(0, finalResult.getFailureCount());
    }

    // ===========================================
    // Round-Trip Test: Export -> Chunked Import
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void exportThenChunkedImport_roundTrip_preservesPhotos() throws Exception {
        // Step 1: Create some photos
        createBookPhoto(testBook, "Cover", 0);
        createBookPhoto(testBook, "Back", 1);
        createAuthorPhoto(testAuthor, "Portrait");

        int originalPhotoCount = photoRepository.findAll().size();
        assertEquals(3, originalPhotoCount);

        // Step 2: Export to ZIP
        MvcResult exportResult = mockMvc.perform(get("/api/photo-export"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] exportedZipBytes = exportResult.getResponse().getContentAsByteArray();
        assertTrue(exportedZipBytes.length > 0);

        // Step 3: Delete all photos (simulate restore scenario)
        photoRepository.deleteAll();
        assertEquals(0, photoRepository.findAll().size());

        // Step 4: Import via chunked endpoint
        ChunkUploadResultDto result = importZipChunked(exportedZipBytes);
        PhotoZipImportResultDto finalResult = result.getFinalResult();

        assertEquals(3, finalResult.getSuccessCount());
        assertEquals(0, finalResult.getFailureCount());

        // Step 5: Verify photos were restored
        var restoredPhotos = photoRepository.findAll();
        assertEquals(3, restoredPhotos.size(), "All 3 photos should be restored");

        // Verify book photos
        var bookPhotos = photoRepository.findByBookIdOrderByPhotoOrder(testBook.getId());
        assertEquals(2, bookPhotos.size(), "Book should have 2 photos");

        // Verify author photo
        var authorPhotos = photoRepository.findByAuthorId(testAuthor.getId());
        assertEquals(1, authorPhotos.size(), "Author should have 1 photo");
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void exportThenChunkedImport_preservesImageContent() throws Exception {
        // Create a photo with specific image content
        Photo originalPhoto = createBookPhoto(testBook, "Test", 0);
        String originalChecksum = originalPhoto.getImageChecksum();
        assertNotNull(originalChecksum);

        // Export
        MvcResult exportResult = mockMvc.perform(get("/api/photo-export"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] exportedZipBytes = exportResult.getResponse().getContentAsByteArray();

        // Delete photos
        photoRepository.deleteAll();

        // Import via chunked endpoint
        ChunkUploadResultDto result = importZipChunked(exportedZipBytes);
        assertEquals(1, result.getFinalResult().getSuccessCount());

        // Verify checksum matches
        var restoredPhotos = photoRepository.findByBookIdOrderByPhotoOrder(testBook.getId());
        assertEquals(1, restoredPhotos.size());
        assertEquals(originalChecksum, restoredPhotos.get(0).getImageChecksum(),
                "Image checksum should match after round-trip");
    }

    // ===========================================
    // Long Title Tests (from branch-archive.json)
    // ===========================================

    /**
     * The 12 longest book titles from branch-archive.json, plus the shorter
     * "Catholic and Christian" variant to test prefix/exact match disambiguation.
     * These test that our filename sanitization and import matching handle real-world edge cases.
     */
    private static final String[] LONGEST_TITLES = {
        "Foundations of Coding: Theory and Applications of Error-Correcting Codes with an Introduction to Cryptography and Information Theory",
        "Sermons of St. Bernard on Advent & Christmas Including the Famous Treatise on the Incarnation Called \"Missus Est\"",
        "Butler's Lives of the Saints New Full Edition Supplement of New Saints and Blesseds Volume 1, c. 2",
        "Butler's Lives of the Saints New Full Edition Supplement of New Saints and Blesseds Volume 1, c. 3",
        "Butler's Lives of the Saints New Full Edition Supplement of New Saints and Blesseds Volume 1, c. 1",
        "Catholic and Christian: An Explanation of Commonly Misunderstood Catholic Beliefs, Study Guide",
        "Catholic and Christian: An Explanation of Commonly Misunderstood Catholic Beliefs",
        "Saint Stanislaus of Jesus and Mary Papczynski: The Life and Writings of the Marians' Founder",
        "Friendship with Jesus: Pope Benedict XVI Speaks to Children on Their First Holy Communion",
        "Drawing Stories from Around the World and a Sampling of European Handkerchief Stories",
        "Mother Angelica: The Remarkable Story of a Nun, Her Nerve, and a Network of Miracles",
        "To the Heart of the Matter: The 40-Day Companion to Live a Culture of Life",
        "Praying the Rosary: With the Joyful, Luminous, Sorrowful, & Glorious Mysteries"
    };

    /**
     * Invalid filename characters that must be sanitized: / \ : * ? " < > |
     */
    private static final String INVALID_FILENAME_CHARS = "/\\:*?\"<>|";

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void exportThenChunkedImport_longestTitles_roundTrip() throws Exception {
        // Create books with the 12 longest titles
        for (int i = 0; i < LONGEST_TITLES.length; i++) {
            Book book = new Book();
            book.setTitle(LONGEST_TITLES[i]);
            book.setAuthor(testAuthor);
            book = bookRepository.save(book);

            // Create a photo for each book
            Photo photo = new Photo();
            photo.setBook(book);
            photo.setImage(createDummyImage(50 + i, 50 + i)); // Slightly different images
            photo.setContentType("image/jpeg");
            photo.setPhotoOrder(0);
            photo.setImageChecksum(computeChecksum(photo.getImage()));
            photoRepository.save(photo);
        }

        // Verify we have photos for all titles
        assertEquals(LONGEST_TITLES.length, photoRepository.findAll().size());

        // Export to ZIP
        MvcResult exportResult = mockMvc.perform(get("/api/photo-export"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] exportedZipBytes = exportResult.getResponse().getContentAsByteArray();
        assertTrue(exportedZipBytes.length > 0, "ZIP should not be empty");

        // Verify all files are in the ZIP
        int fileCount = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(exportedZipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String filename = entry.getName();
                // Verify filename doesn't contain invalid characters
                for (char c : INVALID_FILENAME_CHARS.toCharArray()) {
                    assertFalse(filename.contains(String.valueOf(c)),
                            "Filename should not contain '" + c + "': " + filename);
                }
                fileCount++;
            }
        }
        assertEquals(LONGEST_TITLES.length, fileCount, "Should have " + LONGEST_TITLES.length + " files in ZIP");

        // Delete all photos
        photoRepository.deleteAll();
        assertEquals(0, photoRepository.findAll().size());

        // Import via chunked endpoint
        ChunkUploadResultDto result = importZipChunked(exportedZipBytes);
        PhotoZipImportResultDto finalResult = result.getFinalResult();

        assertEquals(LONGEST_TITLES.length, finalResult.getSuccessCount());
        assertEquals(0, finalResult.getFailureCount());

        // Verify all photos were restored to the correct books
        var restoredPhotos = photoRepository.findAll();
        assertEquals(LONGEST_TITLES.length, restoredPhotos.size(), "All photos should be restored");

        // Verify each title got its photo back
        for (String title : LONGEST_TITLES) {
            Book book = bookRepository.findAll().stream()
                    .filter(b -> b.getTitle().equals(title))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Book not found: " + title));
            var photos = photoRepository.findByBookIdOrderByPhotoOrder(book.getId());
            assertEquals(1, photos.size(), "Book should have 1 photo: " + title);
        }
    }

    @Test
    void verifyAllTitles_noInvalidFilenameCharactersAfterSanitization() {
        // All titles that contain invalid filename characters (from branch-archive.json analysis)
        String[] titlesWithInvalidChars = {
            "Foundations of Coding: Theory and Applications of Error-Correcting Codes with an Introduction to Cryptography and Information Theory",
            "Sermons of St. Bernard on Advent & Christmas Including the Famous Treatise on the Incarnation Called \"Missus Est\"",
            "Catholic and Christian: An Explanation of Commonly Misunderstood Catholic Beliefs, Study Guide",
            "Saint Stanislaus of Jesus and Mary Papczynski: The Life and Writings of the Marians' Founder",
            "Friendship with Jesus: Pope Benedict XVI Speaks to Children on Their First Holy Communion",
            "Mother Angelica: The Remarkable Story of a Nun, Her Nerve, and a Network of Miracles",
            "Catholic and Christian: An Explanation of Commonly Misunderstood Catholic Beliefs",
            "Praying the Rosary: With the Joyful, Luminous, Sorrowful, & Glorious Mysteries",
            "To the Heart of the Matter: The 40-Day Companion to Live a Culture of Life",
            "Joy to the World: How Christ's Coming Changed Everything (and Still Does)",
            "More of the Holy Spirit: How to Keep the Fire Burning in Our Hearts",
            "The Adventures of Loupio, Volume 1: The Encounter and other stories",
            "The Adventures of Loupio, Volume 2: The Hunters and Other Stories",
            "Absolute Relativism: The New Dictatorship And What To Do About It",
            "The Discernment of Spirits: An Ignatian Guide for Everyday Living",
            "The Road to Bethlehem: Daily Meditations for Advent and Christmas",
            "Can You Find Jesus?: Introducing Your Child to the Gospel",
            "Our Lady of Guadalupe: Mother of the Civilization of Love",
            "Can You Find Saints?",
            "Code: Polonaise",
            "Mary: God's Yes to Man"
        };

        for (String title : titlesWithInvalidChars) {
            // Verify original title contains at least one invalid character
            boolean hasInvalidChar = false;
            for (char c : INVALID_FILENAME_CHARS.toCharArray()) {
                if (title.contains(String.valueOf(c))) {
                    hasInvalidChar = true;
                    break;
                }
            }
            assertTrue(hasInvalidChar, "Test title should contain invalid char: " + title);

            // Apply the same sanitization as PhotoExportService.sanitizeName()
            String sanitized = sanitizeName(title);

            // Verify sanitized name doesn't contain any invalid characters
            for (char c : INVALID_FILENAME_CHARS.toCharArray()) {
                assertFalse(sanitized.contains(String.valueOf(c)),
                        "Sanitized name should not contain '" + c + "': " + sanitized + " (from: " + title + ")");
            }

            // Verify sanitized name is not empty
            assertFalse(sanitized.isEmpty(), "Sanitized name should not be empty for: " + title);
        }
    }

    /**
     * Mirrors the sanitization logic from PhotoExportService.sanitizeName()
     */
    private String sanitizeName(String name) {
        if (name == null) return "unknown";
        return name
                .replaceAll("[/\\\\:*?\"<>|]+", "-")  // Replace invalid filename chars with dash
                .replaceAll("\\s+", " ")              // Normalize whitespace
                .trim()
                .replaceAll("^-+|-+$", "");           // Remove leading/trailing dashes
    }
}
