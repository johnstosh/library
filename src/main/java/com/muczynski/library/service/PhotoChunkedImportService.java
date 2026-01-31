/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.ChunkUploadResultDto;
import com.muczynski.library.dto.PhotoZipImportResultDto;
import com.muczynski.library.dto.PhotoZipImportResultDto.PhotoZipImportItemDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoChunkedImportService {

    private final PhotoZipImportService photoZipImportService;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    private final ConcurrentHashMap<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();

    private static class ChunkedUploadState {
        final PipedOutputStream pipedOut;
        final PipedInputStream pipedIn;
        final Thread backgroundThread;
        final BlockingQueue<PhotoZipImportItemDto> resultsQueue;
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        final AtomicInteger skippedCount = new AtomicInteger(0);
        final AtomicInteger totalProcessed = new AtomicInteger(0);
        final List<PhotoZipImportItemDto> allItems = new ArrayList<>();
        final Instant createdAt = Instant.now();
        volatile Exception backgroundError;

        ChunkedUploadState(PipedOutputStream pipedOut, PipedInputStream pipedIn,
                           Thread backgroundThread, BlockingQueue<PhotoZipImportItemDto> resultsQueue) {
            this.pipedOut = pipedOut;
            this.pipedIn = pipedIn;
            this.backgroundThread = backgroundThread;
            this.resultsQueue = resultsQueue;
        }
    }

    public ChunkUploadResultDto processChunk(String uploadId, int chunkIndex, long totalSize,
                                              boolean isLastChunk, byte[] chunkBytes) throws IOException {
        ChunkedUploadState state;

        if (chunkIndex == 0) {
            // First chunk: create pipe pair and start background thread
            PipedOutputStream pipedOut = new PipedOutputStream();
            PipedInputStream pipedIn = new PipedInputStream(pipedOut, 1024 * 1024); // 1MB buffer
            BlockingQueue<PhotoZipImportItemDto> resultsQueue = new LinkedBlockingQueue<>();

            // Pre-load books and authors on the main thread
            List<Book> allBooks = bookRepository.findAll();
            List<Author> allAuthors = authorRepository.findAll();

            Thread bgThread = new Thread(() -> {
                ChunkedUploadState s = activeUploads.get(uploadId);
                try {
                    processZipStream(pipedIn, allBooks, allAuthors, s);
                } catch (Exception e) {
                    if (s != null) {
                        s.backgroundError = e;
                    }
                    log.error("Background ZIP processing failed for upload {}: {}", uploadId, e.getMessage(), e);
                } finally {
                    try {
                        pipedIn.close();
                    } catch (IOException ignored) {
                    }
                }
            }, "zip-import-" + uploadId);
            bgThread.setDaemon(true);

            state = new ChunkedUploadState(pipedOut, pipedIn, bgThread, resultsQueue);
            activeUploads.put(uploadId, state);
            bgThread.start();
        } else {
            state = activeUploads.get(uploadId);
            if (state == null) {
                // Upload session expired or was already completed — return graceful response
                if (chunkIndex > 0) {
                    return ChunkUploadResultDto.builder()
                            .uploadId(uploadId)
                            .chunkIndex(chunkIndex)
                            .processedPhotos(List.of())
                            .complete(true)
                            .errorMessage("Upload session expired or was already completed")
                            .build();
                }
                throw new IllegalStateException("No active upload found for ID: " + uploadId);
            }
        }

        String errorMessage = null;

        // Check for background errors — preserve stats instead of throwing
        if (state.backgroundError != null) {
            errorMessage = "Background processing failed: " + state.backgroundError.getMessage();
            log.warn("Background error detected for upload {}: {}", uploadId, errorMessage);
        }

        // Write chunk bytes to pipe (only if no background error)
        if (errorMessage == null) {
            try {
                state.pipedOut.write(chunkBytes);
                state.pipedOut.flush();
            } catch (IOException e) {
                if (state.backgroundError != null) {
                    errorMessage = "Background processing failed: " + state.backgroundError.getMessage();
                    log.warn("Write failed due to background error for upload {}: {}", uploadId, errorMessage);
                } else {
                    throw e;
                }
            }
        }

        if (isLastChunk || errorMessage != null) {
            // Close the pipe to signal end of data
            try {
                state.pipedOut.close();
            } catch (IOException ignored) {
                // Pipe may already be closed if background thread failed
            }

            // Wait for background thread to finish
            try {
                state.backgroundThread.join(60000); // 60 second timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (errorMessage == null) {
                    errorMessage = "Interrupted while waiting for ZIP processing to complete";
                }
            }

            if (errorMessage == null && state.backgroundError != null) {
                errorMessage = "Background processing failed: " + state.backgroundError.getMessage();
            }
        }

        boolean isComplete = isLastChunk || errorMessage != null;

        // Drain results queue
        List<PhotoZipImportItemDto> newItems = new ArrayList<>();
        state.resultsQueue.drainTo(newItems);

        // Build response
        ChunkUploadResultDto.ChunkUploadResultDtoBuilder builder = ChunkUploadResultDto.builder()
                .uploadId(uploadId)
                .chunkIndex(chunkIndex)
                .processedPhotos(newItems)
                .totalProcessedSoFar(state.totalProcessed.get())
                .totalSuccessSoFar(state.successCount.get())
                .totalFailureSoFar(state.failureCount.get())
                .totalSkippedSoFar(state.skippedCount.get())
                .complete(isComplete)
                .errorMessage(errorMessage);

        if (isComplete) {
            // Drain any remaining items
            List<PhotoZipImportItemDto> remaining = new ArrayList<>();
            state.resultsQueue.drainTo(remaining);
            newItems.addAll(remaining);

            // Build final result from all accumulated items
            synchronized (state.allItems) {
                state.allItems.addAll(newItems);
                builder.processedPhotos(newItems)
                        .totalProcessedSoFar(state.totalProcessed.get())
                        .totalSuccessSoFar(state.successCount.get())
                        .totalFailureSoFar(state.failureCount.get())
                        .totalSkippedSoFar(state.skippedCount.get())
                        .finalResult(PhotoZipImportResultDto.builder()
                                .totalFiles(state.allItems.size())
                                .successCount(state.successCount.get())
                                .failureCount(state.failureCount.get())
                                .skippedCount(state.skippedCount.get())
                                .items(new ArrayList<>(state.allItems))
                                .build());
            }
        } else {
            synchronized (state.allItems) {
                state.allItems.addAll(newItems);
            }
        }

        return builder.build();
    }

    public boolean removeUpload(String uploadId) {
        ChunkedUploadState removed = activeUploads.remove(uploadId);
        if (removed != null) {
            log.info("Upload {} removed by client", uploadId);
            return true;
        }
        return false;
    }

    private void processZipStream(PipedInputStream pipedIn, List<Book> allBooks, List<Author> allAuthors,
                                   ChunkedUploadState state) throws IOException {
        int entryCount = 0;

        try (ZipInputStream zis = new ZipInputStream(pipedIn)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryPath = entry.getName();
                if (photoZipImportService.shouldSkipEntry(entryPath)) {
                    log.debug("Skipping hidden/resource fork file: {}", entryPath);
                    zis.closeEntry();
                    continue;
                }

                String filename = photoZipImportService.getFilenameFromPath(entryPath);

                // processEntry is public and @Transactional (via class-level annotation on
                // PhotoZipImportService), so Spring's proxy manages the transaction.
                // This ensures the EntityManager used for saves is the same one cleared by
                // clearPersistenceContext(), preventing memory leaks and data issues.
                try {
                    PhotoZipImportItemDto item = photoZipImportService.processEntry(filename, zis, allBooks, allAuthors);

                    switch (item.getStatus()) {
                        case "SUCCESS" -> state.successCount.incrementAndGet();
                        case "FAILURE" -> state.failureCount.incrementAndGet();
                        case "SKIPPED" -> state.skippedCount.incrementAndGet();
                    }
                    state.totalProcessed.incrementAndGet();
                    state.resultsQueue.add(item);
                } catch (Exception e) {
                    PhotoZipImportItemDto errorItem = PhotoZipImportItemDto.builder()
                            .filename(filename)
                            .status("FAILURE")
                            .errorMessage("Processing failed: " + e.getMessage())
                            .build();
                    state.failureCount.incrementAndGet();
                    state.totalProcessed.incrementAndGet();
                    state.resultsQueue.add(errorItem);
                    log.error("Failed to process ZIP entry {}: {}", filename, e.getMessage(), e);
                }

                entryCount++;
                // Periodically clear the persistence context to release memory
                if (entryCount % 20 == 0) {
                    photoZipImportService.clearPersistenceContext();
                    log.info("Cleared entity manager after {} entries", entryCount);
                }

                zis.closeEntry();
            }
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupStaleUploads() {
        Instant cutoff = Instant.now().minusSeconds(1800); // 30 minutes
        activeUploads.entrySet().removeIf(entry -> {
            if (entry.getValue().createdAt.isBefore(cutoff)) {
                log.warn("Cleaning up stale upload: {}", entry.getKey());
                try {
                    entry.getValue().pipedOut.close();
                } catch (IOException ignored) {
                }
                entry.getValue().backgroundThread.interrupt();
                return true;
            }
            return false;
        });
    }
}
