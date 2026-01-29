// (c) Copyright 2025 by Muczynski
package com.muczynski.library.controller;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.BranchRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.GooglePhotosService;
import com.muczynski.library.photostorage.client.GooglePhotosLibraryClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test that imports a full branch archive, assigns photos to all books,
 * exports as ZIP, and verifies all photos are included.
 * Skips if branch-archive.json does not exist.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIf("archiveFileExists")
class PhotoExportFullArchiveTest {

    static boolean archiveFileExists() {
        return Files.exists(Path.of("branch-archive.json"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private GooglePhotosService googlePhotosService;

    @MockitoBean
    private GooglePhotosLibraryClient googlePhotosLibraryClient;

    @BeforeEach
    void setUp() {
        // Clean slate
        loanRepository.deleteAll();
        photoRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
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
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private Photo createBookPhoto(Book book, int order) throws Exception {
        byte[] imageBytes = createDummyImage(100 + order, 150 + order);
        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(imageBytes);
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photo.setCaption("Photo " + order + " for " + book.getTitle());
        photo.setPhotoOrder(order);
        photo.setImageChecksum(computeChecksum(imageBytes));
        photo.setExportStatus(Photo.ExportStatus.PENDING);
        return photoRepository.save(photo);
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void exportZip_containsAllPhotos_afterArchiveImport() throws Exception {
        // 1. Import branch-archive.json (strip loans to avoid missing user errors)
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode archiveNode = (ObjectNode) mapper.readTree(Files.readString(Path.of("branch-archive.json")));
        archiveNode.putArray("loans"); // clear loans
        archiveNode.putArray("photos"); // clear metadata-only photos
        String archiveJson = mapper.writeValueAsString(archiveNode);
        MvcResult importResult = mockMvc.perform(post("/api/import/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(archiveJson))
                .andReturn();
        assertEquals(200, importResult.getResponse().getStatus(),
                "Import should succeed: " + importResult.getResponse().getContentAsString());

        // 2. Assign a photo to every book
        List<Book> allBooks = bookRepository.findAll();
        assertFalse(allBooks.isEmpty(), "Should have imported books");

        // Delete any metadata-only photos from the import (they have no image data)
        photoRepository.deleteAll();

        int expectedPhotoCount = 0;
        for (Book book : allBooks) {
            createBookPhoto(book, 0);
            expectedPhotoCount++;
        }

        // 3. Add 2nd and 3rd photos to the first two books
        if (allBooks.size() >= 2) {
            createBookPhoto(allBooks.get(0), 1);
            createBookPhoto(allBooks.get(0), 2);
            createBookPhoto(allBooks.get(1), 1);
            createBookPhoto(allBooks.get(1), 2);
            expectedPhotoCount += 4;
        }

        // Verify all photos are in the database
        long dbPhotoCount = photoRepository.count();
        assertEquals(expectedPhotoCount, dbPhotoCount,
                "Database should have all created photos");

        // Verify all photos have image data
        long activeWithImages = photoRepository.findActivePhotoIdsWithImages().size();
        assertEquals(expectedPhotoCount, activeWithImages,
                "All photos should have image data");

        // 4. Export as ZIP
        MvcResult result = mockMvc.perform(get("/api/photo-export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andReturn();

        byte[] zipBytes = result.getResponse().getContentAsByteArray();
        assertTrue(zipBytes.length > 0, "ZIP file should not be empty");

        // 5. Count photos in the ZIP
        int zipFileCount = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                zipFileCount++;
            }
        }

        assertEquals(expectedPhotoCount, zipFileCount,
                "ZIP should contain all " + expectedPhotoCount + " photos, but only has " + zipFileCount);
    }
}
