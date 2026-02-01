/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.PhotoExportInfoDto;
import com.muczynski.library.dto.PhotoExportStatsDto;
import com.muczynski.library.dto.PhotoImportResultDto;
import com.muczynski.library.dto.PhotoVerifyResultDto;
import com.muczynski.library.photostorage.client.GooglePhotosLibraryClient;
import com.muczynski.library.photostorage.dto.AlbumResponse;
import com.muczynski.library.photostorage.dto.BatchCreateRequest;
import com.muczynski.library.photostorage.dto.BatchCreateResponse;
import com.muczynski.library.photostorage.dto.SearchResponse;
import com.muczynski.library.repository.BranchRepository;
import com.muczynski.library.repository.PhotoRepository;
import com.muczynski.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PhotoExportService {

    private static final Logger logger = LoggerFactory.getLogger(PhotoExportService.class);

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository libraryRepository;

    @Autowired
    private com.muczynski.library.repository.BookRepository bookRepository;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private GooglePhotosService googlePhotosService;

    @Autowired
    private GooglePhotosLibraryClient photosLibraryClient;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${APP_ENV:production}")
    private String appEnv;

    // Cache the album name to avoid repeated database lookups
    private String cachedAlbumName = null;

    /**
     * Compute SHA-256 checksum of image bytes
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
            logger.error("SHA-256 algorithm not available", e);
            return null;
        }
    }

    /**
     * Export photos to Google Photos using batch operations
     * Can be triggered manually via API endpoint
     * Processes photos in batches of up to 50 (Google Photos API limit)
     */
    @Transactional
    public void exportPhotos() {
        logger.info("Starting photo export process...");

        try {
            // Get the librarian user (assuming they have Google Photos configured)
            Optional<User> librarianOpt = userRepository.findByUsernameIgnoreCase("librarian");

            if (librarianOpt.isEmpty()) {
                logger.warn("No librarian user found for photo export");
                return;
            }

            User librarian = librarianOpt.get();

            // Check if Google Photos is configured
            if (librarian.getGooglePhotosApiKey() == null || librarian.getGooglePhotosApiKey().trim().isEmpty()) {
                logger.warn("Google Photos not configured for librarian user. Skipping export.");
                return;
            }

            // Find photos that need to be backed up
            List<Photo> photosToExport = findPhotosNeedingExport();

            if (photosToExport.isEmpty()) {
                logger.info("No photos need export at this time");
                return;
            }

            logger.info("Found {} photos to export", photosToExport.size());

            int successCount = 0;
            int failureCount = 0;

            // Process photos in batches of 50 (Google Photos API limit)
            final int BATCH_SIZE = 50;
            for (int i = 0; i < photosToExport.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, photosToExport.size());
                List<Photo> batch = photosToExport.subList(i, endIndex);

                logger.info("Processing batch {}-{} of {}", i + 1, endIndex, photosToExport.size());

                try {
                    int batchSuccess = exportPhotosBatch(batch, librarian);
                    successCount += batchSuccess;
                    failureCount += (batch.size() - batchSuccess);
                } catch (Exception e) {
                    logger.error("Failed to export batch {}-{}: {}", i + 1, endIndex, e.getMessage(), e);
                    // Mark all photos in failed batch as failed
                    for (Photo photo : batch) {
                        markPhotoAsFailed(photo, "Batch export failed: " + e.getMessage());
                    }
                    failureCount += batch.size();
                }
            }

            logger.info("Photo export complete. Success: {}, Failed: {}", successCount, failureCount);

        } catch (Exception e) {
            logger.error("Error during scheduled photo export", e);
        }
    }

    /**
     * Export a batch of photos to Google Photos in a single batch operation
     * @param photos List of photos to export (max 50)
     * @param user User with Google Photos credentials
     * @return Number of successfully exported photos
     */
    @Transactional
    public int exportPhotosBatch(List<Photo> photos, User user) {
        if (photos.isEmpty()) {
            return 0;
        }

        logger.info("Exporting batch of {} photos to Google Photos", photos.size());

        // Mark all photos as in progress
        for (Photo photo : photos) {
            photo.setExportStatus(Photo.ExportStatus.IN_PROGRESS);
        }
        photoRepository.saveAll(photos);

        try {
            // Step 1: Upload all photo bytes and collect upload tokens
            List<String> uploadTokens = new ArrayList<>();
            List<Photo> successfulUploads = new ArrayList<>();

            for (Photo photo : photos) {
                try {
                    String uploadToken = uploadPhotoBytes(photo.getImage(), photo.getContentType(), user);
                    uploadTokens.add(uploadToken);
                    successfulUploads.add(photo);
                } catch (Exception e) {
                    logger.error("Failed to upload photo ID {}: {}", photo.getId(), e.getMessage(), e);
                    markPhotoAsFailed(photo, "Upload failed: " + e.getMessage());
                }
            }

            if (uploadTokens.isEmpty()) {
                logger.warn("No photos successfully uploaded in this batch");
                return 0;
            }

            logger.info("Successfully uploaded {} of {} photos, creating media items...", uploadTokens.size(), photos.size());

            // Step 2: Create all media items in a single batch request
            List<BatchCreateRequest.NewMediaItem> newMediaItems = new ArrayList<>();
            for (int i = 0; i < successfulUploads.size(); i++) {
                Photo photo = successfulUploads.get(i);
                String uploadToken = uploadTokens.get(i);

                String description = buildPhotoDescription(photo);

                BatchCreateRequest.NewMediaItem newMediaItem = new BatchCreateRequest.NewMediaItem();
                newMediaItem.setDescription(description);

                BatchCreateRequest.SimpleMediaItem simpleMediaItem = new BatchCreateRequest.SimpleMediaItem();
                simpleMediaItem.setUploadToken(uploadToken);
                newMediaItem.setSimpleMediaItem(simpleMediaItem);

                newMediaItems.add(newMediaItem);
            }

            // Get valid access token
            String accessToken = googlePhotosService.getValidAccessToken(user);

            // Verify token scopes
            logger.info("Verifying token scopes before batch create operation...");
            googlePhotosService.verifyAccessTokenScopes(accessToken);

            // Get or create the library album
            String albumId = getOrCreateAlbum(user);

            // Create media items in batch
            BatchCreateResponse response = photosLibraryClient.batchCreate(accessToken, albumId, newMediaItems);

            // Step 3: Process results and update photo records
            int successCount = 0;
            if (response.getNewMediaItemResults() != null) {
                for (int i = 0; i < response.getNewMediaItemResults().size(); i++) {
                    BatchCreateResponse.NewMediaItemResult result = response.getNewMediaItemResults().get(i);
                    Photo photo = successfulUploads.get(i);

                    // Check status - code 0 or null means success
                    if (result.getStatus() != null && result.getStatus().getCode() != null
                            && result.getStatus().getCode() != 0) {
                        String errorMsg = "Create failed with code " + result.getStatus().getCode() + ": "
                                + result.getStatus().getMessage();
                        logger.error("Failed to create media item for photo ID {}: {}", photo.getId(), errorMsg);
                        markPhotoAsFailed(photo, errorMsg);
                    } else if (result.getMediaItem() != null && result.getMediaItem().getId() != null) {
                        String permanentId = result.getMediaItem().getId();
                        photo.setPermanentId(permanentId);
                        photo.setExportStatus(Photo.ExportStatus.COMPLETED);
                        photo.setExportedAt(LocalDateTime.now());
                        photo.setExportErrorMessage(null);
                        photoRepository.save(photo);
                        successCount++;
                        logger.debug("Successfully exported photo ID {} with permanent ID: {}",
                                photo.getId(), permanentId);
                    } else {
                        logger.error("No media item ID in response for photo ID {}", photo.getId());
                        markPhotoAsFailed(photo, "No media item ID in response");
                    }
                }
            }

            logger.info("Batch export completed: {} of {} photos succeeded", successCount, successfulUploads.size());
            return successCount;

        } catch (Exception e) {
            logger.error("Batch export failed: {}", e.getMessage(), e);
            // Mark remaining in-progress photos as failed
            for (Photo photo : photos) {
                if (photo.getExportStatus() == Photo.ExportStatus.IN_PROGRESS) {
                    markPhotoAsFailed(photo, "Batch export failed: " + e.getMessage());
                }
            }
            throw e;
        }
    }

    /**
     * Find photos that need to be backed up
     * Uses efficient ID-based query to avoid loading image bytes during filtering
     */
    private List<Photo> findPhotosNeedingExport() {
        // Get IDs of photos needing export without loading image bytes
        List<Long> photoIds = photoRepository.findIdsNeedingExport();

        if (photoIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Load photos one at a time for export processing
        List<Photo> photosToExport = new ArrayList<>();
        for (Long photoId : photoIds) {
            photoRepository.findById(photoId).ifPresent(photosToExport::add);
        }

        return photosToExport;
    }

    /**
     * Export a single photo to Google Photos
     */
    @Transactional
    public void exportPhoto(Photo photo, User user) {
        logger.info("Backing up photo ID: {}", photo.getId());

        // Mark as in progress
        photo.setExportStatus(Photo.ExportStatus.IN_PROGRESS);
        photoRepository.save(photo);

        try {
            // Step 1: Upload the raw bytes to get an upload token
            String uploadToken = uploadPhotoBytes(photo.getImage(), photo.getContentType(), user);

            // Step 2: Create a media item with the upload token
            String permanentId = createMediaItem(uploadToken, photo, user);

            // Step 3: Mark as completed
            photo.setPermanentId(permanentId);
            photo.setExportStatus(Photo.ExportStatus.COMPLETED);
            photo.setExportedAt(LocalDateTime.now());
            photo.setExportErrorMessage(null);
            photoRepository.save(photo);

            logger.info("Successfully backed up photo ID: {} with permanent ID: {}", photo.getId(), permanentId);

        } catch (Exception e) {
            logger.error("Failed to export photo ID: {}", photo.getId(), e);
            throw e;
        }
    }

    /**
     * Upload photo bytes to Google Photos and get an upload token
     */
    private String uploadPhotoBytes(byte[] imageBytes, String contentType, User user) {
        logger.debug("Uploading photo bytes ({} bytes) to Google Photos", imageBytes.length);

        // Get valid access token
        String accessToken = googlePhotosService.getValidAccessToken(user);

        // Verify token scopes before upload
        logger.info("Verifying token scopes before upload operation...");
        googlePhotosService.verifyAccessTokenScopes(accessToken);

        // Use the photostorage client for upload
        return photosLibraryClient.uploadBytes(accessToken, imageBytes, contentType);
    }

    /**
     * Create a media item in Google Photos using the upload token
     */
    private String createMediaItem(String uploadToken, Photo photo, User user) {
        logger.debug("Creating media item in Google Photos");

        // Get valid access token
        String accessToken = googlePhotosService.getValidAccessToken(user);

        // Verify token scopes before attempting batchCreate
        logger.info("Verifying token scopes before batchCreate operation...");
        googlePhotosService.verifyAccessTokenScopes(accessToken);

        // Get or create the library album
        String albumId = getOrCreateAlbum(user);

        // Build description from photo caption and associated book/author
        String description = buildPhotoDescription(photo);

        // Create the new media item request using photostorage DTOs
        BatchCreateRequest.NewMediaItem newMediaItem = new BatchCreateRequest.NewMediaItem();
        newMediaItem.setDescription(description);

        BatchCreateRequest.SimpleMediaItem simpleMediaItem = new BatchCreateRequest.SimpleMediaItem();
        simpleMediaItem.setUploadToken(uploadToken);
        newMediaItem.setSimpleMediaItem(simpleMediaItem);

        // Use the photostorage client for batch create
        BatchCreateResponse response = photosLibraryClient.batchCreate(
                accessToken,
                albumId,
                Collections.singletonList(newMediaItem)
        );

        // Extract the permanent media item ID
        if (response.getNewMediaItemResults() != null && !response.getNewMediaItemResults().isEmpty()) {
            BatchCreateResponse.NewMediaItemResult result = response.getNewMediaItemResults().get(0);

            // Check status first - code 0 means success
            if (result.getStatus() != null) {
                Integer statusCode = result.getStatus().getCode();
                String statusMessage = result.getStatus().getMessage();
                logger.info("Batch create status - code: {}, message: {}", statusCode, statusMessage);

                if (statusCode != null && statusCode != 0) {
                    logger.error("Batch create failed with status code: {}, message: {}", statusCode, statusMessage);
                    throw new LibraryException("Batch create failed: " + statusMessage);
                }
            }

            if (result.getMediaItem() != null && result.getMediaItem().getId() != null) {
                String permanentId = result.getMediaItem().getId();
                logger.info("Successfully created media item with ID: {}", permanentId);
                return permanentId;
            } else {
                logger.error("Media item created but no ID in response");
                throw new LibraryException("No media item ID in response");
            }
        } else {
            logger.error("No media item results in response");
            throw new LibraryException("No media item results in response");
        }
    }

    /**
     * Get the album name based on library name and environment
     */
    private String getAlbumName() {
        if (cachedAlbumName != null) {
            return cachedAlbumName;
        }

        // Get the first library from the database
        List<Library> libraries = libraryRepository.findAll();
        String libraryName = "Library";

        if (!libraries.isEmpty()) {
            libraryName = libraries.get(0).getBranchName();
        } else {
            logger.warn("No library found in database, using default name: {}", libraryName);
        }

        // Prefix with "test-" if in staging environment
        if ("staging".equalsIgnoreCase(appEnv)) {
            libraryName = "test-" + libraryName;
            logger.info("Staging environment detected. Using album name: {}", libraryName);
        } else {
            logger.info("Production environment. Using album name: {}", libraryName);
        }

        cachedAlbumName = libraryName;
        return cachedAlbumName;
    }

    /**
     * Get or create the library album in Google Photos
     */
    private String getOrCreateAlbum(User user) {
        // Check if user has a saved album ID
        if (user.getGooglePhotosAlbumId() != null && !user.getGooglePhotosAlbumId().trim().isEmpty()) {
            logger.info("Using saved album ID from user settings: {}", user.getGooglePhotosAlbumId());
            return user.getGooglePhotosAlbumId();
        }

        // No saved album ID, need to create a new album
        String albumName = getAlbumName();
        logger.info("No saved album ID found. Creating new album: {}", albumName);

        // Get valid access token
        String accessToken = googlePhotosService.getValidAccessToken(user);

        // Verify token scopes before creating album
        logger.info("Verifying token scopes before album creation...");
        googlePhotosService.verifyAccessTokenScopes(accessToken);

        // Create the album
        logger.info("Creating new Google Photos album: '{}'", albumName);
        String albumId = createAlbum(albumName, accessToken);

        // Save the album ID to the user for future use
        logger.info("Saving album ID '{}' to user settings", albumId);
        user.setGooglePhotosAlbumId(albumId);
        userRepository.save(user);

        return albumId;
    }

    /**
     * Create a new album in Google Photos
     */
    private String createAlbum(String title, String accessToken) {
        logger.info("Creating new album: {}", title);

        // Use the photostorage client to create the album
        AlbumResponse response = photosLibraryClient.createAlbum(accessToken, title);

        String albumId = response.getAlbum().getId();
        logger.info("Successfully created album '{}' with ID: {}", title, albumId);

        return albumId;
    }

    /**
     * Build a descriptive caption for the photo
     */
    private String buildPhotoDescription(Photo photo) {
        StringBuilder description = new StringBuilder();

        if (photo.getCaption() != null && !photo.getCaption().trim().isEmpty()) {
            description.append(photo.getCaption());
        }

        if (photo.getBook() != null) {
            if (description.length() > 0) {
                description.append(" - ");
            }
            description.append("Book: ").append(photo.getBook().getTitle());
            if (photo.getBook().getAuthor() != null) {
                description.append(" by ").append(photo.getBook().getAuthor().getName());
            }
        } else if (photo.getAuthor() != null) {
            if (description.length() > 0) {
                description.append(" - ");
            }
            description.append("Author: ").append(photo.getAuthor().getName());
        }

        if (description.length() == 0) {
            description.append("Library photo #").append(photo.getId());
        }

        return description.toString();
    }

    /**
     * Get file extension from content type
     */
    private String getFileExtension(String contentType) {
        if (contentType == null) {
            return ".jpg";
        }

        if (contentType.contains("jpeg")) {
            return ".jpg";
        } else if (contentType.contains("png")) {
            return ".png";
        } else if (contentType.contains("gif")) {
            return ".gif";
        } else if (contentType.contains("webp")) {
            return ".webp";
        } else {
            return ".jpg";
        }
    }

    /**
     * Mark a photo as failed
     */
    @Transactional
    private void markPhotoAsFailed(Photo photo, String errorMessage) {
        photo.setExportStatus(Photo.ExportStatus.FAILED);
        photo.setExportErrorMessage(errorMessage != null ? errorMessage.substring(0, Math.min(255, errorMessage.length())) : "Unknown error");
        photoRepository.save(photo);
    }

    /**
     * Get export statistics
     * Uses efficient COUNT queries to avoid loading image bytes into memory
     */
    @Transactional(readOnly = true)
    public PhotoExportStatsDto getExportStats() {
        PhotoExportStatsDto stats = new PhotoExportStatsDto();

        try {
            // Use efficient COUNT queries that don't load image bytes
            long total = photoRepository.countActivePhotos();
            long exported = photoRepository.countExportedPhotos();
            long imported = photoRepository.countImportedPhotos();
            long pendingExport = photoRepository.countPendingExportPhotos();
            long pendingImport = photoRepository.countPendingImportPhotos();
            long failed = photoRepository.countByExportStatus(Photo.ExportStatus.FAILED);
            long inProgress = photoRepository.countByExportStatus(Photo.ExportStatus.IN_PROGRESS);

            stats.setTotal(total);
            stats.setExported(exported);
            stats.setImported(imported);
            stats.setPendingExport(pendingExport);
            stats.setPendingImport(pendingImport);
            stats.setFailed(failed);
            stats.setInProgress(inProgress);

            // Keep legacy fields for backwards compatibility
            stats.setCompleted(exported);
            stats.setPending(pendingExport);
        } catch (Exception e) {
            logger.error("Failed to retrieve photo statistics from database", e);
            // Return safe defaults
            stats.setTotal(0L);
            stats.setExported(0L);
            stats.setImported(0L);
            stats.setPendingExport(0L);
            stats.setPendingImport(0L);
            stats.setFailed(0L);
            stats.setInProgress(0L);
            stats.setCompleted(0L);
            stats.setPending(0L);
        }

        // Add album information
        try {
            String albumName = getAlbumName();
            stats.setAlbumName(albumName);
        } catch (Exception e) {
            logger.error("Failed to retrieve album name", e);
            stats.setAlbumName("Library");
        }

        // Get album ID from librarian user settings
        try {
            Optional<User> librarianOpt = userRepository.findByUsernameIgnoreCase("librarian");
            if (librarianOpt.isPresent()) {
                String albumId = librarianOpt.get().getGooglePhotosAlbumId();
                stats.setAlbumId(albumId != null && !albumId.trim().isEmpty() ? albumId : null);
            } else {
                stats.setAlbumId(null);
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve album ID from librarian user", e);
            stats.setAlbumId(null);
        }

        return stats;
    }

    /**
     * Get all photos with their export status
     * Uses PhotoMetadataProjection to avoid loading image bytes and prevent OutOfMemory errors
     */
    @Transactional(readOnly = true)
    public List<PhotoExportInfoDto> getAllPhotosWithExportStatus() {
        logger.debug("Fetching all photo metadata (without image bytes)");
        List<com.muczynski.library.repository.PhotoMetadataProjection> allPhotos = photoRepository.findBy();
        logger.info("Found {} photos in database", allPhotos.size());

        List<PhotoExportInfoDto> result = new ArrayList<>();

        for (com.muczynski.library.repository.PhotoMetadataProjection photo : allPhotos) {
            // Skip soft-deleted photos
            if (photo.getDeletedAt() != null) {
                continue;
            }
            try {
                PhotoExportInfoDto photoInfo = new PhotoExportInfoDto();
                photoInfo.setId(photo.getId());
                photoInfo.setCaption(photo.getCaption());
                // Derive status from actual data to match stats counts exactly
                // Priority: FAILED/IN_PROGRESS from stored status, then derive from permanentId/imageChecksum
                String derivedStatus;
                boolean hasPermanentId = photo.getPermanentId() != null && !photo.getPermanentId().isEmpty();
                boolean hasChecksum = photo.getImageChecksum() != null;

                if (photo.getExportStatus() == Photo.ExportStatus.FAILED) {
                    derivedStatus = "FAILED";
                } else if (photo.getExportStatus() == Photo.ExportStatus.IN_PROGRESS) {
                    derivedStatus = "IN_PROGRESS";
                } else if (hasPermanentId && hasChecksum) {
                    // Has both permanentId and checksum = fully synced (exported and imported)
                    derivedStatus = "COMPLETED";
                } else if (hasPermanentId && !hasChecksum) {
                    // Has permanentId but no checksum = needs import from Google Photos
                    derivedStatus = "PENDING_IMPORT";
                } else if (hasChecksum && !hasPermanentId) {
                    // Has checksum but no permanentId = needs export to Google Photos
                    derivedStatus = "PENDING";
                } else {
                    // No image and no permanentId = no data to export
                    derivedStatus = "NO_IMAGE";
                }
                photoInfo.setExportStatus(derivedStatus);
                photoInfo.setExportedAt(photo.getExportedAt());
                photoInfo.setPermanentId(photo.getPermanentId());
                photoInfo.setExportErrorMessage(photo.getExportErrorMessage());
                photoInfo.setContentType(photo.getContentType());
                // Use imageChecksum as proxy for hasImage to avoid loading image bytes
                photoInfo.setHasImage(photo.getImageChecksum() != null);
                photoInfo.setChecksum(photo.getImageChecksum());

                if (photo.getBook() != null) {
                    photoInfo.setBookTitle(photo.getBook().getTitle());
                    photoInfo.setBookId(photo.getBook().getId());
                    photoInfo.setBookLocNumber(photo.getBook().getLocNumber());
                    // Use dateAddedToLibrary from projection (no need to load full Book entity)
                    photoInfo.setBookDateAdded(photo.getBook().getDateAddedToLibrary());
                    // Include book's author if available
                    if (photo.getBook().getAuthor() != null) {
                        photoInfo.setBookAuthorName(photo.getBook().getAuthor().getName());
                    }
                }

                if (photo.getAuthor() != null) {
                    photoInfo.setAuthorName(photo.getAuthor().getName());
                    photoInfo.setAuthorId(photo.getAuthor().getId());
                }

                result.add(photoInfo);
            } catch (Exception e) {
                logger.error("Error processing photo ID: {} - Error: {}", photo.getId(), e.getMessage(), e);
                // Continue processing other photos even if one fails
            }
        }

        // Sort by book dateAdded (descending, most recent first) to group photos of the same book together
        // Photos without books will appear at the end
        result.sort((p1, p2) -> {
            LocalDateTime date1 = p1.getBookDateAdded();
            LocalDateTime date2 = p2.getBookDateAdded();

            // Handle null values - put photos without books at the end
            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;  // p1 goes after p2
            if (date2 == null) return -1; // p1 goes before p2

            // Both dates exist - compare in descending order (most recent first)
            return date2.compareTo(date1);
        });

        logger.info("Successfully processed {} photos for display", result.size());
        return result;
    }

    /**
     * Manually trigger export for a specific photo
     */
    @Transactional
    public void exportPhotoById(Long photoId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LibraryException("No authenticated user found");
        }
        // The principal name is the database user ID (not username)
        Long userId = Long.parseLong(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LibraryException("User not found"));

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new LibraryException("Photo not found: " + photoId));

        exportPhoto(photo, user);
    }

    /**
     * Import a photo from Google Photos by downloading the image using its permanent ID
     * Returns error message if failed, null if successful
     */
    @Transactional
    public String importPhotoById(Long photoId) {
        logger.info("=== Starting import for photo ID: {} ===", photoId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Import failed for photo {}: No authenticated user found", photoId);
            return "No authenticated user found";
        }
        // The principal name is the database user ID (not username)
        Long userId = Long.parseLong(authentication.getName());
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.error("Import failed for photo {}: User {} not found", photoId, userId);
            return "User not found";
        }

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null) {
            logger.error("Import failed: Photo not found with ID: {}", photoId);
            return "Photo not found: " + photoId;
        }

        // Log photo details for debugging
        String bookInfo = photo.getBook() != null
            ? String.format("Book ID: %d, Title: '%s'", photo.getBook().getId(), photo.getBook().getTitle())
            : "No book";
        String authorInfo = photo.getAuthor() != null
            ? String.format("Author ID: %d, Name: '%s'", photo.getAuthor().getId(), photo.getAuthor().getName())
            : "No author";
        logger.info("Photo {} details: permanentId='{}', photoOrder={}, {}, {}",
            photoId, photo.getPermanentId(), photo.getPhotoOrder(), bookInfo, authorInfo);

        if (photo.getPermanentId() == null || photo.getPermanentId().trim().isEmpty()) {
            logger.error("Import failed for photo {}: No permanent ID", photoId);
            return "Photo does not have a permanent ID to import from";
        }

        String accessToken;
        try {
            accessToken = googlePhotosService.getValidAccessToken(user);
            logger.debug("Got access token for user {} (token length: {})", user.getId(), accessToken.length());
        } catch (Exception e) {
            String errorMsg = "Failed to get access token: " + e.getMessage();
            logger.error("Import failed for photo {}: {}", photoId, errorMsg, e);
            markPhotoAsFailed(photo, "Import failed: " + errorMsg);
            return errorMsg;
        }

        try {
            // Get media item details from Google Photos
            logger.info("Fetching media item from Google Photos for permanentId: {}", photo.getPermanentId());
            var mediaItem = photosLibraryClient.getMediaItem(accessToken, photo.getPermanentId());

            if (mediaItem == null) {
                String errorMsg = "Media item not found in Google Photos for permanentId: " + photo.getPermanentId();
                logger.error("Import failed for photo {}: {}", photoId, errorMsg);
                markPhotoAsFailed(photo, "Import failed: " + errorMsg);
                return errorMsg;
            }

            logger.info("Media item found: filename='{}', mimeType='{}'",
                mediaItem.getFilename(), mediaItem.getMimeType());

            // Download the image bytes
            String baseUrl = mediaItem.getBaseUrl();
            if (baseUrl == null || baseUrl.isEmpty()) {
                String errorMsg = "No base URL available for media item (permanentId: " + photo.getPermanentId() + ")";
                logger.error("Import failed for photo {}: {}", photoId, errorMsg);
                markPhotoAsFailed(photo, "Import failed: " + errorMsg);
                return errorMsg;
            }

            logger.info("Downloading photo from baseUrl (length: {} chars)", baseUrl.length());
            // Use downloadPhoto which already appends =d for original quality
            byte[] imageBytes = photosLibraryClient.downloadPhoto(accessToken, baseUrl);

            if (imageBytes == null || imageBytes.length == 0) {
                String errorMsg = "Failed to download image from Google Photos (permanentId: " + photo.getPermanentId() + ")";
                logger.error("Import failed for photo {}: {} - received null or empty bytes", photoId, errorMsg);
                markPhotoAsFailed(photo, "Import failed: " + errorMsg);
                return errorMsg;
            }

            logger.info("Downloaded {} bytes for photo {}", imageBytes.length, photoId);

            // Correct EXIF orientation before storing
            String mimeType = mediaItem.getMimeType() != null ? mediaItem.getMimeType() : photo.getContentType();
            imageBytes = photoService.correctImageOrientation(imageBytes, mimeType);

            // Update the photo with downloaded image
            photo.setImage(imageBytes);
            if (mediaItem.getMimeType() != null) {
                photo.setContentType(mediaItem.getMimeType());
            }
            // Compute and set the image checksum so it's no longer counted as "pending import"
            String checksum = computeChecksum(imageBytes);
            photo.setImageChecksum(checksum);
            logger.debug("Computed checksum for photo {}: {}", photoId, checksum);

            // Clear any previous error message and status on success
            photo.setExportErrorMessage(null);
            photo.setExportStatus(Photo.ExportStatus.COMPLETED);

            logger.info("Saving photo {} to database...", photoId);
            photoRepository.save(photo);

            logger.info("=== Successfully imported photo ID: {} from Google Photos ({} bytes) ===", photoId, imageBytes.length);
            return null; // Success

        } catch (Exception e) {
            logger.error("Import failed for photo {} with exception: {} - {}", photoId, e.getClass().getSimpleName(), e.getMessage(), e);
            // Mark the photo as failed so it shows in stats and table
            String errorMsg = e.getMessage();
            markPhotoAsFailed(photo, "Import failed: " + errorMsg);
            return errorMsg;
        }
    }

    /**
     * Import all photos that have permanent IDs but no image data
     * Uses efficient ID-based query to avoid loading image bytes
     * Processes photos in batches for better performance
     */
    @Transactional
    public PhotoImportResultDto importAllPhotos() {
        logger.info("Starting import of all pending photos...");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LibraryException("No authenticated user found");
        }

        // The principal name is the database user ID (not username)
        Long userId = Long.parseLong(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LibraryException("User not found"));

        // Use efficient query that returns only IDs without loading image bytes
        List<Long> photoIdsToImport = photoRepository.findIdsNeedingImport();

        if (photoIdsToImport.isEmpty()) {
            logger.info("No photos need import at this time");
            PhotoImportResultDto result = new PhotoImportResultDto();
            result.setMessage("No photos need import");
            result.setImported(0);
            result.setFailed(0);
            return result;
        }

        logger.info("Found {} photos to import", photoIdsToImport.size());

        int successCount = 0;
        int failureCount = 0;

        // Process photos in batches of 20 (conservative batch size for downloads)
        final int BATCH_SIZE = 20;
        for (int i = 0; i < photoIdsToImport.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, photoIdsToImport.size());
            List<Long> batchIds = photoIdsToImport.subList(i, endIndex);

            logger.info("Processing batch {}-{} of {}", i + 1, endIndex, photoIdsToImport.size());

            try {
                int batchSuccess = importPhotosBatch(batchIds, user);
                successCount += batchSuccess;
                failureCount += (batchIds.size() - batchSuccess);
            } catch (Exception e) {
                logger.error("Failed to import batch {}-{}: {}", i + 1, endIndex, e.getMessage(), e);
                failureCount += batchIds.size();
            }
        }

        logger.info("Photo import complete. Success: {}, Failed: {}", successCount, failureCount);

        PhotoImportResultDto result = new PhotoImportResultDto();
        result.setMessage(String.format("Import complete. %d succeeded, %d failed", successCount, failureCount));
        result.setImported(successCount);
        result.setFailed(failureCount);
        return result;
    }

    /**
     * Import a batch of photos from Google Photos in parallel
     * @param photoIds List of photo IDs to import
     * @param user User with Google Photos credentials
     * @return Number of successfully imported photos
     */
    @Transactional
    public int importPhotosBatch(List<Long> photoIds, User user) {
        if (photoIds.isEmpty()) {
            return 0;
        }

        logger.info("Importing batch of {} photos from Google Photos", photoIds.size());

        // Load photos from database
        List<Photo> photos = new ArrayList<>();
        for (Long photoId : photoIds) {
            photoRepository.findById(photoId).ifPresent(photos::add);
        }

        if (photos.isEmpty()) {
            logger.warn("No valid photos found in batch");
            return 0;
        }

        int successCount = 0;
        String accessToken;

        try {
            accessToken = googlePhotosService.getValidAccessToken(user);
            logger.debug("Got access token for user {} (token length: {})", user.getId(), accessToken.length());
        } catch (Exception e) {
            logger.error("Failed to get access token: {}", e.getMessage(), e);
            // Mark all photos as failed
            for (Photo photo : photos) {
                markPhotoAsFailed(photo, "Import failed: " + e.getMessage());
            }
            return 0;
        }

        // Process each photo in the batch
        for (Photo photo : photos) {
            try {
                if (photo.getPermanentId() == null || photo.getPermanentId().trim().isEmpty()) {
                    logger.warn("Photo {} has no permanent ID, skipping", photo.getId());
                    continue;
                }

                // Get media item details from Google Photos
                logger.debug("Fetching media item from Google Photos for photo {} (permanentId: {})",
                        photo.getId(), photo.getPermanentId());
                var mediaItem = photosLibraryClient.getMediaItem(accessToken, photo.getPermanentId());

                if (mediaItem == null) {
                    String errorMsg = "Media item not found in Google Photos for permanentId: " + photo.getPermanentId();
                    logger.error("Import failed for photo {}: {}", photo.getId(), errorMsg);
                    markPhotoAsFailed(photo, "Import failed: " + errorMsg);
                    continue;
                }

                logger.debug("Media item found for photo {}: filename='{}', mimeType='{}'",
                        photo.getId(), mediaItem.getFilename(), mediaItem.getMimeType());

                // Download the image bytes
                String baseUrl = mediaItem.getBaseUrl();
                if (baseUrl == null || baseUrl.isEmpty()) {
                    String errorMsg = "No base URL available for media item (permanentId: " + photo.getPermanentId() + ")";
                    logger.error("Import failed for photo {}: {}", photo.getId(), errorMsg);
                    markPhotoAsFailed(photo, "Import failed: " + errorMsg);
                    continue;
                }

                logger.debug("Downloading photo {} from baseUrl (length: {} chars)", photo.getId(), baseUrl.length());
                byte[] imageBytes = photosLibraryClient.downloadPhoto(accessToken, baseUrl);

                if (imageBytes == null || imageBytes.length == 0) {
                    String errorMsg = "Failed to download image from Google Photos (permanentId: " + photo.getPermanentId() + ")";
                    logger.error("Import failed for photo {}: {} - received null or empty bytes", photo.getId(), errorMsg);
                    markPhotoAsFailed(photo, "Import failed: " + errorMsg);
                    continue;
                }

                logger.debug("Downloaded {} bytes for photo {}", imageBytes.length, photo.getId());

                // Correct EXIF orientation before storing
                String batchMimeType = mediaItem.getMimeType() != null ? mediaItem.getMimeType() : photo.getContentType();
                imageBytes = photoService.correctImageOrientation(imageBytes, batchMimeType);

                // Update the photo with downloaded image
                photo.setImage(imageBytes);
                if (mediaItem.getMimeType() != null) {
                    photo.setContentType(mediaItem.getMimeType());
                }
                // Compute and set the image checksum
                String checksum = computeChecksum(imageBytes);
                photo.setImageChecksum(checksum);
                logger.debug("Computed checksum for photo {}: {}", photo.getId(), checksum);

                // Clear any previous error message and status on success
                photo.setExportErrorMessage(null);
                photo.setExportStatus(Photo.ExportStatus.COMPLETED);

                photoRepository.save(photo);
                successCount++;

                logger.debug("Successfully imported photo {} ({} bytes)", photo.getId(), imageBytes.length);

            } catch (Exception e) {
                logger.error("Import failed for photo {} with exception: {} - {}",
                        photo.getId(), e.getClass().getSimpleName(), e.getMessage(), e);
                String errorMsg = e.getMessage();
                markPhotoAsFailed(photo, "Import failed: " + errorMsg);
            }
        }

        logger.info("Batch import completed: {} of {} photos succeeded", successCount, photos.size());
        return successCount;
    }

    /**
     * Verify that a photo's permanent ID still works in Google Photos
     */
    @Transactional(readOnly = true)
    public PhotoVerifyResultDto verifyPhotoById(Long photoId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LibraryException("No authenticated user found");
        }
        // The principal name is the database user ID (not username)
        Long userId = Long.parseLong(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LibraryException("User not found"));

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new LibraryException("Photo not found: " + photoId));

        if (photo.getPermanentId() == null || photo.getPermanentId().trim().isEmpty()) {
            throw new LibraryException("Photo does not have a permanent ID to verify");
        }
        String accessToken = googlePhotosService.getValidAccessToken(user);

        PhotoVerifyResultDto result = new PhotoVerifyResultDto();
        result.setPhotoId(photoId);
        result.setPermanentId(photo.getPermanentId());

        try {
            // Try to get media item details from Google Photos
            var mediaItem = photosLibraryClient.getMediaItem(accessToken, photo.getPermanentId());

            if (mediaItem != null) {
                result.setValid(true);
                result.setMessage("Permanent ID is valid");
                result.setFilename(mediaItem.getFilename());
                result.setMimeType(mediaItem.getMimeType());
            } else {
                result.setValid(false);
                result.setMessage("Media item not found in Google Photos");
            }

        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            logger.warn("Verification returned 404 for photo ID {}: permanent ID {} not found in Google Photos. " +
                    "This may indicate the photo was deleted, or was uploaded with a different OAuth client/authorization.",
                    photoId, photo.getPermanentId());
            result.setValid(false);
            result.setMessage("Media item not found in Google Photos. The photo may have been deleted, " +
                    "or the permanent ID was stored from a failed upload. Consider unlinking and re-exporting.");
        } catch (Exception e) {
            logger.warn("Verification failed for photo ID {}: {}", photoId, e.getMessage());
            result.setValid(false);
            result.setMessage("Verification failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Unlink a photo by removing its permanent ID
     */
    @Transactional
    public void unlinkPhotoById(Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new LibraryException("Photo not found: " + photoId));

        if (photo.getPermanentId() == null || photo.getPermanentId().trim().isEmpty()) {
            throw new LibraryException("Photo does not have a permanent ID to unlink");
        }

        String oldPermanentId = photo.getPermanentId();
        photo.setPermanentId(null);
        photo.setExportStatus(Photo.ExportStatus.PENDING);
        photo.setExportedAt(null);
        photoRepository.save(photo);

        logger.info("Unlinked photo ID: {} from permanent ID: {}", photoId, oldPermanentId);
    }

    /**
     * Data class containing metadata needed for ZIP filename generation.
     * Does not include image bytes to minimize memory usage.
     */
    public static class PhotoZipMetadata {
        public final Long id;
        public final String contentType;
        public final String bookTitle;
        public final String authorName;
        public final String loanBookTitle;
        public final String loanUsername;

        public PhotoZipMetadata(Long id, String contentType, String bookTitle, String authorName,
                               String loanBookTitle, String loanUsername) {
            this.id = id;
            this.contentType = contentType;
            this.bookTitle = bookTitle;
            this.authorName = authorName;
            this.loanBookTitle = loanBookTitle;
            this.loanUsername = loanUsername;
        }
    }

    /**
     * Get photo IDs for ZIP export (lightweight query).
     * Returns ALL active photos, including those that need to be downloaded from Google Photos.
     */
    @Transactional(readOnly = true)
    public List<Long> getPhotoIdsForZipExport() {
        logger.info("Querying photo IDs for ZIP export...");
        List<Long> photoIds = photoRepository.findAllActivePhotoIds();
        logger.info("Found {} photo IDs for ZIP export", photoIds.size());
        return photoIds;
    }

    /**
     * Get metadata for all photos that can be exported (have images).
     * This loads photo metadata efficiently, one at a time.
     */
    @Transactional(readOnly = true)
    public List<PhotoZipMetadata> getPhotoMetadataForZipExport() {
        List<Long> photoIds = getPhotoIdsForZipExport();

        // Build metadata list by loading each photo individually
        // This avoids loading all Photo objects into memory at once
        List<PhotoZipMetadata> metadata = new ArrayList<>();
        for (Long id : photoIds) {
            photoRepository.findById(id).ifPresent(photo -> {
                String bookTitle = photo.getBook() != null ? photo.getBook().getTitle() : null;
                String authorName = photo.getAuthor() != null ? photo.getAuthor().getName() : null;
                String loanBookTitle = null;
                String loanUsername = null;
                if (photo.getLoan() != null) {
                    loanBookTitle = photo.getLoan().getBook() != null ? photo.getLoan().getBook().getTitle() : null;
                    loanUsername = photo.getLoan().getUser() != null ? photo.getLoan().getUser().getUsername() : null;
                }
                metadata.add(new PhotoZipMetadata(id, photo.getContentType(),
                        bookTitle, authorName, loanBookTitle, loanUsername));
            });
        }

        return metadata;
    }

    /**
     * Stream photos to a ZIP output stream, loading one photo at a time.
     * This is memory-efficient for large photo collections.
     * Note: Not transactional to avoid timeout issues with large exports.
     * Each photo is loaded individually with its own database access.
     *
     * @param outputStream the output stream to write the ZIP to
     * @throws IOException if writing fails
     */
    public void streamPhotosToZip(java.io.OutputStream outputStream) throws java.io.IOException {
        logger.info("Starting streaming photo ZIP export...");

        // Get ALL active photo IDs - including those needing download from Google Photos
        List<Long> photoIds = photoRepository.findAllActivePhotoIds();
        long photosWithImages = photoRepository.findActivePhotoIdsWithImages().size();
        long photosPendingImport = photoIds.size() - photosWithImages;

        if (photoIds.isEmpty()) {
            throw new LibraryException("No photos available for export.");
        }

        logger.info("Streaming {} photos to ZIP ({} with local images, {} will be downloaded from Google Photos)",
                photoIds.size(), photosWithImages, photosPendingImport);

        // Get access token for downloading photos from Google Photos (if needed)
        String accessToken = null;
        if (photosPendingImport > 0) {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    Long userId = Long.parseLong(authentication.getName());
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        accessToken = googlePhotosService.getValidAccessToken(user);
                        logger.info("Got access token for downloading {} photos from Google Photos", photosPendingImport);
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not get Google Photos access token, photos without local data will be skipped: {}", e.getMessage());
            }
        }

        // Track filename counts for handling multiple photos of same entity
        Map<String, Integer> filenameCount = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(outputStream)) {
            for (Long photoId : photoIds) {
                try {
                    // Load this photo with relationships (image is LAZY loaded)
                    Photo photo = photoRepository.findById(photoId).orElse(null);
                    if (photo == null) {
                        logger.warn("Photo {} not found during export", photoId);
                        errorCount++;
                        continue;
                    }

                    // Get image bytes - from local storage or Google Photos
                    byte[] imageBytes = photo.getImage();
                    if ((imageBytes == null || imageBytes.length == 0) && accessToken != null
                            && photo.getPermanentId() != null && !photo.getPermanentId().isEmpty()) {
                        // Download from Google Photos
                        try {
                            var mediaItem = photosLibraryClient.getMediaItem(accessToken, photo.getPermanentId());
                            if (mediaItem != null && mediaItem.getBaseUrl() != null) {
                                imageBytes = photosLibraryClient.downloadPhoto(accessToken, mediaItem.getBaseUrl());
                                if (imageBytes != null && imageBytes.length > 0) {
                                    logger.info("Downloaded photo {} from Google Photos ({} bytes)", photoId, imageBytes.length);
                                    // Save locally for future use
                                    photo.setImage(imageBytes);
                                    photo.setImageChecksum(computeChecksum(imageBytes));
                                    if (mediaItem.getMimeType() != null) {
                                        photo.setContentType(mediaItem.getMimeType());
                                    }
                                    photoRepository.save(photo);
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to download photo {} from Google Photos: {}", photoId, e.getMessage());
                        }
                    }
                    if (imageBytes == null || imageBytes.length == 0) {
                        logger.warn("Skipping photo {} - no image data and could not download", photoId);
                        errorCount++;
                        continue;
                    }

                    // Correct EXIF orientation for existing photos
                    imageBytes = photoService.correctImageOrientation(imageBytes, photo.getContentType());

                    // Backfill checksum if missing
                    if (photo.getImageChecksum() == null) {
                        String checksum = computeChecksum(imageBytes);
                        photo.setImageChecksum(checksum);
                        photoRepository.save(photo);
                        logger.info("Backfilled checksum for photo {}", photoId);
                    }

                    // Generate filename from the loaded photo
                    String baseFilename = generateZipFilename(photo);
                    String extension = getFileExtension(photo.getContentType());

                    // Handle multiple photos for same entity
                    int count = filenameCount.getOrDefault(baseFilename, 0);
                    filenameCount.put(baseFilename, count + 1);

                    String filename;
                    if (count == 0) {
                        filename = baseFilename + extension;
                    } else {
                        filename = baseFilename + "-" + (count + 1) + extension;
                    }

                    // Add to ZIP and immediately release memory
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(filename);
                    zos.putNextEntry(entry);
                    zos.write(imageBytes);
                    zos.closeEntry();
                    zos.flush();

                    // Detach entity to release memory from persistence context
                    entityManager.detach(photo);

                    successCount++;
                    if (successCount % 10 == 0) {
                        logger.info("Progress: {} photos exported", successCount);
                    }

                    // Periodically clear the entire persistence context to free first-level cache
                    if (successCount % 20 == 0) {
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    logger.error("Failed to add photo {} to ZIP: {}", photoId, e.getMessage(), e);
                    errorCount++;
                    // Continue with other photos
                }
            }

            zos.finish();
            logger.info("Photo ZIP export completed: {} succeeded, {} failed", successCount, errorCount);
        }
    }

    /**
     * Export all photos as a ZIP file (legacy method for tests).
     * Uses the same filename format as the ZIP import feature.
     * WARNING: This loads all photos into memory - use streamPhotosToZip for production.
     *
     * @return byte array containing the ZIP file
     */
    @Transactional(readOnly = true)
    public byte[] exportPhotosAsZip() {
        logger.info("Starting photo ZIP export (in-memory)...");

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            streamPhotosToZip(baos);
            return baos.toByteArray();
        } catch (java.io.IOException e) {
            logger.error("Failed to create ZIP file: {}", e.getMessage(), e);
            throw new LibraryException("Failed to create ZIP file: " + e.getMessage());
        }
    }

    /**
     * Generate a filename for a photo in the ZIP archive.
     * Format matches the import filename format for round-trip compatibility.
     */
    private String generateZipFilename(Photo photo) {
        if (photo.getBook() != null) {
            return "book-" + sanitizeName(photo.getBook().getTitle());
        } else if (photo.getAuthor() != null) {
            return "author-" + sanitizeName(photo.getAuthor().getName());
        } else if (photo.getLoan() != null) {
            String bookTitle = photo.getLoan().getBook() != null
                    ? photo.getLoan().getBook().getTitle() : "unknown";
            String username = photo.getLoan().getUser() != null
                    ? photo.getLoan().getUser().getUsername() : "unknown";
            return "loan-" + sanitizeName(bookTitle) + "-" + sanitizeName(username);
        } else {
            return "photo-" + photo.getId();
        }
    }

    /**
     * Sanitize a name for use in filenames.
     * Preserves the complete name but removes/replaces characters that are
     * invalid in filenames across different operating systems.
     * Invalid chars: / \ : * ? " < > |
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
