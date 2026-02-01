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
import com.muczynski.library.dto.ResumeInfoDto;
import com.muczynski.library.repository.AuthorityRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import com.muczynski.library.repository.PhotoUploadSessionRepository;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.PhotoChunkedImportService;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Autowired
    private PhotoUploadSessionRepository uploadSessionRepository;

    @Autowired
    private PhotoChunkedImportService photoChunkedImportService;

    private Book testBook;
    private Author testAuthor;

    @BeforeEach
    void setUp() {
        // Clean up from previous tests (non-transactional tests don't roll back)
        loanRepository.deleteAll();
        photoRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
        userRepository.deleteAll();

        Authority librarianAuth = com.muczynski.library.TestEntityHelper.findOrCreateAuthority(authorityRepository, "LIBRARIAN");

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
        userRepository.deleteAll();
        uploadSessionRepository.deleteAll();
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
    @WithMockUser(username = "testuser", authorities = {"LIBRARIAN"})
    void testChunkedImport_doesNotChangeBookDateAdded() throws Exception {
        // Set a specific dateAddedToLibrary and record lastModified
        LocalDateTime originalDateAdded = LocalDateTime.of(2020, 6, 15, 10, 0, 0);
        testBook.setDateAddedToLibrary(originalDateAdded);
        testBook = bookRepository.save(testBook);
        LocalDateTime originalLastModified = testBook.getLastModified();

        // Wait a moment to ensure any timestamp changes would be detectable
        Thread.sleep(100);

        byte[] zipData = createTestZip();
        String uploadId = UUID.randomUUID().toString();

        MvcResult result = mockMvc.perform(put("/api/photos/import-zip-chunk")
                        .header("X-Upload-Id", uploadId)
                        .header("X-Chunk-Index", 0)
                        .header("X-Is-Last-Chunk", true)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(zipData))
                .andExpect(status().isOk())
                .andReturn();

        ChunkUploadResultDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ChunkUploadResultDto.class);
        assertTrue(response.isComplete());
        assertTrue(response.getFinalResult().getSuccessCount() >= 2);

        // Verify book dates were NOT changed by photo import
        // Truncate to micros to account for DB precision differences
        Book reloadedBook = bookRepository.findById(testBook.getId()).orElseThrow();
        assertEquals(originalDateAdded, reloadedBook.getDateAddedToLibrary(),
                "Photo import should not change dateAddedToLibrary");
        assertEquals(originalLastModified.truncatedTo(ChronoUnit.MILLIS),
                reloadedBook.getLastModified().truncatedTo(ChronoUnit.MILLIS),
                "Photo import should not change lastModified");
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"LIBRARIAN"})
    void testChunkedImport_photosHaveImageData() throws Exception {
        byte[] zipData = createTestZip();
        String uploadId = UUID.randomUUID().toString();

        mockMvc.perform(put("/api/photos/import-zip-chunk")
                        .header("X-Upload-Id", uploadId)
                        .header("X-Chunk-Index", 0)
                        .header("X-Is-Last-Chunk", true)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(zipData))
                .andExpect(status().isOk());

        // Verify all imported photos have image data
        var photos = photoRepository.findAll();
        assertFalse(photos.isEmpty(), "Should have imported photos");
        for (var photo : photos) {
            assertNotNull(photo.getImage(), "Photo ID " + photo.getId() + " should have image data");
            assertTrue(photo.getImage().length > 0, "Photo ID " + photo.getId() + " image should not be empty");
            assertNotNull(photo.getImageChecksum(), "Photo ID " + photo.getId() + " should have checksum");
        }
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"LIBRARIAN"})
    void testChunkedImport_trailingChunkAfterCleanup() throws Exception {
        byte[] zipData = createTestZip();
        String uploadId = UUID.randomUUID().toString();

        // Send the complete upload as a single chunk
        mockMvc.perform(put("/api/photos/import-zip-chunk")
                        .header("X-Upload-Id", uploadId)
                        .header("X-Chunk-Index", 0)
                        .header("X-Is-Last-Chunk", true)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(zipData))
                .andExpect(status().isOk());

        // Client cleans up the upload session
        mockMvc.perform(delete("/api/photos/import-zip-chunk/" + uploadId))
                .andExpect(status().isNoContent());

        // Send a trailing chunk after cleanup — state is gone
        MvcResult trailingResult = mockMvc.perform(put("/api/photos/import-zip-chunk")
                        .header("X-Upload-Id", uploadId)
                        .header("X-Chunk-Index", 1)
                        .header("X-Is-Last-Chunk", false)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(new byte[]{0}))
                .andExpect(status().isOk())
                .andReturn();

        ChunkUploadResultDto response = objectMapper.readValue(
                trailingResult.getResponse().getContentAsString(), ChunkUploadResultDto.class);

        assertTrue(response.isComplete());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("expired"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"LIBRARIAN"})
    void testChunkedImport_cleanupEndpoint() throws Exception {
        byte[] zipData = createTestZip();
        String uploadId = UUID.randomUUID().toString();

        // Complete an upload
        mockMvc.perform(put("/api/photos/import-zip-chunk")
                        .header("X-Upload-Id", uploadId)
                        .header("X-Chunk-Index", 0)
                        .header("X-Is-Last-Chunk", true)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(zipData))
                .andExpect(status().isOk());

        // Cleanup should succeed
        mockMvc.perform(delete("/api/photos/import-zip-chunk/" + uploadId))
                .andExpect(status().isNoContent());

        // Second cleanup is also fine (idempotent)
        mockMvc.perform(delete("/api/photos/import-zip-chunk/" + uploadId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"LIBRARIAN"})
    void testChunkedImport_resumeInfoEndpoint() throws Exception {
        byte[] zipData = createTestZip();
        String uploadId = UUID.randomUUID().toString();

        // Upload as a single chunk to populate DB session
        mockMvc.perform(put("/api/photos/import-zip-chunk")
                        .header("X-Upload-Id", uploadId)
                        .header("X-Chunk-Index", 0)
                        .header("X-Is-Last-Chunk", true)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(zipData))
                .andExpect(status().isOk());

        // Session is complete, so resume info should return 404
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/photos/import-zip-chunk-resume/" + uploadId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"LIBRARIAN"})
    void testChunkedImport_resumeAfterSessionLoss() throws Exception {
        byte[] zipData = createTestZip();
        String uploadId = UUID.randomUUID().toString();
        int chunkSize = zipData.length / 3 + 1;

        // Send first chunk to start processing and persist state
        byte[] chunk0 = Arrays.copyOfRange(zipData, 0, Math.min(chunkSize, zipData.length));
        MvcResult result0 = mockMvc.perform(put("/api/photos/import-zip-chunk")
                        .header("X-Upload-Id", uploadId)
                        .header("X-Chunk-Index", 0)
                        .header("X-Is-Last-Chunk", false)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(chunk0))
                .andExpect(status().isOk())
                .andReturn();

        ChunkUploadResultDto response0 = objectMapper.readValue(
                result0.getResponse().getContentAsString(), ChunkUploadResultDto.class);
        assertFalse(response0.isComplete());

        // Give background thread time to process entries and persist to DB
        Thread.sleep(2000);

        // Simulate Cloud Run reboot: clear in-memory state
        photoChunkedImportService.removeUpload(uploadId);

        // Verify session was persisted to DB
        var dbSession = uploadSessionRepository.findByUploadId(uploadId);
        assertTrue(dbSession.isPresent(), "Upload session should be persisted in DB");

        // Mark session as not complete so resume works (it was still in progress)
        var session = dbSession.get();
        session.setComplete(false);
        uploadSessionRepository.save(session);

        // Get resume info
        MvcResult resumeResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/photos/import-zip-chunk-resume/" + uploadId))
                .andExpect(status().isOk())
                .andReturn();

        ResumeInfoDto resumeInfo = objectMapper.readValue(
                resumeResult.getResponse().getContentAsString(), ResumeInfoDto.class);

        assertEquals(uploadId, resumeInfo.getUploadId());
        assertTrue(resumeInfo.getTotalProcessed() >= 0);
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"LIBRARIAN"})
    void testChunkedImport_resumeActuallySendsResumedChunks() throws Exception {
        byte[] zipData = createTestZip();
        String uploadId = UUID.randomUUID().toString();

        // === First attempt: send entire ZIP as non-last chunk, let it process, then reboot ===
        sendChunk(uploadId, 0, false, zipData, -1, 0);
        Thread.sleep(2000); // Let background thread process entries and persist to DB
        simulateReboot(uploadId);

        // === Resume #1: resend all data, but interrupt after first chunk again ===
        ResumeInfoDto resumeInfo1 = getResumeInfo(uploadId);
        int processed1 = resumeInfo1.getTotalProcessed();
        assertTrue(processed1 > 0, "Should have processed some entries before first reboot");

        // Resume by resending the full ZIP as non-last, then reboot again
        sendChunk(uploadId, 0, false, zipData, processed1, resumeInfo1.getBytesToSkipInChunk());
        Thread.sleep(2000);
        simulateReboot(uploadId);

        // === Resume #2: resend all data and complete ===
        ResumeInfoDto resumeInfo2 = getResumeInfo(uploadId);
        int processed2 = resumeInfo2.getTotalProcessed();
        assertTrue(processed2 >= processed1,
                "Second resume should have at least as many processed (" + processed2 + " >= " + processed1 + ")");

        // Send full ZIP as last chunk to complete
        ChunkUploadResultDto lastResponse = sendChunk(uploadId, 0, true, zipData,
                processed2, resumeInfo2.getBytesToSkipInChunk());

        if (lastResponse.getErrorMessage() != null) {
            assertFalse(lastResponse.getErrorMessage().contains("Pipe closed"),
                    "Resume #2 should not fail with 'Pipe closed': " + lastResponse.getErrorMessage());
        }

        assertTrue(lastResponse.isComplete(), "Upload should complete after second resume");
        assertEquals(3, lastResponse.getTotalProcessedSoFar(),
                "All 3 ZIP entries should be processed across resumes");
    }

    /** Send a single chunk and assert 200. Returns the parsed response. */
    private ChunkUploadResultDto sendChunk(String uploadId, int chunkIndex, boolean isLast,
                                            byte[] chunk, int resumeFromProcessed, long bytesToSkip) throws Exception {
        var reqBuilder = put("/api/photos/import-zip-chunk")
                .header("X-Upload-Id", uploadId)
                .header("X-Chunk-Index", chunkIndex)
                .header("X-Is-Last-Chunk", isLast)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(chunk);

        if (resumeFromProcessed >= 0) {
            reqBuilder = reqBuilder
                    .header("X-Resume-From-Processed", resumeFromProcessed)
                    .header("X-Bytes-To-Skip", bytesToSkip);
        }

        MvcResult result = mockMvc.perform(reqBuilder).andReturn();
        assertEquals(200, result.getResponse().getStatus(),
                "Chunk " + chunkIndex + " should succeed. Response: "
                        + result.getResponse().getContentAsString());
        return objectMapper.readValue(result.getResponse().getContentAsString(), ChunkUploadResultDto.class);
    }

    /** Simulate a Cloud Run reboot: remove in-memory state and mark DB session incomplete. */
    private void simulateReboot(String uploadId) {
        photoChunkedImportService.removeUpload(uploadId);
        uploadSessionRepository.findByUploadId(uploadId).ifPresent(session -> {
            session.setComplete(false);
            uploadSessionRepository.save(session);
        });
    }

    /** Fetch resume info and assert it's available. */
    private ResumeInfoDto getResumeInfo(String uploadId) throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/photos/import-zip-chunk-resume/" + uploadId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), ResumeInfoDto.class);
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"LIBRARIAN"})
    void testChunkedImport_resumeInfoNotFound() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/photos/import-zip-chunk-resume/nonexistent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    void testChunkedImport_forbiddenForNonLibrarian() throws Exception {
        byte[] zipData = createTestZip();
        String uploadId = UUID.randomUUID().toString();

        mockMvc.perform(put("/api/photos/import-zip-chunk")
                        .header("X-Upload-Id", uploadId)
                        .header("X-Chunk-Index", 0)
                        .header("X-Is-Last-Chunk", true)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(zipData))
                .andExpect(status().isForbidden());
    }
}
