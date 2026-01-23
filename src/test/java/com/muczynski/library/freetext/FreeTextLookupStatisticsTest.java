/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test that gathers statistics about free text lookup providers.
 * <p>
 * This test is manually executed and not part of normal test runs.
 * It reads books from branch-archive.json and tests each provider against each book,
 * collecting hit/miss statistics and detailed logs.
 * <p>
 * Processing approach: For each book, all providers are tested before moving to the next book.
 * This provides natural rate limiting - by the time the test cycles back to a provider for the
 * next book, sufficient time has passed to avoid overwhelming any external API.
 * <p>
 * Output:
 * - ./book-lookup/[ProviderName].log - detailed log per provider
 * - ./book-lookup-statistics.json - aggregated statistics (also used for resume support)
 * <p>
 * Run with: ./gradlew test --tests "*.FreeTextLookupStatisticsTest" -DincludeTags=manual
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("manual")
class FreeTextLookupStatisticsTest {

    private static final String ARCHIVE_FILE = "./branch-archive.json";
    private static final String STATISTICS_FILE = "./book-lookup-statistics.json";
    private static final String LOG_DIRECTORY = "./book-lookup";

    @Autowired
    private List<FreeTextProvider> providers;

    private ObjectMapper objectMapper;
    private Path logDirectory;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        logDirectory = Path.of(LOG_DIRECTORY);
    }

    @Test
    void gatherLookupStatistics() throws Exception {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("FREE TEXT LOOKUP STATISTICS TEST");
        System.out.println("Started: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("=".repeat(60));
        System.out.println();

        // Skip if archive file doesn't exist
        File archiveFile = new File(ARCHIVE_FILE);
        System.out.println("Checking for archive file: " + archiveFile.getAbsolutePath());
        assumeTrue(archiveFile.exists(),
                "Skipping test: " + ARCHIVE_FILE + " does not exist");
        System.out.println("Archive file found. Size: " + (archiveFile.length() / 1024) + " KB");

        // Sort providers by priority
        System.out.println();
        System.out.println("Initializing providers...");
        providers.sort(Comparator.comparingInt(FreeTextProvider::getPriority));
        System.out.println("Found " + providers.size() + " providers (sorted by priority):");
        for (FreeTextProvider provider : providers) {
            System.out.println("  " + provider.getPriority() + ": " + provider.getProviderName());
        }

        // Create log directory
        System.out.println();
        System.out.println("Log directory: " + logDirectory.toAbsolutePath());
        if (!Files.exists(logDirectory)) {
            Files.createDirectories(logDirectory);
            System.out.println("Created log directory.");
        } else {
            System.out.println("Log directory already exists.");
        }

        // Load archive
        System.out.println();
        System.out.println("Loading archive file...");
        long parseStart = System.currentTimeMillis();
        Map<String, Object> archive = objectMapper.readValue(archiveFile, new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> books = (List<Map<String, Object>>) archive.get("books");
        long parseTime = System.currentTimeMillis() - parseStart;
        System.out.println("Archive parsed in " + parseTime + "ms");

        if (books == null || books.isEmpty()) {
            System.out.println("ERROR: No books found in archive");
            return;
        }

        System.out.println("Found " + books.size() + " books in archive");

        // Load existing statistics for resume support
        System.out.println();
        Statistics stats = loadOrCreateStatistics(books.size());
        Set<String> processedTitles = stats.getProcessedBookTitles() != null
                ? new HashSet<>(stats.getProcessedBookTitles())
                : new HashSet<>();
        System.out.println("Already processed: " + processedTitles.size() + " books");
        System.out.println("Remaining: " + (books.size() - processedTitles.size()) + " books");

        // Open log files for each provider
        System.out.println();
        System.out.println("Opening log files...");
        Map<String, PrintWriter> logWriters = openLogFiles();
        System.out.println("Opened " + logWriters.size() + " log files");

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("STARTING BOOK PROCESSING");
        System.out.println("=".repeat(60));
        System.out.println();

        try {
            int bookIndex = 0;
            int skippedBlank = 0;
            int skippedProcessed = 0;
            int totalHits = 0;
            int totalLookups = 0;

            for (Map<String, Object> book : books) {
                bookIndex++;
                String title = (String) book.get("title");
                String authorName = (String) book.get("authorName");

                if (title == null || title.isBlank()) {
                    skippedBlank++;
                    System.out.println("[" + bookIndex + "/" + books.size() + "] SKIP: blank title");
                    continue;
                }

                // Skip already processed books (resume support)
                if (processedTitles.contains(title)) {
                    skippedProcessed++;
                    continue;
                }

                System.out.println();
                System.out.printf("[%d/%d] Processing: \"%s\" by %s%n",
                        bookIndex, books.size(), title, authorName != null ? authorName : "Unknown");
                System.out.println("-".repeat(60));

                int bookHits = 0;
                long bookStartTime = System.currentTimeMillis();

                // Test each provider
                for (FreeTextProvider provider : providers) {
                    String providerName = provider.getProviderName();
                    ProviderStats providerStats = stats.getProviders()
                            .computeIfAbsent(providerName, k -> new ProviderStats());

                    long providerStartTime = System.currentTimeMillis();
                    try {
                        FreeTextLookupResult result = provider.search(title, authorName);
                        long providerTime = System.currentTimeMillis() - providerStartTime;

                        providerStats.setLookups(providerStats.getLookups() + 1);
                        totalLookups++;

                        if (result.isFound()) {
                            providerStats.setHits(providerStats.getHits() + 1);
                            totalHits++;
                            bookHits++;
                            logResult(logWriters.get(providerName), title, authorName, "HIT", result.getUrl());
                            System.out.printf("  %-25s [%4dms] HIT: %s%n", providerName, providerTime, result.getUrl());
                        } else {
                            logResult(logWriters.get(providerName), title, authorName, "MISS",
                                    result.getErrorMessage() != null ? result.getErrorMessage() : "Not found");
                            System.out.printf("  %-25s [%4dms] MISS%n", providerName, providerTime);
                        }

                        // Update hit rate
                        if (providerStats.getLookups() > 0) {
                            providerStats.setHitRate(
                                    (double) providerStats.getHits() / providerStats.getLookups());
                        }

                    } catch (Exception e) {
                        long providerTime = System.currentTimeMillis() - providerStartTime;
                        providerStats.setLookups(providerStats.getLookups() + 1);
                        totalLookups++;
                        logResult(logWriters.get(providerName), title, authorName, "ERROR", e.getMessage());
                        System.out.printf("  %-25s [%4dms] ERROR: %s%n", providerName, providerTime,
                                e.getMessage() != null ? e.getMessage().substring(0, Math.min(50, e.getMessage().length())) : "null");
                    }
                }

                long bookTime = System.currentTimeMillis() - bookStartTime;
                System.out.printf("  Book complete: %d hits from %d providers in %dms%n",
                        bookHits, providers.size(), bookTime);

                // Mark book as processed
                stats.getProcessedBookTitles().add(title);
                stats.setBooksProcessed(stats.getProcessedBookTitles().size());
                stats.setLastUpdated(LocalDateTime.now());

                // Save progress after each book
                saveStatistics(stats);

                // Flush all log files
                for (PrintWriter writer : logWriters.values()) {
                    writer.flush();
                }

                // Print running totals every 10 books
                if (stats.getBooksProcessed() % 10 == 0) {
                    System.out.println();
                    System.out.printf(">>> Progress: %d/%d books | Total hits: %d | Total lookups: %d | Hit rate: %.1f%%%n",
                            stats.getBooksProcessed(), books.size(), totalHits, totalLookups,
                            totalLookups > 0 ? (100.0 * totalHits / totalLookups) : 0);
                    System.out.println();
                }
            }

            System.out.println();
            System.out.println("=".repeat(60));
            System.out.println("PROCESSING COMPLETE");
            System.out.println("=".repeat(60));
            System.out.println("Skipped (blank title): " + skippedBlank);
            System.out.println("Skipped (already processed): " + skippedProcessed);
            System.out.println("Total lookups: " + totalLookups);
            System.out.println("Total hits: " + totalHits);

            // Mark as complete
            stats.setComplete(true);
            stats.setLastUpdated(LocalDateTime.now());
            saveStatistics(stats);

            // Print summary
            printSummary(stats);

        } finally {
            // Close all log files
            System.out.println();
            System.out.println("Closing log files...");
            for (PrintWriter writer : logWriters.values()) {
                writer.close();
            }
            System.out.println("Done.");
        }
    }

    private Statistics loadOrCreateStatistics(int totalBooks) {
        File statsFile = new File(STATISTICS_FILE);
        if (statsFile.exists()) {
            try {
                Statistics stats = objectMapper.readValue(statsFile, Statistics.class);
                stats.setTotalBooks(totalBooks);
                System.out.println("Resuming from previous run. " +
                        stats.getBooksProcessed() + "/" + totalBooks + " books already processed.");
                return stats;
            } catch (IOException e) {
                System.out.println("Could not load existing statistics, starting fresh: " + e.getMessage());
            }
        }

        Statistics stats = new Statistics();
        stats.setTotalBooks(totalBooks);
        stats.setBooksProcessed(0);
        stats.setComplete(false);
        stats.setProviders(new HashMap<>());
        stats.setProcessedBookTitles(new ArrayList<>());
        return stats;
    }

    private void saveStatistics(Statistics stats) throws IOException {
        objectMapper.writeValue(new File(STATISTICS_FILE), stats);
    }

    private Map<String, PrintWriter> openLogFiles() throws IOException {
        Map<String, PrintWriter> writers = new HashMap<>();
        for (FreeTextProvider provider : providers) {
            String filename = provider.getProviderName().replaceAll("[^a-zA-Z0-9]", "_") + ".log";
            Path logPath = logDirectory.resolve(filename);
            // Append mode for resume support
            PrintWriter writer = new PrintWriter(new FileWriter(logPath.toFile(), true));
            writers.put(provider.getProviderName(), writer);
        }
        return writers;
    }

    private void logResult(PrintWriter writer, String title, String author, String status, String detail) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        writer.println("========================================");
        writer.println("Book: " + title);
        writer.println("Author: " + (author != null ? author : "Unknown"));
        writer.println("Timestamp: " + LocalDateTime.now().format(formatter));
        writer.println("----------------------------------------");
        writer.println("Status: " + status);
        if (detail != null && !detail.isBlank()) {
            if ("HIT".equals(status)) {
                writer.println("URL: " + detail);
            } else {
                writer.println("Error: " + detail);
            }
        }
        writer.println("========================================");
        writer.println();
    }

    private void printSummary(Statistics stats) {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("LOOKUP STATISTICS SUMMARY");
        System.out.println("=".repeat(60));
        System.out.printf("Total books processed: %d/%d%n", stats.getBooksProcessed(), stats.getTotalBooks());
        System.out.printf("Complete: %s%n", stats.isComplete() ? "Yes" : "No");
        System.out.println();
        System.out.println("Provider Results:");
        System.out.println("-".repeat(60));
        System.out.printf("%-30s %8s %8s %8s%n", "Provider", "Hits", "Lookups", "Hit Rate");
        System.out.println("-".repeat(60));

        stats.getProviders().entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().getHitRate(), a.getValue().getHitRate()))
                .forEach(entry -> {
                    ProviderStats ps = entry.getValue();
                    System.out.printf("%-30s %8d %8d %7.1f%%%n",
                            entry.getKey(),
                            ps.getHits(),
                            ps.getLookups(),
                            ps.getHitRate() * 100);
                });

        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Statistics saved to: " + STATISTICS_FILE);
        System.out.println("Logs saved to: " + LOG_DIRECTORY + "/");
    }

    /**
     * Statistics data model for JSON serialization.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class Statistics {
        private LocalDateTime lastUpdated;
        private int booksProcessed;
        private int totalBooks;
        private boolean complete;
        private Map<String, ProviderStats> providers;
        private List<String> processedBookTitles;
    }

    /**
     * Per-provider statistics.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class ProviderStats {
        private int hits;
        private int lookups;
        private double hitRate;
    }
}
