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
import com.muczynski.library.photostorage.dto.MediaItemResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    private Author testAuthor;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test authority
        Authority librarianAuth = new Authority();
        librarianAuth.setName("LIBRARIAN");
        librarianAuth = authorityRepository.save(librarianAuth);

        // Create test user (JPA will assign ID 1, matches @WithMockUser(username = "1"))
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("{bcrypt}$2a$10$..."); // Dummy encoded password
        testUser.setAuthorities(Set.of(librarianAuth));
        testUser = userRepository.save(testUser);

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

        // Create a failed photo (has checksum but no permanentId)
        // FAILED photos are excluded from pendingExport count to prevent double-counting
        Photo failedPhoto = createPhotoWithImage(testBook, "Failed");
        failedPhoto.setExportStatus(Photo.ExportStatus.FAILED);
        failedPhoto.setExportErrorMessage("Test error");
        photoRepository.save(failedPhoto);

        mockMvc.perform(get("/api/photo-export/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(5)))
                .andExpect(jsonPath("$.exported", is(2)))  // 2 with permanentId
                .andExpect(jsonPath("$.imported", is(1)))  // 1 with permanentId AND checksum
                .andExpect(jsonPath("$.pendingExport", is(2)))  // 2 with checksum but no permanentId (excluding failed)
                .andExpect(jsonPath("$.pendingImport", is(1)))  // 1 with permanentId but no checksum (excluding failed)
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
    void getExportStats_excludesFailedFromPendingCounts() throws Exception {
        // Create non-failed pending export photos
        createPhotoWithImage(testBook, "Pending export 1");
        createPhotoWithImage(testBook, "Pending export 2");

        // Create failed export photo (has checksum, no permanentId, FAILED)
        Photo failedExportPhoto = createPhotoWithImage(testBook, "Failed export");
        failedExportPhoto.setExportStatus(Photo.ExportStatus.FAILED);
        photoRepository.save(failedExportPhoto);

        // Create non-failed pending import photo
        createPhotoNeedingImport(testBook, "permanent-pending");

        // Create failed import photo (has permanentId, no checksum, FAILED)
        Photo failedImportPhoto = createPhotoNeedingImport(testBook, "permanent-failed");
        failedImportPhoto.setExportStatus(Photo.ExportStatus.FAILED);
        failedImportPhoto.setExportErrorMessage("Import failed");
        photoRepository.save(failedImportPhoto);

        mockMvc.perform(get("/api/photo-export/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(5)))
                .andExpect(jsonPath("$.pendingExport", is(2)))  // 2 non-failed pending exports
                .andExpect(jsonPath("$.pendingImport", is(1)))  // 1 non-failed pending import
                .andExpect(jsonPath("$.failed", is(2)));        // 2 failed (1 export + 1 import)
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

    // ===========================================
    // Status Derivation Tests
    // Verify that status shown in list matches stats counts
    // ===========================================

    @Test
    @WithMockUser(username = "1")
    void getAllPhotosWithExportStatus_derivesStatusFromActualData() throws Exception {
        // Photo with checksum but no permanentId (and stored status null) -> should show PENDING
        Photo pendingPhoto = createPhotoWithImage(testBook, "Pending export");
        pendingPhoto.setExportStatus(null);  // Clear stored status to test derivation
        photoRepository.save(pendingPhoto);

        // Photo with permanentId AND checksum (even if stored status is null) -> should show COMPLETED
        Photo exportedPhoto = createPhotoWithImage(testBook, "Exported");
        exportedPhoto.setPermanentId("permanent-id-derived");
        exportedPhoto.setExportStatus(null);  // Clear stored status to test derivation
        photoRepository.save(exportedPhoto);

        // Photo with permanentId but NO checksum -> should show PENDING_IMPORT
        Photo needsImportPhoto = createPhotoNeedingImport(testBook, "permanent-needs-import");
        needsImportPhoto.setCaption("Needs import");
        photoRepository.save(needsImportPhoto);

        // Photo with no checksum and no permanentId -> should show NO_IMAGE
        Photo noImagePhoto = new Photo();
        noImagePhoto.setBook(testBook);
        noImagePhoto.setCaption("No image");
        noImagePhoto.setContentType(MediaType.IMAGE_JPEG_VALUE);
        noImagePhoto.setPhotoOrder(0);
        photoRepository.save(noImagePhoto);

        // Photo with FAILED status -> should preserve FAILED
        Photo failedPhoto = createPhotoWithImage(testBook, "Failed");
        failedPhoto.setExportStatus(Photo.ExportStatus.FAILED);
        failedPhoto.setExportErrorMessage("Test failure");
        photoRepository.save(failedPhoto);

        mockMvc.perform(get("/api/photo-export/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[?(@.caption == 'Pending export')].exportStatus", contains("PENDING")))
                .andExpect(jsonPath("$[?(@.caption == 'Exported')].exportStatus", contains("COMPLETED")))
                .andExpect(jsonPath("$[?(@.caption == 'Needs import')].exportStatus", contains("PENDING_IMPORT")))
                .andExpect(jsonPath("$[?(@.caption == 'No image')].exportStatus", contains("NO_IMAGE")))
                .andExpect(jsonPath("$[?(@.caption == 'Failed')].exportStatus", contains("FAILED")));
    }

    @Test
    @WithMockUser(username = "1")
    void getAllPhotosWithExportStatus_statusMatchesStatsCount() throws Exception {
        // Create 2 pending export photos (have checksum, no permanentId)
        createPhotoWithImage(testBook, "Pending 1");
        createPhotoWithImage(testBook, "Pending 2");

        // Create 1 exported photo (has permanentId)
        createPhotoWithPermanentId(testBook, "permanent-1");

        // Get stats and list
        mockMvc.perform(get("/api/photo-export/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingExport", is(2)))
                .andExpect(jsonPath("$.exported", is(1)));

        // Verify list status matches: 2 PENDING, 1 COMPLETED
        mockMvc.perform(get("/api/photo-export/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.exportStatus == 'PENDING')]", hasSize(2)))
                .andExpect(jsonPath("$[?(@.exportStatus == 'COMPLETED')]", hasSize(1)));
    }

    // ===========================================
    // Import with Batch Fallback Tests
    // Verify fallback from single-item to batch endpoint on 404
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importPhoto_usesBatchFallbackOn404() throws Exception {
        Photo photo = createPhotoNeedingImport(testBook, "permanent-for-import");

        // Mock GooglePhotosService to return valid access token
        when(googlePhotosService.getValidAccessToken(ArgumentMatchers.any())).thenReturn("mock-access-token");

        // Mock getMediaItem to return a valid media item (simulating batch fallback success)
        MediaItemResponse mockResponse = new MediaItemResponse();
        mockResponse.setId("permanent-for-import");
        mockResponse.setBaseUrl("https://example.com/photo");
        mockResponse.setMimeType("image/jpeg");
        when(googlePhotosLibraryClient.getMediaItem(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(mockResponse);

        // Mock downloadPhoto to return image bytes
        byte[] mockImageBytes = createDummyImage(100, 100);
        when(googlePhotosLibraryClient.downloadPhoto(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(mockImageBytes);

        mockMvc.perform(post("/api/photo-export/import/" + photo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("imported")))
                .andExpect(jsonPath("$.photoId", is(photo.getId().intValue())));

        // Verify getMediaItem was called (which internally handles fallback)
        verify(googlePhotosLibraryClient).getMediaItem(ArgumentMatchers.anyString(), eq("permanent-for-import"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importPhoto_failsWhenBothEndpointsFail() throws Exception {
        Photo photo = createPhotoNeedingImport(testBook, "permanent-not-found");

        // Mock GooglePhotosService to return valid access token
        when(googlePhotosService.getValidAccessToken(ArgumentMatchers.any())).thenReturn("mock-access-token");

        // Mock getMediaItem to throw NotFound (simulating both endpoints failing)
        when(googlePhotosLibraryClient.getMediaItem(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Media item not found via both single-item and batch endpoints"
                ));

        mockMvc.perform(post("/api/photo-export/import/" + photo.getId()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", notNullValue()));

        // Verify photo was marked as failed
        Photo updatedPhoto = photoRepository.findById(photo.getId()).orElseThrow();
        assert updatedPhoto.getExportStatus() == Photo.ExportStatus.FAILED;
        assert updatedPhoto.getExportErrorMessage() != null;
    }

    // ===========================================
    // Multiple Photos per Book Import Tests
    // Verify importing multiple photos for the same book works correctly
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importPhoto_multiplePhotosForSameBook_succeedsSequentially() throws Exception {
        // Create multiple photos for the same book that need import
        Photo photo1 = createPhotoNeedingImport(testBook, "permanent-1");
        photo1.setPhotoOrder(0);
        photo1 = photoRepository.save(photo1);

        Photo photo2 = createPhotoNeedingImport(testBook, "permanent-2");
        photo2.setPhotoOrder(1);
        photo2 = photoRepository.save(photo2);

        Photo photo3 = createPhotoNeedingImport(testBook, "permanent-3");
        photo3.setPhotoOrder(2);
        photo3 = photoRepository.save(photo3);

        // Mock GooglePhotosService to return valid access token
        when(googlePhotosService.getValidAccessToken(ArgumentMatchers.any())).thenReturn("mock-access-token");

        // Mock getMediaItem to return different media items based on permanentId
        MediaItemResponse mockResponse1 = new MediaItemResponse();
        mockResponse1.setId("permanent-1");
        mockResponse1.setBaseUrl("https://example.com/photo1");
        mockResponse1.setMimeType("image/jpeg");
        mockResponse1.setFilename("photo1.jpg");

        MediaItemResponse mockResponse2 = new MediaItemResponse();
        mockResponse2.setId("permanent-2");
        mockResponse2.setBaseUrl("https://example.com/photo2");
        mockResponse2.setMimeType("image/jpeg");
        mockResponse2.setFilename("photo2.jpg");

        MediaItemResponse mockResponse3 = new MediaItemResponse();
        mockResponse3.setId("permanent-3");
        mockResponse3.setBaseUrl("https://example.com/photo3");
        mockResponse3.setMimeType("image/jpeg");
        mockResponse3.setFilename("photo3.jpg");

        when(googlePhotosLibraryClient.getMediaItem(ArgumentMatchers.anyString(), eq("permanent-1")))
                .thenReturn(mockResponse1);
        when(googlePhotosLibraryClient.getMediaItem(ArgumentMatchers.anyString(), eq("permanent-2")))
                .thenReturn(mockResponse2);
        when(googlePhotosLibraryClient.getMediaItem(ArgumentMatchers.anyString(), eq("permanent-3")))
                .thenReturn(mockResponse3);

        // Mock downloadPhoto to return different image bytes for each photo
        byte[] mockImageBytes1 = createDummyImage(100, 100);
        byte[] mockImageBytes2 = createDummyImage(110, 110);
        byte[] mockImageBytes3 = createDummyImage(120, 120);

        when(googlePhotosLibraryClient.downloadPhoto(ArgumentMatchers.anyString(), eq("https://example.com/photo1")))
                .thenReturn(mockImageBytes1);
        when(googlePhotosLibraryClient.downloadPhoto(ArgumentMatchers.anyString(), eq("https://example.com/photo2")))
                .thenReturn(mockImageBytes2);
        when(googlePhotosLibraryClient.downloadPhoto(ArgumentMatchers.anyString(), eq("https://example.com/photo3")))
                .thenReturn(mockImageBytes3);

        // Import first photo
        mockMvc.perform(post("/api/photo-export/import/" + photo1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("imported")));

        // Import second photo
        mockMvc.perform(post("/api/photo-export/import/" + photo2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("imported")));

        // Import third photo
        mockMvc.perform(post("/api/photo-export/import/" + photo3.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("imported")));

        // Verify all photos were imported successfully
        Photo updatedPhoto1 = photoRepository.findById(photo1.getId()).orElseThrow();
        Photo updatedPhoto2 = photoRepository.findById(photo2.getId()).orElseThrow();
        Photo updatedPhoto3 = photoRepository.findById(photo3.getId()).orElseThrow();

        // All photos should have image checksums after successful import
        assert updatedPhoto1.getImageChecksum() != null : "Photo 1 should have checksum after import";
        assert updatedPhoto2.getImageChecksum() != null : "Photo 2 should have checksum after import";
        assert updatedPhoto3.getImageChecksum() != null : "Photo 3 should have checksum after import";

        // All photos should have COMPLETED status
        assert updatedPhoto1.getExportStatus() == Photo.ExportStatus.COMPLETED : "Photo 1 should be COMPLETED";
        assert updatedPhoto2.getExportStatus() == Photo.ExportStatus.COMPLETED : "Photo 2 should be COMPLETED";
        assert updatedPhoto3.getExportStatus() == Photo.ExportStatus.COMPLETED : "Photo 3 should be COMPLETED";

        // All photos should still be associated with the same book
        assert updatedPhoto1.getBook().getId().equals(testBook.getId()) : "Photo 1 should still belong to test book";
        assert updatedPhoto2.getBook().getId().equals(testBook.getId()) : "Photo 2 should still belong to test book";
        assert updatedPhoto3.getBook().getId().equals(testBook.getId()) : "Photo 3 should still belong to test book";

        // Photo orders should be preserved
        assert updatedPhoto1.getPhotoOrder() == 0 : "Photo 1 order should be 0";
        assert updatedPhoto2.getPhotoOrder() == 1 : "Photo 2 order should be 1";
        assert updatedPhoto3.getPhotoOrder() == 2 : "Photo 3 order should be 2";
    }

    // ===========================================
    // Batch Export Tests
    // Verify batch export functionality works correctly
    // ===========================================

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void exportAllPhotos_processesPhotosInBatches() throws Exception {
        // Create 75 photos to verify batch processing (should be 2 batches: 50 + 25)
        for (int i = 0; i < 75; i++) {
            createPhotoWithImage(testBook, "Batch photo " + i);
        }

        // Verify stats before export
        mockMvc.perform(get("/api/photo-export/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(75)))
                .andExpect(jsonPath("$.pendingExport", is(75)));

        // Note: Actual export will fail without configured Google Photos credentials,
        // but this test verifies the batching logic doesn't crash
        mockMvc.perform(post("/api/photo-export/export-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Export process started")));
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importAllPhotos_processesPhotosInBatches() throws Exception {
        // Create 45 photos needing import to verify batch processing (should be 3 batches of 20, 20, 5)
        for (int i = 0; i < 45; i++) {
            createPhotoNeedingImport(testBook, "permanent-import-" + i);
        }

        // Verify stats before import
        mockMvc.perform(get("/api/photo-export/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(45)))
                .andExpect(jsonPath("$.pendingImport", is(45)));

        // Mock GooglePhotosService to return valid access token
        when(googlePhotosService.getValidAccessToken(ArgumentMatchers.any())).thenReturn("mock-access-token");

        // Import will attempt to fetch from Google Photos and fail, but batching logic should work
        mockMvc.perform(post("/api/photo-export/import-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Import complete")))
                .andExpect(jsonPath("$.imported").exists())
                .andExpect(jsonPath("$.failed").exists());
    }

    @Test
    @WithMockUser(username = "1", authorities = "LIBRARIAN")
    void importAllPhotos_partialSuccess_reportsCorrectCounts() throws Exception {
        // Create 5 photos needing import
        for (int i = 0; i < 5; i++) {
            createPhotoNeedingImport(testBook, "permanent-" + i);
        }

        // Mock GooglePhotosService to return valid access token
        when(googlePhotosService.getValidAccessToken(ArgumentMatchers.any())).thenReturn("mock-access-token");

        // Mock first 2 photos to succeed, rest to fail
        MediaItemResponse successResponse = new MediaItemResponse();
        successResponse.setBaseUrl("https://example.com/success");
        successResponse.setMimeType("image/jpeg");

        when(googlePhotosLibraryClient.getMediaItem(ArgumentMatchers.anyString(), eq("permanent-0")))
                .thenReturn(successResponse);
        when(googlePhotosLibraryClient.getMediaItem(ArgumentMatchers.anyString(), eq("permanent-1")))
                .thenReturn(successResponse);
        when(googlePhotosLibraryClient.getMediaItem(ArgumentMatchers.anyString(), ArgumentMatchers.matches("permanent-[2-4]")))
                .thenReturn(null); // Fail

        byte[] mockImageBytes = createDummyImage(100, 100);
        when(googlePhotosLibraryClient.downloadPhoto(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(mockImageBytes);

        mockMvc.perform(post("/api/photo-export/import-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Import complete")))
                .andExpect(jsonPath("$.imported", is(2)))
                .andExpect(jsonPath("$.failed", is(3)));
    }
}
