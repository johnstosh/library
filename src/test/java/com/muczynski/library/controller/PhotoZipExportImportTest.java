/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Authority;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.domain.User;
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
import org.springframework.mock.web.MockMultipartFile;
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
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for photo ZIP export and import functionality.
 * Tests the round-trip capability of exporting photos to ZIP and importing them back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhotoZipExportImportTest {

    @Autowired
    private MockMvc mockMvc;

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
    // ZIP Import Tests
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importFromZip_importsBookPhotos() throws Exception {
        // Create a ZIP file with a book photo
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            byte[] imageBytes = createDummyImage(100, 100);
            zos.putNextEntry(new ZipEntry("book-the-adventures-of-tom-sawyer.jpg"));
            zos.write(imageBytes);
            zos.closeEntry();
        }

        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "photos.zip",
                "application/zip",
                baos.toByteArray()
        );

        mockMvc.perform(multipart("/api/photos/import-zip").file(zipFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles", is(1)))
                .andExpect(jsonPath("$.successCount", is(1)))
                .andExpect(jsonPath("$.failureCount", is(0)))
                .andExpect(jsonPath("$.items[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.items[0].entityType", is("book")))
                .andExpect(jsonPath("$.items[0].entityName", is("The Adventures of Tom Sawyer")));

        // Verify photo was created
        var photos = photoRepository.findByBookIdOrderByPhotoOrder(testBook.getId());
        assertEquals(1, photos.size(), "Book should have 1 photo");
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importFromZip_importsAuthorPhotos() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            byte[] imageBytes = createDummyImage(100, 100);
            zos.putNextEntry(new ZipEntry("author-mark-twain.jpg"));
            zos.write(imageBytes);
            zos.closeEntry();
        }

        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "photos.zip",
                "application/zip",
                baos.toByteArray()
        );

        mockMvc.perform(multipart("/api/photos/import-zip").file(zipFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount", is(1)))
                .andExpect(jsonPath("$.items[0].entityType", is("author")))
                .andExpect(jsonPath("$.items[0].entityName", is("Mark Twain")));

        // Verify photo was created
        var photos = photoRepository.findByAuthorId(testAuthor.getId());
        assertEquals(1, photos.size(), "Author should have 1 photo");
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importFromZip_skipsUnrecognizedFilenames() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            byte[] imageBytes = createDummyImage(100, 100);
            // Valid book photo
            zos.putNextEntry(new ZipEntry("book-the-adventures-of-tom-sawyer.jpg"));
            zos.write(imageBytes);
            zos.closeEntry();
            // Invalid: no prefix
            zos.putNextEntry(new ZipEntry("random-photo.jpg"));
            zos.write(imageBytes);
            zos.closeEntry();
            // Invalid: text file
            zos.putNextEntry(new ZipEntry("readme.txt"));
            zos.write("Hello".getBytes());
            zos.closeEntry();
        }

        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "photos.zip",
                "application/zip",
                baos.toByteArray()
        );

        mockMvc.perform(multipart("/api/photos/import-zip").file(zipFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles", is(3)))
                .andExpect(jsonPath("$.successCount", is(1)))
                .andExpect(jsonPath("$.skippedCount", is(2)));
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importFromZip_failsForUnknownEntity() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            byte[] imageBytes = createDummyImage(100, 100);
            zos.putNextEntry(new ZipEntry("book-nonexistent-book-title.jpg"));
            zos.write(imageBytes);
            zos.closeEntry();
        }

        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "photos.zip",
                "application/zip",
                baos.toByteArray()
        );

        mockMvc.perform(multipart("/api/photos/import-zip").file(zipFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failureCount", is(1)))
                .andExpect(jsonPath("$.items[0].status", is("FAILURE")))
                .andExpect(jsonPath("$.items[0].errorMessage", containsString("No book found")));
    }

    @Test
    @WithMockUser(username = "1")
    void importFromZip_requiresLibrarianAuthority() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "photos.zip",
                "application/zip",
                new byte[10]
        );

        mockMvc.perform(multipart("/api/photos/import-zip").file(zipFile))
                .andExpect(status().isForbidden());
    }

    // ===========================================
    // Round-Trip Test: Export -> Import
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void exportThenImport_roundTrip_preservesPhotos() throws Exception {
        // Step 1: Create some photos
        Photo bookPhoto1 = createBookPhoto(testBook, "Cover", 0);
        Photo bookPhoto2 = createBookPhoto(testBook, "Back", 1);
        Photo authorPhoto = createAuthorPhoto(testAuthor, "Portrait");

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

        // Step 4: Import from the exported ZIP
        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "library-photos.zip",
                "application/zip",
                exportedZipBytes
        );

        mockMvc.perform(multipart("/api/photos/import-zip").file(zipFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount", is(3)))
                .andExpect(jsonPath("$.failureCount", is(0)));

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
    void exportThenImport_preservesImageContent() throws Exception {
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

        // Import
        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "library-photos.zip",
                "application/zip",
                exportedZipBytes
        );

        mockMvc.perform(multipart("/api/photos/import-zip").file(zipFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount", is(1)));

        // Verify checksum matches
        var restoredPhotos = photoRepository.findByBookIdOrderByPhotoOrder(testBook.getId());
        assertEquals(1, restoredPhotos.size());
        assertEquals(originalChecksum, restoredPhotos.get(0).getImageChecksum(),
                "Image checksum should match after round-trip");
    }
}
