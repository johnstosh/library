/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.photostorage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for searching media items in Google Photos Library API
 */
@Data
@NoArgsConstructor
public class SearchRequest {
    private String albumId;
    private Integer pageSize;
    private String pageToken;
    private Filters filters;

    @Data
    @NoArgsConstructor
    public static class Filters {
        private DateFilter dateFilter;
        private ContentFilter contentFilter;
        private MediaTypeFilter mediaTypeFilter;

        @Data
        @NoArgsConstructor
        public static class DateFilter {
            private java.util.List<DateRange> ranges;

            @Data
            @NoArgsConstructor
            public static class DateRange {
                private Date startDate;
                private Date endDate;

                @Data
                @NoArgsConstructor
                public static class Date {
                    private Integer year;
                    private Integer month;
                    private Integer day;
                }
            }
        }

        @Data
        @NoArgsConstructor
        public static class ContentFilter {
            private java.util.List<String> includedContentCategories;
            private java.util.List<String> excludedContentCategories;
        }

        @Data
        @NoArgsConstructor
        public static class MediaTypeFilter {
            private java.util.List<String> mediaTypes; // e.g., "PHOTO", "VIDEO"
        }
    }
}
