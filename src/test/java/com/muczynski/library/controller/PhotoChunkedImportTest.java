/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Authority;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.ChunkUploadResultDto;
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
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for chunked photo ZIP import functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhotoChunkedImportTest {

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

    @MockitoBean
    private GooglePhotosService googlePhotosService;

    @MockitoBean
    private GooglePhotosLibraryClient googlePhotosLibraryClient;

    private Book testBook;
    private Author testAuthor;

    @BeforeEach
    void setUp() {
        Authority librarianAuth = new Authority();
        librarianAuth.setName("LIBRARIAN");
        librarianAuth = authorityRepository.save(librarianAuth);

        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("{bcrypt}$2a$10$...");
        testUser.setAuthorities(Set.of(librarianAuth));
        userRepository.save(testUser);

        testAuthor = new Author();
        testAuthor.setName("Mark Twain");
        testAuthor = authorRepository.save(testAuthor);

        testBook = new Book();
        testBook.setTitle("The Adventures of Tom Sawyer");
        testBook.setAuthor(testAuthor);
        testBook = bookRepository.save(testBook);
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

    private byte[] createTestZip() throws Exception {
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipBytes)) {
            // Book photo
            byte[] img1 = createDummyImage(100, 100);
            zos.putNextEntry(new ZipEntry("book-The Adventures of Tom Sawyer.jpg"));
            zos.write(img1);
            zos.closeEntry();

            // Author photo
            byte[] img2 = createDummyImage(120, 120);
            zos.putNextEntry(new ZipEntry("author-Mark Twain.jpg"));
            zos.write(img2);
            zos.closeEntry();

            // Second book photo at order 2
            byte[] img3 = createDummyImage(140, 140);
            zos.putNextEntry(new ZipEntry("book-The Adventures of Tom Sawyer-2.jpg"));
            zos.write(img3);
            zos.closeEntry();
        }
        return zipBytes.toByteArray();
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"LIBRARIAN"})
    void testChunkedImport_singleChunk() throws Exception {
        byte[] zipData = createTestZip();
        String uploadId = UUID.randomUUID().toString();

        MvcResult result = mockMvc.perform(put("/api/photos/import-zip-chunk")
                        .header("X-Upload-Id", uploadId)
                        .header("X-Chunk-Index", 0)
                        .header("X-Total-Size", zipData.length)
                        .header("X-Is-Last-Chunk", true)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(zipData))
                .andExpect(status().isOk())
                .andReturn();

        ChunkUploadResultDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ChunkUploadResultDto.class);

        assertTrue(response.isComplete());
        assertNotNull(response.getFinalResult());
        assertEquals(3, response.getFinalResult().getTotalFiles());
        assertTrue(response.getFinalResult().getSuccessCount() >= 2);

        // Verify photos saved to DB
        assertTrue(photoRepository.count() >= 2);
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"LIBRARIAN"})
    void testChunkedImport_multipleChunks() throws Exception {
        byte[] zipData = createTestZip();
        String uploadId = UUID.randomUUID().toString();
        int chunkSize = zipData.length / 3 + 1; // Split into ~3 chunks

        ChunkUploadResultDto lastResponse = null;
        int chunkIndex = 0;

        for (int offset = 0; offset < zipData.length; offset += chunkSize) {
            int end = Math.min(offset + chunkSize, zipData.length);
            byte[] chunk = Arrays.copyOfRange(zipData, offset, end);
            boolean isLast = end >= zipData.length;

            MvcResult result = mockMvc.perform(put("/api/photos/import-zip-chunk")
                            .header("X-Upload-Id", uploadId)
                            .header("X-Chunk-Index", chunkIndex)
                            .header("X-Total-Size", zipData.length)
                            .header("X-Is-Last-Chunk", isLast)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .content(chunk))
                    .andExpect(status().isOk())
                    .andReturn();

            lastResponse = objectMapper.readValue(
                    result.getResponse().getContentAsString(), ChunkUploadResultDto.class);

            if (!isLast) {
                assertFalse(lastResponse.isComplete());
                assertNull(lastResponse.getFinalResult());
            }

            chunkIndex++;
        }

        assertNotNull(lastResponse);
        assertTrue(lastResponse.isComplete());
        assertNotNull(lastResponse.getFinalResult());
        assertEquals(3, lastResponse.getFinalResult().getTotalFiles());

        // Verify photos saved to DB
        assertTrue(photoRepository.count() >= 2);
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    void testChunkedImport_forbiddenForNonLibrarian() throws Exception {
        byte[] zipData = createTestZip();
        String uploadId = UUID.randomUUID().toString();

        mockMvc.perform(put("/api/photos/import-zip-chunk")
                        .header("X-Upload-Id", uploadId)
                        .header("X-Chunk-Index", 0)
                        .header("X-Total-Size", zipData.length)
                        .header("X-Is-Last-Chunk", true)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(zipData))
                .andExpect(status().isForbidden());
    }
}
