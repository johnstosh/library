/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.muczynski.library.dto.importdtos.ImportBookDto;
import com.muczynski.library.dto.importdtos.ImportRequestDto;
import com.muczynski.library.service.ImportService;
import com.muczynski.library.util.PasswordHashingUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that gathers statistics about free text lookup providers
 * and compares production free-text links against the latest algorithm/cache.
 * <p>
 * Production JSON is downloaded once from {@code GET /api/import/json} and cached
 * in a temp file so later runs (and the long provider scan) reuse it.
 * <p>
 * Compare existing links with:
 * {@code RUN_FREETEXT_PROD_COMPARE=true ./gradlew test --tests '*.FreeTextLookupStatisticsTest.compareExistingLinksAgainstLatestAlgorithm'}
 * <p>
 * Full provider scan (long-running):
 * {@code ./gradlew test --tests '*.FreeTextLookupStatisticsTest.gatherLookupStatistics'}
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("manual")
class FreeTextLookupStatisticsTest {

    private static final String PROD_BASE = "https://library.muczynskifamily.com";
    private static final Path EXPORT_CACHE = Path.of(System.getProperty("java.io.tmpdir"),
            "library-prod-export.json");
    private static final Path CACHE_REFRESH_FILE = Path.of(System.getProperty("java.io.tmpdir"),
            "freetext-cache-refresh-adds.java");
    private static final Path COMPARE_REPORT_FILE = Path.of(System.getProperty("java.io.tmpdir"),
            "freetext-link-compare.txt");
    private static final String STATISTICS_FILE = "./book-lookup-statistics.json";
    private static final String LOG_DIRECTORY = "./book-lookup";
    private static final String PROGRESS_FILE = "./book-lookup-progress.log";

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
    @Disabled("Long-running provider scan - run manually")
    void gatherLookupStatistics() throws Exception {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("FREE TEXT LOOKUP STATISTICS TEST");
        System.out.println("Started: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("=".repeat(60));
        System.out.println();

        Path archiveFile = loadOrDownloadExport();
        System.out.println("Using export file: " + archiveFile.toAbsolutePath());
        System.out.println("Export file size: " + (Files.size(archiveFile) / 1024) + " KB");

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
        Map<String, Object> archive = objectMapper.readValue(archiveFile.toFile(), new TypeReference<>() {});
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

        // Create progress file for real-time monitoring (tail -f ./book-lookup-progress.log)
        PrintWriter progressWriter = new PrintWriter(new FileWriter(PROGRESS_FILE, false));
        progressWriter.println("=".repeat(60));
        progressWriter.println("FREE TEXT LOOKUP STATISTICS - PROGRESS LOG");
        progressWriter.println("Started: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        progressWriter.println("Monitor with: tail -f " + PROGRESS_FILE);
        progressWriter.println("=".repeat(60));
        progressWriter.flush();

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("STARTING BOOK PROCESSING");
        System.out.println("Monitor progress with: tail -f " + PROGRESS_FILE);
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

                // Write to progress file (can be tailed)
                progressWriter.printf("%n[%d/%d] %s - \"%s\" by %s%n",
                        bookIndex, books.size(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        title, authorName != null ? authorName : "Unknown");
                progressWriter.flush();

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
                            logResult(logWriters.get(providerName), title, authorName, "HIT", result.getUrl(), providerTime);
                            System.out.printf("  %-25s [%4dms] HIT: %s%n", providerName, providerTime, result.getUrl());
                        } else {
                            // Truncate error message (some providers return full HTML bodies)
                            String errMsg = result.getErrorMessage() != null ? result.getErrorMessage() : "Not found";
                            String truncatedErr = errMsg.length() > 100 ? errMsg.substring(0, 100) + "..." : errMsg;
                            logResult(logWriters.get(providerName), title, authorName, "MISS", truncatedErr, providerTime);
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
                        // Truncate error message for logging (some errors contain full HTML pages)
                        String errorMsg = e.getMessage() != null ? e.getMessage() : "null";
                        String truncatedError = errorMsg.length() > 100 ? errorMsg.substring(0, 100) + "..." : errorMsg;
                        logResult(logWriters.get(providerName), title, authorName, "ERROR", truncatedError, providerTime);
                        System.out.printf("  %-25s [%4dms] ERROR: %s%n", providerName, providerTime,
                                errorMsg.length() > 50 ? errorMsg.substring(0, 50) + "..." : errorMsg);
                    }
                }

                long bookTime = System.currentTimeMillis() - bookStartTime;
                System.out.printf("  Book complete: %d hits from %d providers in %dms%n",
                        bookHits, providers.size(), bookTime);
                System.out.flush(); // Force output to show immediately

                progressWriter.printf("  -> %d hits in %dms%n", bookHits, bookTime);
                progressWriter.flush();

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
            progressWriter.println();
            progressWriter.println("Test completed at " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            progressWriter.close();
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

    private void logResult(PrintWriter writer, String title, String author, String status, String detail, long durationMs) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        writer.println("========================================");
        writer.println("Book: " + title);
        writer.println("Author: " + (author != null ? author : "Unknown"));
        writer.println("Timestamp: " + LocalDateTime.now().format(formatter));
        writer.println("Duration: " + durationMs + "ms");
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
     * Download the production JSON export (cached in a temp file) and compare each
     * book's stored freeTextUrl against what the latest cache/algorithm would write
     * if lookup overwrote the field. A 2→1 drop is a lost link.
     * <p>
     * Also writes suggested {@code add()} calls to refresh the cache with production
     * titles and the union of existing URLs.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_FREETEXT_PROD_COMPARE", matches = "true")
    void compareExistingLinksAgainstLatestAlgorithm() throws Exception {
        Path exportFile = loadOrDownloadExport();
        ImportRequestDto export = objectMapper.readValue(exportFile.toFile(), ImportRequestDto.class);
        List<ImportBookDto> books = export.getBooks() != null ? export.getBooks() : List.of();

        System.out.println();
        System.out.println("=".repeat(72));
        System.out.println("FREE TEXT LINK COMPARISON (production export vs latest algorithm)");
        System.out.println("Export: " + exportFile.toAbsolutePath());
        System.out.println("Books in export: " + books.size());
        System.out.println("=".repeat(72));

        int withLinks = 0;
        int cacheHits = 0;
        int cacheNotFound = 0;
        int cacheMisses = 0;
        int overwriteLosses = 0;
        int mergeSafe = 0;
        List<String> lossLines = new ArrayList<>();
        List<String> missingFromCache = new ArrayList<>();
        List<String> cacheRefreshAdds = new ArrayList<>();

        for (ImportBookDto book : books) {
            String title = book.getTitle();
            if (title == null || title.isBlank()) {
                continue;
            }
            String author = book.getAuthorName();
            List<String> existingUrls = FreeTextLookupCache.extractUrls(book.getFreeTextUrl());
            if (existingUrls.isEmpty()) {
                continue;
            }
            withLinks++;

            String cached = FreeTextLookupCache.lookup(author, title);
            List<String> lost;
            if (cached == null) {
                cacheMisses++;
                missingFromCache.add(formatBook(author, title) + "  existing=" + existingUrls);
                // Cache miss: first provider hit would replace the field with a single URL.
                lost = existingUrls.size() > 1 ? existingUrls.subList(1, existingUrls.size()) : List.of();
            } else if (cached.isBlank()) {
                cacheNotFound++;
                missingFromCache.add(formatBook(author, title)
                        + "  cached as NOT FOUND but production has " + existingUrls);
                // Cached "not found" does not overwrite the book, so links stay
                lost = List.of();
            } else {
                cacheHits++;
                lost = FreeTextLookupCache.urlsLostIfReplaced(book.getFreeTextUrl(), cached);
            }

            if (!lost.isEmpty()) {
                overwriteLosses++;
                lossLines.add(formatBook(author, title)
                        + "\n    existing (" + existingUrls.size() + "): " + existingUrls
                        + "\n    algorithm/cache: " + FreeTextLookupCache.extractUrls(cached)
                        + "\n    WOULD LOSE: " + lost);
            }

            String merged = FreeTextLookupCache.mergeUrls(book.getFreeTextUrl(),
                    cached != null && !cached.isBlank() ? cached : book.getFreeTextUrl());
            if (FreeTextLookupCache.urlsLostIfReplaced(book.getFreeTextUrl(), merged).isEmpty()) {
                mergeSafe++;
            }

            LinkedHashSet<String> union = new LinkedHashSet<>(existingUrls);
            union.addAll(FreeTextLookupCache.extractUrls(cached));
            cacheRefreshAdds.add(toAddCall(author, title, union));
        }

        Files.writeString(CACHE_REFRESH_FILE, String.join("\n", cacheRefreshAdds) + "\n");

        StringBuilder report = new StringBuilder();
        report.append("Books with free-text URLs:     ").append(withLinks).append('\n');
        report.append("Cache hits (has URLs):         ").append(cacheHits).append('\n');
        report.append("Cache 'not found' marker:      ").append(cacheNotFound).append('\n');
        report.append("Cache miss (never searched):   ").append(cacheMisses).append('\n');
        report.append("Overwrite would lose URLs:     ").append(overwriteLosses).append('\n');
        report.append("Safe if lookup merges URLs:    ").append(mergeSafe).append('\n');
        if (!lossLines.isEmpty()) {
            report.append("\nBOOKS THAT WOULD LOSE LINKS IF LOOKUP OVERWROTE:\n");
            lossLines.forEach(line -> report.append(line).append('\n'));
        }
        if (!missingFromCache.isEmpty()) {
            report.append("\nPRODUCTION TITLES TO ADD/REFRESH IN CACHE:\n");
            missingFromCache.forEach(line -> report.append(line).append('\n'));
        }
        Files.writeString(COMPARE_REPORT_FILE, report.toString());

        System.out.println();
        System.out.printf("Books with free-text URLs:     %d%n", withLinks);
        System.out.printf("Cache hits (has URLs):         %d%n", cacheHits);
        System.out.printf("Cache 'not found' marker:      %d%n", cacheNotFound);
        System.out.printf("Cache miss (never searched):   %d%n", cacheMisses);
        System.out.printf("Overwrite would lose URLs:     %d%n", overwriteLosses);
        System.out.printf("Safe if lookup merges URLs:    %d%n", mergeSafe);
        System.out.println("Cache refresh add() calls:    " + CACHE_REFRESH_FILE);
        System.out.println("Comparison report:            " + COMPARE_REPORT_FILE);

        if (!lossLines.isEmpty()) {
            System.out.println();
            System.out.println("BOOKS THAT WOULD LOSE LINKS IF LOOKUP OVERWROTE:");
            System.out.println("-".repeat(72));
            lossLines.forEach(System.out::println);
        }
        if (!missingFromCache.isEmpty()) {
            System.out.println();
            System.out.println("PRODUCTION TITLES TO ADD/REFRESH IN CACHE:");
            System.out.println("-".repeat(72));
            missingFromCache.forEach(line -> System.out.println("  " + line));
        }

        System.out.println();
        System.out.println("=".repeat(72));
        // Merge is the runtime guarantee; overwrite losses are reported above for research.
        assertTrue(mergeSafe == withLinks,
                "Merging algorithm results into existing freeTextUrl must not drop links");
        System.out.printf("Answer: overwrite would lose URLs on %d book(s); merge keeps all %d.%n",
                overwriteLosses, withLinks);
    }

    /**
     * Download GET /api/import/json from production once and reuse the temp file.
     */
    Path loadOrDownloadExport() throws Exception {
        if (Files.exists(EXPORT_CACHE) && Files.size(EXPORT_CACHE) > 1000) {
            System.out.println("Using cached production export: " + EXPORT_CACHE.toAbsolutePath()
                    + " (" + (Files.size(EXPORT_CACHE) / 1024) + " KB)");
            return EXPORT_CACHE;
        }

        System.out.println("Downloading production JSON export to " + EXPORT_CACHE.toAbsolutePath());
        CookieManager cookies = new CookieManager();
        cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        String hashed = PasswordHashingUtil.hashPasswordSHA256(ImportService.DEFAULT_PASSWORD);
        String loginJson = "{\"username\":\"librarian\",\"password\":\"" + hashed + "\"}";
        HttpRequest login = HttpRequest.newBuilder()
                .uri(URI.create(PROD_BASE + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                .build();
        HttpResponse<String> loginResponse = client.send(login, HttpResponse.BodyHandlers.ofString());
        if (loginResponse.statusCode() == 429) {
            throw new IllegalStateException("Production rate-limited login (429). Wait 3 minutes and retry.");
        }
        if (loginResponse.statusCode() != 200) {
            throw new IllegalStateException("Login failed: HTTP " + loginResponse.statusCode()
                    + " " + loginResponse.body());
        }

        HttpRequest export = HttpRequest.newBuilder()
                .uri(URI.create(PROD_BASE + "/api/import/json"))
                .timeout(Duration.ofMinutes(3))
                .GET()
                .build();
        HttpResponse<Path> exportResponse = client.send(export,
                HttpResponse.BodyHandlers.ofFile(EXPORT_CACHE));
        if (exportResponse.statusCode() == 429) {
            Files.deleteIfExists(EXPORT_CACHE);
            throw new IllegalStateException("Production rate-limited export (429). Wait 3 minutes and retry.");
        }
        if (exportResponse.statusCode() != 200) {
            Files.deleteIfExists(EXPORT_CACHE);
            throw new IllegalStateException("Export failed: HTTP " + exportResponse.statusCode());
        }
        System.out.println("Downloaded export: " + Files.size(EXPORT_CACHE) + " bytes");
        return EXPORT_CACHE;
    }

    private static String formatBook(String author, String title) {
        return "\"" + title + "\" by " + (author != null ? author : "Unknown");
    }

    private static String toAddCall(String author, String title, Set<String> urls) {
        StringBuilder sb = new StringBuilder();
        sb.append("        add(")
                .append(javaQuote(author != null ? author : ""))
                .append(", ")
                .append(javaQuote(title));
        for (String url : urls) {
            sb.append(",\n            ").append(javaQuote(url));
        }
        sb.append(");");
        return sb.toString();
    }

    private static String javaQuote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * Test with specific books to debug provider behavior:
     * - "Fireside Reader" by Reader's Digest - should NOT be found (copyrighted, only available for borrowing)
     * - "Eight Cousins" by Louisa May Alcott - expected to be found (public domain)
     *
     * Run with: ./gradlew test --tests "*.FreeTextLookupStatisticsTest.testDebugBooks" --console=plain
     */
    @Test
    @Tag("manual")
    @Disabled("Hits live providers - run manually")
    void testDebugBooks() {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("DEBUG BOOKS TEST - ALL PROVIDERS");
        System.out.println("=".repeat(60));
        System.out.println();

        // Sort providers by priority
        providers.sort(Comparator.comparingInt(FreeTextProvider::getPriority));

        // Test 1: Fireside Reader (copyrighted, should NOT be found - only available for borrowing on Internet Archive)
        String title1 = "Fireside Reader";
        String author1 = "Reader's Digest Editors";
        System.out.println("Book 1: \"" + title1 + "\" by " + author1);
        System.out.println("Expected: NOT FOUND in any provider (copyrighted - only borrowing available)");
        System.out.println("-".repeat(60));

        int falsePositives = 0;
        for (FreeTextProvider provider : providers) {
            long startTime = System.currentTimeMillis();
            try {
                FreeTextLookupResult result = provider.search(title1, author1);
                long elapsed = System.currentTimeMillis() - startTime;

                if (result.isFound()) {
                    falsePositives++;
                    System.out.printf("  %-35s [%4dms] FALSE POSITIVE: %s%n",
                            provider.getProviderName(), elapsed, result.getUrl());
                } else {
                    System.out.printf("  %-35s [%4dms] MISS (correct)%n",
                            provider.getProviderName(), elapsed);
                }
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("  %-35s [%4dms] ERROR: %s%n",
                        provider.getProviderName(), elapsed,
                        e.getMessage() != null ? e.getMessage().substring(0, Math.min(50, e.getMessage().length())) : "null");
            }
        }

        System.out.println();
        System.out.println("=".repeat(60));
        if (falsePositives == 0) {
            System.out.println("TEST PASSED: No false positives for copyrighted book");
        } else {
            System.out.println("TEST FAILED: " + falsePositives + " provider(s) returned false positives");
            System.out.println("These providers need to be fixed to distinguish free text from borrowing");
        }
        System.out.println("=".repeat(60));
    }

    /**
     * Test with two specific books to verify provider behavior:
     * - "The Spiritual Exercises" by Ignatius of Loyola - expected to be found in MOST providers
     * - "How the Grinch Stole Christmas" by Dr. Seuss - expected to be found in NONE (copyrighted)
     *
     * Run with: ./gradlew test --tests "*.FreeTextLookupStatisticsTest.testTwoKnownBooks" --console=plain
     */
    @Test
    @Tag("manual")
    @Disabled("Hits live providers - run manually")
    void testTwoKnownBooks() {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("TWO KNOWN BOOKS TEST");
        System.out.println("=".repeat(60));
        System.out.println();

        // Sort providers by priority
        providers.sort(Comparator.comparingInt(FreeTextProvider::getPriority));

        // Test 1: Ignatius - The Spiritual Exercises (public domain, should be found in many providers)
        String ignatiusTitle = "The Spiritual Exercises";
        String ignatiusAuthor = "Ignatius of Loyola";
        int ignatiusHits = 0;

        System.out.println("Book 1: \"" + ignatiusTitle + "\" by " + ignatiusAuthor);
        System.out.println("Expected: Found in MOST providers (public domain classic)");
        System.out.println("-".repeat(60));

        for (FreeTextProvider provider : providers) {
            long startTime = System.currentTimeMillis();
            try {
                FreeTextLookupResult result = provider.search(ignatiusTitle, ignatiusAuthor);
                long elapsed = System.currentTimeMillis() - startTime;

                if (result.isFound()) {
                    ignatiusHits++;
                    System.out.printf("  %-30s [%4dms] HIT: %s%n",
                            provider.getProviderName(), elapsed, result.getUrl());
                } else {
                    System.out.printf("  %-30s [%4dms] MISS%n",
                            provider.getProviderName(), elapsed);
                }
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("  %-30s [%4dms] ERROR: %s%n",
                        provider.getProviderName(), elapsed,
                        e.getMessage() != null ? e.getMessage().substring(0, Math.min(50, e.getMessage().length())) : "null");
            }
        }

        System.out.println();
        System.out.printf("Ignatius result: %d/%d providers found it (%.0f%%)%n",
                ignatiusHits, providers.size(), (100.0 * ignatiusHits / providers.size()));
        System.out.println();

        // Test 2: Dr. Seuss - How the Grinch Stole Christmas (copyrighted, should NOT be found)
        String seussTitle = "How the Grinch Stole Christmas";
        String seussAuthor = "Dr. Seuss";
        int seussHits = 0;

        System.out.println("Book 2: \"" + seussTitle + "\" by " + seussAuthor);
        System.out.println("Expected: Found in NONE (copyrighted, not public domain)");
        System.out.println("-".repeat(60));

        for (FreeTextProvider provider : providers) {
            long startTime = System.currentTimeMillis();
            try {
                FreeTextLookupResult result = provider.search(seussTitle, seussAuthor);
                long elapsed = System.currentTimeMillis() - startTime;

                if (result.isFound()) {
                    seussHits++;
                    System.out.printf("  %-30s [%4dms] HIT: %s%n",
                            provider.getProviderName(), elapsed, result.getUrl());
                } else {
                    System.out.printf("  %-30s [%4dms] MISS%n",
                            provider.getProviderName(), elapsed);
                }
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("  %-30s [%4dms] ERROR: %s%n",
                        provider.getProviderName(), elapsed,
                        e.getMessage() != null ? e.getMessage().substring(0, Math.min(50, e.getMessage().length())) : "null");
            }
        }

        System.out.println();
        System.out.printf("Seuss result: %d/%d providers found it (%.0f%%)%n",
                seussHits, providers.size(), (100.0 * seussHits / providers.size()));

        // Summary and assertions
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("SUMMARY");
        System.out.println("=".repeat(60));
        System.out.printf("Ignatius (Spiritual Exercises): %d/%d hits - %s%n",
                ignatiusHits, providers.size(),
                ignatiusHits >= providers.size() / 2 ? "PASS (found in majority)" : "UNEXPECTED (should be found in most)");
        System.out.printf("Seuss (Grinch): %d/%d hits - %s%n",
                seussHits, providers.size(),
                seussHits == 0 ? "PASS (not found, as expected)" : "UNEXPECTED (should not be found - copyrighted)");
        System.out.println("=".repeat(60));

        // Soft assertions - log but don't fail (providers may vary)
        if (ignatiusHits < providers.size() / 2) {
            System.out.println("WARNING: Ignatius found in fewer than half the providers");
        }
        if (seussHits > 0) {
            System.out.println("WARNING: Seuss was found - these may be unauthorized copies or false positives");
        }
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
