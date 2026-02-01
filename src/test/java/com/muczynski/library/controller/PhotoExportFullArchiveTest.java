// (c) Copyright 2025 by Muczynski
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.ChunkUploadResultDto;
import com.muczynski.library.dto.PhotoZipImportResultDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test that imports a full branch archive, assigns photos to all books,
 * exports as ZIP, and verifies all photos are included. Then re-imports the ZIP
 * via the chunked upload endpoint and verifies the round-trip.
 * Skips if branch-archive.json does not exist.
 *
 * The export is streamed to a temp file in 1MB chunks to avoid OutOfMemory errors.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@EnabledIf("archiveFileExists")
class PhotoExportFullArchiveTest {

    private static final Logger log = LoggerFactory.getLogger(PhotoExportFullArchiveTest.class);
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks

    static boolean archiveFileExists() {
        return Files.exists(Path.of("branch-archive.json"));
    }

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
        // Clean slate - use deleteAllInBatch to avoid loading entities into memory
        loanRepository.deleteAllInBatch();
        photoRepository.deleteAllInBatch();
        bookRepository.deleteAllInBatch();
        authorRepository.deleteAllInBatch();
    }

    @AfterEach
    void tearDown() {
        loanRepository.deleteAllInBatch();
        photoRepository.deleteAllInBatch();
        bookRepository.deleteAllInBatch();
        authorRepository.deleteAllInBatch();
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
        byte[] imageBytes = createDummyImage(1000 + order, 1500 + order);
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

    /**
     * Count entries in a ZIP file by streaming from disk.
     */
    private int countZipEntries(Path zipFile) throws IOException {
        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile.toFile())))) {
            while (zis.getNextEntry() != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Import a ZIP file via the chunked upload endpoint, reading from disk in CHUNK_SIZE pieces.
     * Returns the final ChunkUploadResultDto (with complete=true and finalResult).
     */
    private ChunkUploadResultDto importZipChunkedFromFile(Path zipFile) throws Exception {
        long totalSize = Files.size(zipFile);
        String uploadId = UUID.randomUUID().toString();
        int totalChunks = (int) Math.ceil((double) totalSize / CHUNK_SIZE);
        double totalMb = totalSize / (1024.0 * 1024.0);

        log.info("=== Chunked ZIP Import: {} MB in {} chunk(s) of {} KB ===",
                String.format("%.2f", totalMb), totalChunks, CHUNK_SIZE / 1024);

        ChunkUploadResultDto lastResponse = null;
        int chunkIndex = 0;
        byte[] buffer = new byte[CHUNK_SIZE];

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(zipFile.toFile()))) {
            long bytesSent = 0;
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                byte[] chunk = (bytesRead == CHUNK_SIZE) ? buffer : Arrays.copyOf(buffer, bytesRead);
                bytesSent += bytesRead;
                boolean isLast = bytesSent >= totalSize;
                double sentMb = bytesSent / (1024.0 * 1024.0);

                long startMs = System.currentTimeMillis();

                MvcResult result = mockMvc.perform(put("/api/photos/import-zip-chunk")
                                .header("X-Upload-Id", uploadId)
                                .header("X-Chunk-Index", chunkIndex)
                                .header("X-Is-Last-Chunk", isLast)
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .content(chunk))
                        .andExpect(status().isOk())
                        .andReturn();

                long elapsedMs = System.currentTimeMillis() - startMs;

                lastResponse = objectMapper.readValue(
                        result.getResponse().getContentAsString(), ChunkUploadResultDto.class);

                int newPhotos = lastResponse.getProcessedPhotos() != null
                        ? lastResponse.getProcessedPhotos().size() : 0;

                log.info("  Chunk {}/{} | {} / {} MB ({}%) | {} ms | +{} photos | total: {} processed, {} success, {} failed, {} skipped{}",
                        String.format("%3d", chunkIndex + 1), totalChunks,
                        String.format("%6.2f", sentMb), String.format("%.2f", totalMb),
                        String.format("%3.0f", (bytesSent * 100.0) / totalSize),
                        String.format("%4d", elapsedMs),
                        newPhotos,
                        lastResponse.getTotalProcessedSoFar(),
                        lastResponse.getTotalSuccessSoFar(),
                        lastResponse.getTotalFailureSoFar(),
                        lastResponse.getTotalSkippedSoFar(),
                        isLast ? "  [LAST]" : "");

                chunkIndex++;
            }
        }

        assertNotNull(lastResponse);
        assertTrue(lastResponse.isComplete(), "Final chunk response should be complete");
        assertNotNull(lastResponse.getFinalResult(), "Final chunk should have finalResult");

        PhotoZipImportResultDto finalResult = lastResponse.getFinalResult();
        log.info("=== Import complete: {} files, {} success, {} failed, {} skipped ===",
                finalResult.getTotalFiles(), finalResult.getSuccessCount(),
                finalResult.getFailureCount(), finalResult.getSkippedCount());

        return lastResponse;
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void exportZip_containsAllPhotos_afterArchiveImport() throws Exception {
        // 1. Import branch-archive.json (strip loans to avoid missing user errors)
        log.info("[Step 1] Importing branch-archive.json...");
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
        log.info("[Step 1] Archive imported successfully.");

        // 2. Assign a photo to every book
        List<Book> allBooks = bookRepository.findAll();
        assertFalse(allBooks.isEmpty(), "Should have imported books");
        log.info("[Step 2] Creating photos for {} books...", allBooks.size());

        // Delete any metadata-only photos from the import (they have no image data)
        photoRepository.deleteAllInBatch();

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
        log.info("[Step 2] Created {} photos total ({} books + 4 extra for first two books).",
                expectedPhotoCount, allBooks.size());

        // Verify all photos are in the database
        long dbPhotoCount = photoRepository.count();
        assertEquals(expectedPhotoCount, dbPhotoCount,
                "Database should have all created photos");

        // Verify all photos have image data
        long activeWithImages = photoRepository.findActivePhotoIdsWithImages().size();
        assertEquals(expectedPhotoCount, activeWithImages,
                "All photos should have image data");
        log.info("[Step 2] Verified: {} photos in DB, {} with image data.", dbPhotoCount, activeWithImages);

        // 4. Export as ZIP - write directly to temp file to avoid holding full ZIP in memory
        log.info("[Step 3] Exporting photos as ZIP...");
        Path tempZipFile = Files.createTempFile("photo-export-test-", ".zip");
        try {
            MvcResult result = mockMvc.perform(get("/api/photo-export"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/zip"))
                    .andReturn();

            // Write response to temp file immediately
            byte[] responseBytes = result.getResponse().getContentAsByteArray();
            Files.write(tempZipFile, responseBytes);
            responseBytes = null; // Allow GC to reclaim

            long zipSize = Files.size(tempZipFile);
            assertTrue(zipSize > 0, "ZIP file should not be empty");
            log.info("[Step 3] ZIP exported to temp file: {} MB.", String.format("%.2f", zipSize / (1024.0 * 1024.0)));

            // 5. Count photos in the ZIP by streaming from file
            int zipFileCount = countZipEntries(tempZipFile);
            log.info("[Step 4] ZIP contains {} files (expected {}).", zipFileCount, expectedPhotoCount);

            assertEquals(expectedPhotoCount, zipFileCount,
                    "ZIP should contain all " + expectedPhotoCount + " photos, but only has " + zipFileCount);

            // 6. Delete all photos and re-import via chunked upload from file
            log.info("[Step 5] Deleting all photos from DB...");
            photoRepository.deleteAllInBatch();
            assertEquals(0, photoRepository.count(), "All photos should be deleted before re-import");

            log.info("[Step 6] Re-importing ZIP via chunked upload from file...");
            ChunkUploadResultDto chunkedResult = importZipChunkedFromFile(tempZipFile);
            PhotoZipImportResultDto finalResult = chunkedResult.getFinalResult();

            assertEquals(expectedPhotoCount, finalResult.getSuccessCount(),
                    "Chunked import should succeed for all " + expectedPhotoCount + " photos");
            assertEquals(0, finalResult.getFailureCount(),
                    "Chunked import should have no failures");

            // 7. Verify all photos restored to correct books
            long restoredCount = photoRepository.count();
            assertEquals(expectedPhotoCount, restoredCount,
                    "All " + expectedPhotoCount + " photos should be restored after chunked import");
            log.info("[Step 7] Verified: {} photos restored in DB.", restoredCount);

            // Verify first two books still have 3 photos each (if applicable)
            if (allBooks.size() >= 2) {
                var book0Photos = photoRepository.findByBookIdOrderByPhotoOrder(allBooks.get(0).getId());
                assertEquals(3, book0Photos.size(),
                        "First book should have 3 photos after round-trip");
                var book1Photos = photoRepository.findByBookIdOrderByPhotoOrder(allBooks.get(1).getId());
                assertEquals(3, book1Photos.size(),
                        "Second book should have 3 photos after round-trip");
                log.info("[Step 7] Book '{}': {} photos. Book '{}': {} photos.",
                        allBooks.get(0).getTitle(), book0Photos.size(),
                        allBooks.get(1).getTitle(), book1Photos.size());
            }

            // 8. Re-import the same ZIP a second time - all should be skipped as duplicates
            log.info("[Step 8] Re-importing ZIP a second time (should all be skipped as duplicates)...");
            ChunkUploadResultDto secondImportResult = importZipChunkedFromFile(tempZipFile);
            PhotoZipImportResultDto secondFinalResult = secondImportResult.getFinalResult();

            assertEquals(expectedPhotoCount, secondFinalResult.getTotalFiles(),
                    "Second import should receive all " + expectedPhotoCount + " photos");
            assertEquals(0, secondFinalResult.getSuccessCount(),
                    "Second import should have 0 new photos (all duplicates)");
            assertEquals(expectedPhotoCount, secondFinalResult.getSkippedCount(),
                    "Second import should skip all " + expectedPhotoCount + " photos as duplicates");
            assertEquals(0, secondFinalResult.getFailureCount(),
                    "Second import should have no failures");

            // Verify photo count unchanged after second import
            long countAfterSecondImport = photoRepository.count();
            assertEquals(expectedPhotoCount, countAfterSecondImport,
                    "Photo count should be unchanged after second import");
            log.info("[Step 8] Verified: second import received {} photos, {} skipped, {} new, DB count unchanged at {}.",
                    secondFinalResult.getTotalFiles(), secondFinalResult.getSkippedCount(),
                    secondFinalResult.getSuccessCount(), countAfterSecondImport);

            log.info("[PASS] Full archive export -> chunked import round-trip succeeded.");

        } finally {
            // Clean up temp file
            Files.deleteIfExists(tempZipFile);
        }
    }
}
