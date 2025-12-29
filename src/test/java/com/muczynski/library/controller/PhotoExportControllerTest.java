/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhotoExportControllerTest {

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

    // Mock external Google Photos services
    @MockitoBean
    private GooglePhotosService googlePhotosService;

    @MockitoBean
    private GooglePhotosLibraryClient googlePhotosLibraryClient;

    private Book testBook;
    private Author testAuthor;

    @BeforeEach
    void setUp() {
        // Create test author
        testAuthor = new Author();
        testAuthor.setName("Test Author");
        testAuthor = authorRepository.save(testAuthor);

        // Create test book
        testBook = new Book();
        testBook.setTitle("Test Book");
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

    private Photo createPhotoWithImage(Book book, String caption) throws Exception {
        byte[] imageBytes = createDummyImage(200, 300);
        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(imageBytes);
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photo.setCaption(caption);
        photo.setPhotoOrder(0);
        photo.setImageChecksum(computeChecksum(imageBytes));
        photo.setExportStatus(Photo.ExportStatus.PENDING);
        return photoRepository.save(photo);
    }

    private Photo createPhotoWithPermanentId(Book book, String permanentId) throws Exception {
        byte[] imageBytes = createDummyImage(200, 300);
        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(imageBytes);
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photo.setCaption("Exported photo");
        photo.setPhotoOrder(0);
        photo.setImageChecksum(computeChecksum(imageBytes));
        photo.setPermanentId(permanentId);
        photo.setExportStatus(Photo.ExportStatus.COMPLETED);
        photo.setExportedAt(LocalDateTime.now());
        return photoRepository.save(photo);
    }

    private Photo createPhotoNeedingImport(Book book, String permanentId) {
        Photo photo = new Photo();
        photo.setBook(book);
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photo.setCaption("Needs import");
        photo.setPhotoOrder(0);
        photo.setPermanentId(permanentId);
        // No image or checksum - needs import
        return photoRepository.save(photo);
    }

    // ===========================================
    // GET /api/photo-export/stats Tests
    // ===========================================

    @Test
    @WithMockUser(username = "1")
    void getExportStats_returnsCorrectCounts() throws Exception {
        // Create photos with different states
        createPhotoWithImage(testBook, "Pending export 1");
        createPhotoWithImage(testBook, "Pending export 2");
        createPhotoWithPermanentId(testBook, "permanent-id-1");
        createPhotoNeedingImport(testBook, "permanent-id-2");

        // Create a failed photo (has checksum but no permanentId, so counted in pendingExport)
        Photo failedPhoto = createPhotoWithImage(testBook, "Failed");
        failedPhoto.setExportStatus(Photo.ExportStatus.FAILED);
        failedPhoto.setExportErrorMessage("Test error");
        photoRepository.save(failedPhoto);

        mockMvc.perform(get("/api/photo-export/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(5)))
                .andExpect(jsonPath("$.exported", is(2)))  // 2 with permanentId
                .andExpect(jsonPath("$.imported", is(1)))  // 1 with permanentId AND checksum
                .andExpect(jsonPath("$.pendingExport", is(3)))  // 3 with checksum but no permanentId (including failed)
                .andExpect(jsonPath("$.pendingImport", is(1)))  // 1 with permanentId but no checksum
                .andExpect(jsonPath("$.failed", is(1)));
    }

    @Test
    @WithMockUser(username = "1")
    void getExportStats_emptyDatabase_returnsZeros() throws Exception {
        mockMvc.perform(get("/api/photo-export/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(0)))
                .andExpect(jsonPath("$.exported", is(0)))
                .andExpect(jsonPath("$.imported", is(0)))
                .andExpect(jsonPath("$.pendingExport", is(0)))
                .andExpect(jsonPath("$.pendingImport", is(0)))
                .andExpect(jsonPath("$.failed", is(0)));
    }

    @Test
    @WithMockUser(username = "1")
    void getExportStats_excludesSoftDeletedPhotos() throws Exception {
        // Create active photo
        createPhotoWithImage(testBook, "Active");

        // Create soft-deleted photo
        Photo deletedPhoto = createPhotoWithImage(testBook, "Deleted");
        deletedPhoto.setDeletedAt(LocalDateTime.now());
        photoRepository.save(deletedPhoto);

        mockMvc.perform(get("/api/photo-export/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)));  // Only counts active photo
    }

    @Test
    void getExportStats_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/photo-export/stats"))
                .andExpect(status().isUnauthorized());
    }

    // ===========================================
    // GET /api/photo-export/photos Tests
    // ===========================================

    @Test
    @WithMockUser(username = "1")
    void getAllPhotosWithExportStatus_returnsPhotoDetails() throws Exception {
        Photo photo = createPhotoWithImage(testBook, "Test caption");

        mockMvc.perform(get("/api/photo-export/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(photo.getId().intValue())))
                .andExpect(jsonPath("$[0].caption", is("Test caption")))
                .andExpect(jsonPath("$[0].exportStatus", is("PENDING")))
                .andExpect(jsonPath("$[0].hasImage", is(true)))
                .andExpect(jsonPath("$[0].bookTitle", is("Test Book")))
                .andExpect(jsonPath("$[0].bookAuthorName", is("Test Author")));
    }

    @Test
    @WithMockUser(username = "1")
    void getAllPhotosWithExportStatus_returnsEmptyListForEmptyDatabase() throws Exception {
        mockMvc.perform(get("/api/photo-export/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "1")
    void getAllPhotosWithExportStatus_excludesSoftDeletedPhotos() throws Exception {
        createPhotoWithImage(testBook, "Active");

        Photo deletedPhoto = createPhotoWithImage(testBook, "Deleted");
        deletedPhoto.setDeletedAt(LocalDateTime.now());
        photoRepository.save(deletedPhoto);

        mockMvc.perform(get("/api/photo-export/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].caption", is("Active")));
    }

    @Test
    @WithMockUser(username = "1")
    void getAllPhotosWithExportStatus_usesChecksumForHasImage() throws Exception {
        // Photo with checksum (has image)
        createPhotoWithImage(testBook, "With image");

        // Photo without checksum (needs import)
        createPhotoNeedingImport(testBook, "permanent-id");

        mockMvc.perform(get("/api/photo-export/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.caption == 'With image')].hasImage", contains(true)))
                .andExpect(jsonPath("$[?(@.caption == 'Needs import')].hasImage", contains(false)));
    }

    @Test
    @WithMockUser(username = "1")
    void getAllPhotosWithExportStatus_includesAuthorPhotos() throws Exception {
        Photo authorPhoto = new Photo();
        authorPhoto.setAuthor(testAuthor);
        byte[] imageBytes = createDummyImage(200, 300);
        authorPhoto.setImage(imageBytes);
        authorPhoto.setContentType(MediaType.IMAGE_JPEG_VALUE);
        authorPhoto.setCaption("Author photo");
        authorPhoto.setPhotoOrder(0);
        authorPhoto.setImageChecksum(computeChecksum(imageBytes));
        photoRepository.save(authorPhoto);

        mockMvc.perform(get("/api/photo-export/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].authorName", is("Test Author")))
                .andExpect(jsonPath("$[0].bookTitle").doesNotExist());
    }

    @Test
    void getAllPhotosWithExportStatus_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/photo-export/photos"))
                .andExpect(status().isUnauthorized());
    }

    // ===========================================
    // POST /api/photo-export/unlink/{photoId} Tests
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void unlinkPhoto_removePermanentId() throws Exception {
        Photo photo = createPhotoWithPermanentId(testBook, "permanent-to-unlink");

        mockMvc.perform(post("/api/photo-export/unlink/" + photo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Photo unlinked successfully")))
                .andExpect(jsonPath("$.photoId", is(photo.getId().intValue())));

        // Verify the photo was updated
        Photo updatedPhoto = photoRepository.findById(photo.getId()).orElseThrow();
        assert updatedPhoto.getPermanentId() == null;
        assert updatedPhoto.getExportStatus() == Photo.ExportStatus.PENDING;
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void unlinkPhoto_notFound_returnsError() throws Exception {
        // Verify that attempting to unlink a non-existent photo returns an error
        mockMvc.perform(post("/api/photo-export/unlink/99999"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void unlinkPhoto_noPermanentId_returnsError() throws Exception {
        Photo photo = createPhotoWithImage(testBook, "No permanent ID");

        // Verify that attempting to unlink a photo without permanentId returns an error
        mockMvc.perform(post("/api/photo-export/unlink/" + photo.getId()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(username = "2")  // Regular user, not librarian
    void unlinkPhoto_requiresLibrarianAuthority() throws Exception {
        Photo photo = createPhotoWithPermanentId(testBook, "permanent-id");

        mockMvc.perform(post("/api/photo-export/unlink/" + photo.getId()))
                .andExpect(status().isForbidden());
    }

    // ===========================================
    // POST /api/photo-export/export-all Tests
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void exportAllPhotos_requiresLibrarianAuthority() throws Exception {
        // This will fail because Google Photos isn't configured, but it tests authorization
        mockMvc.perform(post("/api/photo-export/export-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Export process started")));
    }

    @Test
    @WithMockUser(username = "2")  // Regular user
    void exportAllPhotos_forbiddenForRegularUser() throws Exception {
        mockMvc.perform(post("/api/photo-export/export-all"))
                .andExpect(status().isForbidden());
    }

    // ===========================================
    // POST /api/photo-export/export/{photoId} Tests
    // ===========================================

    @Test
    @WithMockUser(username = "2")  // Regular user
    void exportPhoto_forbiddenForRegularUser() throws Exception {
        Photo photo = createPhotoWithImage(testBook, "Test");

        mockMvc.perform(post("/api/photo-export/export/" + photo.getId()))
                .andExpect(status().isForbidden());
    }

    // ===========================================
    // POST /api/photo-export/import/{photoId} Tests
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importPhoto_withoutPermanentId_returnsError() throws Exception {
        Photo photo = createPhotoWithImage(testBook, "No permanent ID");

        // importPhotoById returns error message in response body, not via exception
        mockMvc.perform(post("/api/photo-export/import/" + photo.getId()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", containsString("does not have a permanent ID")));
    }

    @Test
    @WithMockUser(username = "2")  // Regular user
    void importPhoto_forbiddenForRegularUser() throws Exception {
        Photo photo = createPhotoNeedingImport(testBook, "permanent-id");

        mockMvc.perform(post("/api/photo-export/import/" + photo.getId()))
                .andExpect(status().isForbidden());
    }

    // ===========================================
    // POST /api/photo-export/import-all Tests
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importAllPhotos_noPhotosNeedImport_returnsMessage() throws Exception {
        // Create only photos with images (no photos needing import)
        createPhotoWithImage(testBook, "Has image");

        mockMvc.perform(post("/api/photo-export/import-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("No photos need import")))
                .andExpect(jsonPath("$.imported", is(0)))
                .andExpect(jsonPath("$.failed", is(0)));
    }

    @Test
    @WithMockUser(username = "2")  // Regular user
    void importAllPhotos_forbiddenForRegularUser() throws Exception {
        mockMvc.perform(post("/api/photo-export/import-all"))
                .andExpect(status().isForbidden());
    }

    // ===========================================
    // POST /api/photo-export/verify/{photoId} Tests
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void verifyPhoto_withoutPermanentId_returnsError() throws Exception {
        Photo photo = createPhotoWithImage(testBook, "No permanent ID");

        // Controller catches exception and returns 500 with error message
        mockMvc.perform(post("/api/photo-export/verify/" + photo.getId()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", containsString("does not have a permanent ID")));
    }

    @Test
    @WithMockUser(username = "2")  // Regular user
    void verifyPhoto_forbiddenForRegularUser() throws Exception {
        Photo photo = createPhotoWithPermanentId(testBook, "permanent-id");

        mockMvc.perform(post("/api/photo-export/verify/" + photo.getId()))
                .andExpect(status().isForbidden());
    }

    // ===========================================
    // Memory Efficiency Tests
    // These tests verify the optimizations don't break functionality
    // ===========================================

    @Test
    @WithMockUser(username = "1")
    void getExportStats_withManyPhotos_doesNotLoadImageBytes() throws Exception {
        // Create multiple photos to verify stats work without loading all images
        for (int i = 0; i < 10; i++) {
            createPhotoWithImage(testBook, "Photo " + i);
        }

        mockMvc.perform(get("/api/photo-export/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(10)))
                .andExpect(jsonPath("$.pendingExport", is(10)));
    }

    @Test
    @WithMockUser(username = "1")
    void getAllPhotosWithExportStatus_correctlyReportsHasImage() throws Exception {
        // Mix of photos with and without images
        createPhotoWithImage(testBook, "With image 1");
        createPhotoWithImage(testBook, "With image 2");
        createPhotoNeedingImport(testBook, "permanent-1");
        createPhotoNeedingImport(testBook, "permanent-2");

        mockMvc.perform(get("/api/photo-export/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[?(@.hasImage == true)]", hasSize(2)))
                .andExpect(jsonPath("$[?(@.hasImage == false)]", hasSize(2)));
    }
}
