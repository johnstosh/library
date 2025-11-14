/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Photo;
import com.muczynski.library.domain.User;
import com.muczynski.library.repository.PhotoRepository;
import com.muczynski.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    private GooglePhotosService googlePhotosService;

    @Value("${google.oauth.client-id}")
    private String clientId;

    private final RestTemplate restTemplate;

    public PhotoBackupService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Scheduled task to backup photos to Google Photos
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
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
        List<Photo> allPhotos = photoRepository.findAll();
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

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add("X-Goog-Upload-Content-Type", contentType);
        headers.add("X-Goog-Upload-Protocol", "raw");

        HttpEntity<byte[]> entity = new HttpEntity<>(imageBytes, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://photoslibrary.googleapis.com/v1/uploads",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String uploadToken = response.getBody();
                logger.debug("Successfully obtained upload token: {}", uploadToken.substring(0, Math.min(20, uploadToken.length())));
                return uploadToken;
            } else {
                logger.error("Failed to upload photo bytes with status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to upload photo bytes: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Failed to upload photo bytes to Google Photos", e);
            throw new RuntimeException("Failed to upload photo bytes: " + e.getMessage(), e);
        }
    }

    /**
     * Create a media item in Google Photos using the upload token
     */
    private String createMediaItem(String uploadToken, Photo photo, String username) {
        logger.debug("Creating media item in Google Photos");

        // Get valid access token
        String accessToken = googlePhotosService.getValidAccessToken(username);

        // Build description from photo caption and associated book/author
        String description = buildPhotoDescription(photo);

        // Build request body
        Map<String, Object> newMediaItem = new HashMap<>();
        newMediaItem.put("description", description);

        Map<String, Object> simpleMediaItem = new HashMap<>();
        simpleMediaItem.put("uploadToken", uploadToken);
        simpleMediaItem.put("fileName", "library-photo-" + photo.getId() + getFileExtension(photo.getContentType()));

        newMediaItem.put("simpleMediaItem", simpleMediaItem);

        Map<String, Object> request = new HashMap<>();
        request.put("newMediaItems", Collections.singletonList(newMediaItem));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://photoslibrary.googleapis.com/v1/mediaItems:batchCreate",
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> newMediaItemResults = (List<Map<String, Object>>) responseBody.get("newMediaItemResults");

                if (newMediaItemResults != null && !newMediaItemResults.isEmpty()) {
                    Map<String, Object> result = newMediaItemResults.get(0);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> mediaItem = (Map<String, Object>) result.get("mediaItem");

                    if (mediaItem != null && mediaItem.containsKey("id")) {
                        String permanentId = (String) mediaItem.get("id");
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
            } else {
                logger.error("Failed to create media item with status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to create media item: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Failed to create media item in Google Photos", e);
            throw new RuntimeException("Failed to create media item: " + e.getMessage(), e);
        }
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
            if (photo.getBook().getAuthors() != null && !photo.getBook().getAuthors().isEmpty()) {
                description.append(" by ").append(photo.getBook().getAuthors().iterator().next().getName());
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
    public Map<String, Object> getBackupStats() {
        List<Photo> allPhotos = photoRepository.findAll();

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

        return stats;
    }

    /**
     * Get all photos with their backup status
     */
    public List<Map<String, Object>> getAllPhotosWithBackupStatus() {
        List<Photo> allPhotos = photoRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Photo photo : allPhotos) {
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
        }

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
