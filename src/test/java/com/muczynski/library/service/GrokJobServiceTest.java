/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.dto.GrokJobDto;
import com.muczynski.library.exception.BookHasNoPhotosException;
import com.muczynski.library.exception.GrokCreditsExhaustedException;
import com.muczynski.library.exception.GrokJobsBusyException;
import com.muczynski.library.exception.LibraryException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GrokJobServiceTest {

    /** Clock the test can move forward to exercise TTL expiry. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-05T23:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    private MutableClock clock;
    private GrokJobService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        service = new GrokJobService(clock);
        loginAs("7");
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, "n/a", List.of()));
    }

    private GrokJobDto awaitFinished(String jobId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            GrokJobDto job = service.get(jobId).orElseThrow();
            if (!GrokJobService.STATUS_RUNNING.equals(job.getStatus())) {
                return job;
            }
            Thread.sleep(10);
        }
        fail("Job did not finish in time");
        return null;
    }

    @Test
    void start_returnsRunningThenSucceededWithResult() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        GrokJobDto started = service.start("book-from-title-author", () -> {
            await(release);
            return Map.of("title", "Pride and Prejudice");
        });

        assertNotNull(started.getJobId());
        assertEquals(GrokJobService.STATUS_RUNNING, started.getStatus());
        assertEquals("book-from-title-author", started.getKind());
        assertEquals(GrokJobService.STATUS_RUNNING, service.get(started.getJobId()).orElseThrow().getStatus());

        release.countDown();
        GrokJobDto done = awaitFinished(started.getJobId());
        assertEquals(GrokJobService.STATUS_SUCCEEDED, done.getStatus());
        assertEquals(Map.of("title", "Pride and Prejudice"), done.getResult());
        assertNull(done.getError());
    }

    @Test
    void worker_runsWithTheStartingUsersSecurityContext() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        GrokJobDto started = service.start("k", () -> {
            seen.set(SecurityContextHolder.getContext().getAuthentication().getName());
            return "ok";
        });
        awaitFinished(started.getJobId());
        assertEquals("7", seen.get());
    }

    @Test
    void failures_carryTheSameStatusAndMessageAsTheSyncEndpoints() throws Exception {
        GrokJobDto credits = service.start("k", () -> {
            throw new GrokCreditsExhaustedException();
        });
        GrokJobDto noPhotos = service.start("k", () -> {
            throw new BookHasNoPhotosException(BookHasNoPhotosException.BOOK_FROM_IMAGE_MESSAGE);
        });
        GrokJobDto other = service.start("k", () -> {
            throw new IllegalStateException("boom");
        });
        GrokJobDto library = service.start("k", () -> {
            throw new LibraryException("Title is required");
        });

        GrokJobDto creditsDone = awaitFinished(credits.getJobId());
        assertEquals(GrokJobService.STATUS_FAILED, creditsDone.getStatus());
        assertEquals(402, creditsDone.getHttpStatus());
        assertEquals(GrokCreditsExhaustedException.MESSAGE, creditsDone.getError());
        assertNull(creditsDone.getResult());

        GrokJobDto noPhotosDone = awaitFinished(noPhotos.getJobId());
        assertEquals(400, noPhotosDone.getHttpStatus());
        assertEquals("This book has no photos, so Book from Image has nothing to read.", noPhotosDone.getError());

        GrokJobDto otherDone = awaitFinished(other.getJobId());
        assertEquals(500, otherDone.getHttpStatus());
        assertEquals("boom", otherDone.getError());

        GrokJobDto libraryDone = awaitFinished(library.getJobId());
        assertEquals(422, libraryDone.getHttpStatus());
        assertEquals("Title is required", libraryDone.getError());
    }

    @Test
    void get_hidesOtherUsersJobsAndUnknownIds() throws Exception {
        GrokJobDto started = service.start("k", () -> "ok");
        awaitFinished(started.getJobId());

        loginAs("8");
        assertTrue(service.get(started.getJobId()).isEmpty());
        assertTrue(service.get("no-such-job").isEmpty());
    }

    @Test
    void finishedJobs_expireAfterTtl() throws Exception {
        GrokJobDto started = service.start("k", () -> "ok");
        awaitFinished(started.getJobId());

        clock.advance(GrokJobService.FINISHED_TTL.minusSeconds(1));
        assertTrue(service.get(started.getJobId()).isPresent());

        clock.advance(Duration.ofSeconds(2));
        assertTrue(service.get(started.getJobId()).isEmpty());
        assertEquals(0, service.jobCount());
    }

    @Test
    void start_isBoundedAndRejectsWhenFullOfRunningJobs() {
        CountDownLatch release = new CountDownLatch(1);
        try {
            int accepted = 0;
            GrokJobsBusyException busy = null;
            for (int i = 0; i < GrokJobService.MAX_JOBS + 5 && busy == null; i++) {
                try {
                    service.start("k", () -> {
                        await(release);
                        return "ok";
                    });
                    accepted++;
                } catch (GrokJobsBusyException e) {
                    busy = e;
                }
            }
            assertNotNull(busy, "expected the job queue to fill up");
            assertEquals(GrokJobsBusyException.MESSAGE, busy.getMessage());
            assertTrue(accepted <= GrokJobService.WORKER_THREADS + GrokJobService.QUEUE_CAPACITY);
            assertEquals(accepted, service.jobCount());
        } finally {
            release.countDown();
        }
    }

    @Test
    void start_evictsOldestFinishedJobWhenMapIsFull() throws Exception {
        String first = null;
        for (int i = 0; i < GrokJobService.MAX_JOBS; i++) {
            GrokJobDto job = service.start("k", () -> "ok");
            awaitFinished(job.getJobId());
            if (first == null) {
                first = job.getJobId();
            }
            clock.advance(Duration.ofMillis(1));
        }
        assertEquals(GrokJobService.MAX_JOBS, service.jobCount());

        GrokJobDto extra = service.start("k", () -> "ok");
        awaitFinished(extra.getJobId());
        assertEquals(GrokJobService.MAX_JOBS, service.jobCount());
        assertTrue(service.get(first).isEmpty(), "oldest finished job should be evicted");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
