/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.domain.User;
import com.muczynski.library.photostorage.client.GooglePhotosLibraryClient;
import com.muczynski.library.photostorage.dto.AlbumResponse;
import com.muczynski.library.photostorage.dto.BatchCreateRequest;
import com.muczynski.library.photostorage.dto.BatchCreateResponse;
import com.muczynski.library.photostorage.dto.SearchResponse;
import com.muczynski.library.repository.LibraryRepository;
import com.muczynski.library.repository.PhotoRepository;
import com.muczynski.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PhotoBackupService {

    private static final Logger logger = LoggerFactory.getLogger(PhotoBackupService.class);

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private GooglePhotosService googlePhotosService;

    @Autowired
    private GooglePhotosLibraryClient photosLibraryClient;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${APP_ENV:production}")
    private String appEnv;

    // Cache the album name to avoid repeated database lookups
    private String cachedAlbumName = null;

    /**
     * Scheduled task to backup photos to Google Photos
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    @Transactional
    public void backupPhotos() {
        logger.info("Starting scheduled photo backup process...");

        try {
            // Get the librarian user (assuming they have Google Photos configured)
            Optional<User> librarianOpt = userRepository.findByUsernameIgnoreCase("librarian");

            if (librarianOpt.isEmpty()) {
                logger.warn("No librarian user found for photo backup");
                return;
            }

            User librarian = librarianOpt.get();

            // Check if Google Photos is configured
            if (librarian.getGooglePhotosApiKey() == null || librarian.getGooglePhotosApiKey().trim().isEmpty()) {
                logger.warn("Google Photos not configured for librarian user. Skipping backup.");
                return;
            }

            // Find photos that need to be backed up
            List<Photo> photosToBackup = findPhotosNeedingBackup();

            if (photosToBackup.isEmpty()) {
                logger.info("No photos need backup at this time");
                return;
            }

            logger.info("Found {} photos to backup", photosToBackup.size());

            int successCount = 0;
            int failureCount = 0;

            for (Photo photo : photosToBackup) {
                try {
                    backupPhoto(photo, librarian.getUsername());
                    successCount++;
                } catch (Exception e) {
                    logger.error("Failed to backup photo ID: {}", photo.getId(), e);
                    markPhotoAsFailed(photo, e.getMessage());
                    failureCount++;
                }
            }

            logger.info("Photo backup complete. Success: {}, Failed: {}", successCount, failureCount);

        } catch (Exception e) {
            logger.error("Error during scheduled photo backup", e);
        }
    }

    /**
     * Find photos that need to be backed up
     */
    private List<Photo> findPhotosNeedingBackup() {
        List<Photo> allPhotos = photoRepository.findAllWithBookAndAuthor();
        List<Photo> photosToBackup = new ArrayList<>();

        for (Photo photo : allPhotos) {
            // Skip if already backed up successfully
            if (photo.getBackupStatus() == Photo.BackupStatus.COMPLETED &&
                photo.getPermanentId() != null && !photo.getPermanentId().trim().isEmpty()) {
                continue;
            }

            // Skip if currently in progress
            if (photo.getBackupStatus() == Photo.BackupStatus.IN_PROGRESS) {
                continue;
            }

            // Include if pending, failed, or no status
            photosToBackup.add(photo);
        }

        return photosToBackup;
    }

    /**
     * Backup a single photo to Google Photos
     */
    @Transactional
    public void backupPhoto(Photo photo, String username) {
        logger.info("Backing up photo ID: {}", photo.getId());

        // Mark as in progress
        photo.setBackupStatus(Photo.BackupStatus.IN_PROGRESS);
        photoRepository.save(photo);

        try {
            // Step 1: Upload the raw bytes to get an upload token
            String uploadToken = uploadPhotoBytes(photo.getImage(), photo.getContentType(), username);

            // Step 2: Create a media item with the upload token
            String permanentId = createMediaItem(uploadToken, photo, username);

            // Step 3: Mark as completed
            photo.setPermanentId(permanentId);
            photo.setBackupStatus(Photo.BackupStatus.COMPLETED);
            photo.setBackedUpAt(LocalDateTime.now());
            photo.setBackupErrorMessage(null);
            photoRepository.save(photo);

            logger.info("Successfully backed up photo ID: {} with permanent ID: {}", photo.getId(), permanentId);

        } catch (Exception e) {
            logger.error("Failed to backup photo ID: {}", photo.getId(), e);
            throw e;
        }
    }

    /**
     * Upload photo bytes to Google Photos and get an upload token
     */
    private String uploadPhotoBytes(byte[] imageBytes, String contentType, String username) {
        logger.debug("Uploading photo bytes ({} bytes) to Google Photos", imageBytes.length);

        // Get valid access token
        String accessToken = googlePhotosService.getValidAccessToken(username);

        // Verify token scopes before upload
        logger.info("Verifying token scopes before upload operation...");
        googlePhotosService.verifyAccessTokenScopes(accessToken);

        // Use the photostorage client for upload
        return photosLibraryClient.uploadBytes(accessToken, imageBytes, contentType);
    }

    /**
     * Create a media item in Google Photos using the upload token
     */
    private String createMediaItem(String uploadToken, Photo photo, String username) {
        logger.debug("Creating media item in Google Photos");

        // Get valid access token
        String accessToken = googlePhotosService.getValidAccessToken(username);

        // Verify token scopes before attempting batchCreate
        logger.info("Verifying token scopes before batchCreate operation...");
        googlePhotosService.verifyAccessTokenScopes(accessToken);

        // Get or create the library album
        String albumId = getOrCreateAlbum(username);

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

            if (result.getMediaItem() != null && result.getMediaItem().getId() != null) {
                String permanentId = result.getMediaItem().getId();
                logger.info("Successfully created media item with ID: {}", permanentId);
                return permanentId;
            } else {
                logger.error("Media item created but no ID in response");
                throw new RuntimeException("No media item ID in response");
            }
        } else {
            logger.error("No media item results in response");
            throw new RuntimeException("No media item results in response");
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
            libraryName = libraries.get(0).getName();
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
    private String getOrCreateAlbum(String username) {
        // Get the user from database
        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + username);
        }

        User user = userOpt.get();

        // Check if user has a saved album ID
        if (user.getGooglePhotosAlbumId() != null && !user.getGooglePhotosAlbumId().trim().isEmpty()) {
            logger.info("Using saved album ID from user settings: {}", user.getGooglePhotosAlbumId());
            return user.getGooglePhotosAlbumId();
        }

        // No saved album ID, need to create a new album
        String albumName = getAlbumName();
        logger.info("No saved album ID found. Creating new album: {}", albumName);

        // Get valid access token
        String accessToken = googlePhotosService.getValidAccessToken(username);

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
        photo.setBackupStatus(Photo.BackupStatus.FAILED);
        photo.setBackupErrorMessage(errorMessage != null ? errorMessage.substring(0, Math.min(500, errorMessage.length())) : "Unknown error");
        photoRepository.save(photo);
    }

    /**
     * Get backup statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getBackupStats() {
        List<Photo> allPhotos = photoRepository.findAllWithBookAndAuthor();

        long total = allPhotos.size();
        long completed = allPhotos.stream()
                .filter(p -> p.getBackupStatus() == Photo.BackupStatus.COMPLETED)
                .count();
        long pending = allPhotos.stream()
                .filter(p -> p.getBackupStatus() == null || p.getBackupStatus() == Photo.BackupStatus.PENDING)
                .count();
        long failed = allPhotos.stream()
                .filter(p -> p.getBackupStatus() == Photo.BackupStatus.FAILED)
                .count();
        long inProgress = allPhotos.stream()
                .filter(p -> p.getBackupStatus() == Photo.BackupStatus.IN_PROGRESS)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("pending", pending);
        stats.put("failed", failed);
        stats.put("inProgress", inProgress);

        // Add album information
        String albumName = getAlbumName();
        stats.put("albumName", albumName);

        // Get album ID from librarian user settings
        Optional<User> librarianOpt = userRepository.findByUsernameIgnoreCase("librarian");
        if (librarianOpt.isPresent()) {
            String albumId = librarianOpt.get().getGooglePhotosAlbumId();
            stats.put("albumId", albumId != null && !albumId.trim().isEmpty() ? albumId : null);
        } else {
            stats.put("albumId", null);
        }

        return stats;
    }

    /**
     * Get all photos with their backup status
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllPhotosWithBackupStatus() {
        logger.debug("Fetching all photos with book and author, sorted by ID");
        List<Photo> allPhotos = photoRepository.findAllWithBookAndAuthorOrderById();
        logger.info("Found {} photos in database", allPhotos.size());

        List<Map<String, Object>> result = new ArrayList<>();

        for (Photo photo : allPhotos) {
            try {
                Map<String, Object> photoInfo = new HashMap<>();
                photoInfo.put("id", photo.getId());
                photoInfo.put("caption", photo.getCaption());
                photoInfo.put("backupStatus", photo.getBackupStatus() != null ? photo.getBackupStatus().toString() : "PENDING");
                photoInfo.put("backedUpAt", photo.getBackedUpAt());
                photoInfo.put("permanentId", photo.getPermanentId());
                photoInfo.put("backupErrorMessage", photo.getBackupErrorMessage());
                photoInfo.put("contentType", photo.getContentType());

                if (photo.getBook() != null) {
                    photoInfo.put("bookTitle", photo.getBook().getTitle());
                    photoInfo.put("bookId", photo.getBook().getId());
                }

                if (photo.getAuthor() != null) {
                    photoInfo.put("authorName", photo.getAuthor().getName());
                    photoInfo.put("authorId", photo.getAuthor().getId());
                }

                result.add(photoInfo);
            } catch (Exception e) {
                logger.error("Error processing photo ID: {} - Error: {}", photo.getId(), e.getMessage(), e);
                // Continue processing other photos even if one fails
            }
        }

        logger.info("Successfully processed {} photos for display", result.size());
        return result;
    }

    /**
     * Manually trigger backup for a specific photo
     */
    @Transactional
    public void backupPhotoById(Long photoId) {
        Optional<User> librarianOpt = userRepository.findByUsernameIgnoreCase("librarian");

        if (librarianOpt.isEmpty()) {
            throw new RuntimeException("No librarian user found");
        }

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found: " + photoId));

        backupPhoto(photo, librarianOpt.get().getUsername());
    }
}
