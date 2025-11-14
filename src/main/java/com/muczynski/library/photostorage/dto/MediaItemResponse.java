/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for media item in Google Photos Library API
 */
@Data
@NoArgsConstructor
public class MediaItemResponse {
    private String id;
    private String description;
    private String filename;
    private String mimeType;
    private String baseUrl;
    private String productUrl;
    private MediaMetadata mediaMetadata;
    private ContributorInfo contributorInfo;

    @Data
    @NoArgsConstructor
    public static class MediaMetadata {
        private String creationTime;
        private String width;
        private String height;
        private Photo photo;
        private Video video;

        @Data
        @NoArgsConstructor
        public static class Photo {
            private String cameraMake;
            private String cameraModel;
            private Double focalLength;
            private Double apertureFNumber;
            private Integer isoEquivalent;
        }

        @Data
        @NoArgsConstructor
        public static class Video {
            private String cameraMake;
            private String cameraModel;
            private Double fps;
            private String status;
        }
    }

    @Data
    @NoArgsConstructor
    public static class ContributorInfo {
        private String profilePictureBaseUrl;
        private String displayName;
    }
}
