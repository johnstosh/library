/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.PhotoUploadSession;
import com.muczynski.library.dto.ChunkUploadResultDto;
import com.muczynski.library.dto.PhotoZipImportResultDto;
import com.muczynski.library.dto.PhotoZipImportResultDto.PhotoZipImportItemDto;
import com.muczynski.library.dto.ResumeInfoDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoUploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.muczynski.library.repository.AuthorZipImportProjection;
import com.muczynski.library.repository.BookZipImportProjection;
import com.muczynski.library.repository.LoanZipImportProjection;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    static final long CHUNK_SIZE = 10L * 1024 * 1024; // 10MB - must match frontend

    private final PhotoZipImportService photoZipImportService;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final LoanRepository loanRepository;
    private final PhotoUploadSessionRepository uploadSessionRepository;

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
        volatile Instant lastActivityAt = Instant.now();
        volatile Exception backgroundError;
        /** Bytes consumed by ZipInputStream through the last completed entry */
        volatile long totalBytesConsumed;
        /** Last chunk index that was successfully received */
        volatile int lastChunkIndex;

        ChunkedUploadState(PipedOutputStream pipedOut, PipedInputStream pipedIn,
                           Thread backgroundThread, BlockingQueue<PhotoZipImportItemDto> resultsQueue) {
            this.pipedOut = pipedOut;
            this.pipedIn = pipedIn;
            this.backgroundThread = backgroundThread;
            this.resultsQueue = resultsQueue;
        }
    }

    public ChunkUploadResultDto processChunk(String uploadId, int chunkIndex,
                                              boolean isLastChunk, byte[] chunkBytes) throws IOException {
        return processChunk(uploadId, chunkIndex, isLastChunk, chunkBytes, -1, 0);
    }

    public ChunkUploadResultDto processChunk(String uploadId, int chunkIndex,
                                              boolean isLastChunk, byte[] chunkBytes,
                                              int resumeFromProcessed, long bytesToSkip) throws IOException {
        ChunkedUploadState state;
        boolean isResume = resumeFromProcessed >= 0;

        log.info("[{}] processChunk: chunkIndex={} isLastChunk={} bytes={} isResume={} resumeFrom={}",
                uploadId, chunkIndex, isLastChunk, chunkBytes.length, isResume, resumeFromProcessed);

        if (chunkIndex == 0 || isResume) {
            // First chunk (or first chunk of a resume): pre-load DB data on the HTTP thread, then
            // connect a 1MB pipe — HTTP thread blocks the moment the pipe fills, keeping memory tight.
            log.info("[{}] Loading books/authors/loans from DB for pre-load cache", uploadId);
            long preloadStart = System.currentTimeMillis();
            List<BookZipImportProjection> allBooks = bookRepository.findBy();
            List<AuthorZipImportProjection> allAuthors = authorRepository.findBy();
            List<LoanZipImportProjection> allLoans = loanRepository.findAllForZipImport();
            log.info("[{}] Pre-load complete in {}ms: {} books, {} authors, {} loans",
                    uploadId, System.currentTimeMillis() - preloadStart,
                    allBooks.size(), allAuthors.size(), allLoans.size());

            PipedOutputStream pipedOut = new PipedOutputStream();
            PipedInputStream pipedIn = new PipedInputStream(pipedOut, 1024 * 1024); // 1MB buffer
            BlockingQueue<PhotoZipImportItemDto> resultsQueue = new LinkedBlockingQueue<>();

            int entriesToSkip = isResume ? resumeFromProcessed : 0;

            Thread bgThread = new Thread(() -> {
                ChunkedUploadState s = activeUploads.get(uploadId);
                log.info("[{}] Background thread started, entriesToSkip={}", uploadId, entriesToSkip);
                try {
                    processZipStream(pipedIn, allBooks, allAuthors, allLoans, s, uploadId, entriesToSkip);
                    log.info("[{}] Background thread finished successfully: success={} failure={} skipped={}",
                            uploadId,
                            s != null ? s.successCount.get() : "?",
                            s != null ? s.failureCount.get() : "?",
                            s != null ? s.skippedCount.get() : "?");
                } catch (Exception e) {
                    if (s != null) {
                        s.backgroundError = e;
                    }
                    log.error("[{}] Background ZIP processing failed: {}", uploadId, e.getMessage(), e);
                } finally {
                    try { pipedIn.close(); } catch (IOException ignored) {}
                }
            }, "zip-import-" + uploadId);
            bgThread.setDaemon(true);

            state = new ChunkedUploadState(pipedOut, pipedIn, bgThread, resultsQueue);

            if (isResume) {
                state.successCount.set(getResumeCountFromDb(uploadId, "success"));
                state.failureCount.set(getResumeCountFromDb(uploadId, "failure"));
                state.skippedCount.set(getResumeCountFromDb(uploadId, "skipped"));
                state.totalProcessed.set(resumeFromProcessed);
                log.info("[{}] Resume: restored counters success={} failure={} skipped={} totalProcessed={}",
                        uploadId, state.successCount.get(), state.failureCount.get(),
                        state.skippedCount.get(), state.totalProcessed.get());
            }

            activeUploads.put(uploadId, state);
            bgThread.start();

            // Note: no byte trimming on resume. ZIP must be parsed from byte 0;
            // the background thread skips already-processed entries instead.
        } else {
            state = activeUploads.get(uploadId);
            if (state == null) {
                log.warn("[{}] No active upload found for chunkIndex={} — session expired or already completed",
                        uploadId, chunkIndex);
                return ChunkUploadResultDto.builder()
                        .uploadId(uploadId)
                        .chunkIndex(chunkIndex)
                        .processedPhotos(List.of())
                        .complete(true)
                        .errorMessage("Upload session expired or was already completed")
                        .build();
            }
        }

        state.lastActivityAt = Instant.now();
        state.lastChunkIndex = chunkIndex;

        String errorMessage = null;

        // Check for background errors — preserve stats instead of throwing
        if (state.backgroundError != null) {
            errorMessage = "Background processing failed: " + state.backgroundError.getMessage();
            log.warn("[{}] Background error detected at chunkIndex={}: {}", uploadId, chunkIndex, errorMessage);
        }

        // Write chunk bytes into the pipe — blocks when the 1MB buffer is full (back-pressure)
        if (errorMessage == null && chunkBytes.length > 0) {
            log.info("[{}] Writing chunk {} ({} bytes) to pipe", uploadId, chunkIndex, chunkBytes.length);
            try {
                state.pipedOut.write(chunkBytes);
                state.pipedOut.flush();
            } catch (IOException e) {
                if (state.backgroundError != null) {
                    errorMessage = "Background processing failed: " + state.backgroundError.getMessage();
                    log.warn("[{}] Pipe write failed due to background error at chunkIndex={}: {}",
                            uploadId, chunkIndex, errorMessage);
                } else {
                    throw e;
                }
            }
        }

        if (isLastChunk || errorMessage != null) {
            log.info("[{}] Last chunk or error — closing pipe and waiting for background thread", uploadId);
            // Close the pipe to signal end of stream to background thread
            try { state.pipedOut.close(); } catch (IOException ignored) {}

            // Wait for background thread to finish processing all enqueued data
            long joinStart = System.currentTimeMillis();
            try {
                state.backgroundThread.join(300000); // 5 minute timeout
                log.info("[{}] Background thread joined in {}ms", uploadId, System.currentTimeMillis() - joinStart);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (errorMessage == null) {
                    errorMessage = "Interrupted while waiting for ZIP processing to complete";
                }
                log.warn("[{}] Interrupted while joining background thread after {}ms",
                        uploadId, System.currentTimeMillis() - joinStart);
            }

            if (errorMessage == null && state.backgroundError != null) {
                errorMessage = "Background processing failed: " + state.backgroundError.getMessage();
            }

            markSessionComplete(uploadId);
            log.info("[{}] Session marked complete. Final: success={} failure={} skipped={} total={}",
                    uploadId, state.successCount.get(), state.failureCount.get(),
                    state.skippedCount.get(), state.totalProcessed.get());
        }

        boolean isComplete = isLastChunk || errorMessage != null;

        // Drain results queue
        List<PhotoZipImportItemDto> newItems = new ArrayList<>();
        state.resultsQueue.drainTo(newItems);
        log.info("[{}] Chunk {} response: {} new items drained, totalProcessed={}, complete={}",
                uploadId, chunkIndex, newItems.size(), state.totalProcessed.get(), isComplete);

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
            if (!remaining.isEmpty()) {
                log.info("[{}] Drained {} additional items after completion", uploadId, remaining.size());
            }
            newItems.addAll(remaining);

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

    public ResumeInfoDto getResumeInfo(String uploadId) {
        Optional<PhotoUploadSession> session = uploadSessionRepository.findByUploadId(uploadId);
        if (session.isEmpty()) {
            log.info("[{}] getResumeInfo: no session found", uploadId);
            return null;
        }
        PhotoUploadSession s = session.get();
        if (s.isComplete()) {
            log.info("[{}] getResumeInfo: session already complete", uploadId);
            return null;
        }
        log.info("[{}] getResumeInfo: resuming from totalProcessed={} success={} failure={} skipped={}",
                uploadId, s.getTotalProcessed(), s.getSuccessCount(), s.getFailureCount(), s.getSkippedCount());
        // Always resume from chunk 0 with no byte skipping.
        // ZIP is a streaming format that must be parsed from byte 0;
        // the background thread uses entriesToSkip to skip already-processed entries.
        return ResumeInfoDto.builder()
                .uploadId(uploadId)
                .resumeFromChunkIndex(0)
                .bytesToSkipInChunk(0)
                .totalProcessed(s.getTotalProcessed())
                .successCount(s.getSuccessCount())
                .failureCount(s.getFailureCount())
                .skippedCount(s.getSkippedCount())
                .build();
    }

    public boolean removeUpload(String uploadId) {
        ChunkedUploadState removed = activeUploads.remove(uploadId);
        if (removed != null) {
            log.info("[{}] Upload removed by client", uploadId);
            // Signal background thread to stop if still running
            try { removed.pipedOut.close(); } catch (IOException ignored) {}
            return true;
        }
        return false;
    }

    private void processZipStream(InputStream inputStream, List<BookZipImportProjection> allBooks,
                                   List<AuthorZipImportProjection> allAuthors,
                                   List<LoanZipImportProjection> allLoans, ChunkedUploadState state, String uploadId,
                                   int entriesToSkip) throws IOException {
        int entryCount = 0;
        CountingInputStream countingIn = new CountingInputStream(inputStream);

        log.info("[{}] processZipStream starting, entriesToSkip={}", uploadId, entriesToSkip);

        try (ZipInputStream zis = new ZipInputStream(countingIn)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryPath = entry.getName();
                if (photoZipImportService.shouldSkipEntry(entryPath)) {
                    log.debug("[{}] Skipping hidden/resource fork file: {}", uploadId, entryPath);
                    zis.closeEntry();
                    continue;
                }

                String filename = photoZipImportService.getFilenameFromPath(entryPath);

                // If resuming, skip entries that were already processed
                if (entryCount < entriesToSkip) {
                    byte[] buf = new byte[8192];
                    while (zis.read(buf) != -1) {
                        // discard
                    }
                    zis.closeEntry();
                    entryCount++;
                    log.debug("[{}] Resume: skipped already-processed entry {} ({}/{})",
                            uploadId, filename, entryCount, entriesToSkip);
                    state.totalBytesConsumed = countingIn.getCount();
                    continue;
                }

                log.info("[{}] Processing entry #{}: {} (bytesConsumed={})",
                        uploadId, entryCount + 1, filename, countingIn.getCount());
                long entryStart = System.currentTimeMillis();

                try {
                    PhotoZipImportItemDto item = photoZipImportService.processEntry(
                            filename, zis, allBooks, allAuthors, allLoans);

                    long entryMs = System.currentTimeMillis() - entryStart;
                    log.info("[{}] Entry #{} {} completed in {}ms: status={}",
                            uploadId, entryCount + 1, filename, entryMs, item.getStatus());

                    switch (item.getStatus()) {
                        case "SUCCESS" -> state.successCount.incrementAndGet();
                        case "FAILURE" -> state.failureCount.incrementAndGet();
                        case "SKIPPED" -> state.skippedCount.incrementAndGet();
                    }
                    state.totalProcessed.incrementAndGet();
                    state.resultsQueue.add(item);
                } catch (Exception e) {
                    long entryMs = System.currentTimeMillis() - entryStart;
                    log.error("[{}] Entry #{} {} FAILED after {}ms: {}",
                            uploadId, entryCount + 1, filename, entryMs, e.getMessage(), e);
                    PhotoZipImportItemDto errorItem = PhotoZipImportItemDto.builder()
                            .filename(filename)
                            .status("FAILURE")
                            .errorMessage("Processing failed: " + e.getMessage())
                            .build();
                    state.failureCount.incrementAndGet();
                    state.totalProcessed.incrementAndGet();
                    state.resultsQueue.add(errorItem);
                }

                entryCount++;
                // Periodically clear the persistence context to release memory
                if (entryCount % 20 == 0) {
                    log.info("[{}] Clearing persistence context after {} entries", uploadId, entryCount);
                    photoZipImportService.clearPersistenceContext();
                }

                zis.closeEntry();

                // Record byte position after each completed entry for resume capability
                state.totalBytesConsumed = countingIn.getCount();
                saveProgressToDb(uploadId, state);
            }
        }

        log.info("[{}] processZipStream complete: {} entries processed (skipped {})",
                uploadId, entryCount - entriesToSkip, entriesToSkip);
    }

    private void saveProgressToDb(String uploadId, ChunkedUploadState state) {
        long start = System.currentTimeMillis();
        try {
            PhotoUploadSession session = uploadSessionRepository.findByUploadId(uploadId)
                    .orElseGet(() -> {
                        PhotoUploadSession s = new PhotoUploadSession();
                        s.setUploadId(uploadId);
                        return s;
                    });
            session.setTotalProcessed(state.totalProcessed.get());
            session.setSuccessCount(state.successCount.get());
            session.setFailureCount(state.failureCount.get());
            session.setSkippedCount(state.skippedCount.get());
            session.setLastChunkIndex(state.lastChunkIndex);
            session.setTotalBytesConsumed(state.totalBytesConsumed);
            uploadSessionRepository.save(session);
            log.debug("[{}] saveProgressToDb: totalProcessed={} in {}ms",
                    uploadId, state.totalProcessed.get(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("[{}] saveProgressToDb failed after {}ms: {}",
                    uploadId, System.currentTimeMillis() - start, e.getMessage());
        }
    }

    private void markSessionComplete(String uploadId) {
        try {
            uploadSessionRepository.findByUploadId(uploadId).ifPresent(session -> {
                session.setComplete(true);
                uploadSessionRepository.save(session);
            });
        } catch (Exception e) {
            log.warn("Failed to mark session {} as complete in DB: {}", uploadId, e.getMessage());
        }
    }

    private int getResumeCountFromDb(String uploadId, String type) {
        return uploadSessionRepository.findByUploadId(uploadId)
                .map(s -> switch (type) {
                    case "success" -> s.getSuccessCount();
                    case "failure" -> s.getFailureCount();
                    case "skipped" -> s.getSkippedCount();
                    default -> 0;
                })
                .orElse(0);
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupStaleUploads() {
        Instant cutoff = Instant.now().minusSeconds(1800); // 30 minutes
        activeUploads.entrySet().removeIf(entry -> {
            if (entry.getValue().lastActivityAt.isBefore(cutoff)) {
                log.warn("Cleaning up stale upload: {}", entry.getKey());
                try { entry.getValue().pipedOut.close(); } catch (IOException ignored) {}
                entry.getValue().backgroundThread.interrupt();
                return true;
            }
            return false;
        });

        // Also clean up old DB sessions (24 hours)
        try {
            Instant dbCutoff = Instant.now().minusSeconds(86400);
            uploadSessionRepository.deleteByLastActivityAtBefore(dbCutoff);
        } catch (Exception e) {
            log.warn("Failed to clean up old DB upload sessions: {}", e.getMessage());
        }
    }
}
