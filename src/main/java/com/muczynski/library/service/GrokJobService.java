/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.dto.GrokJobDto;
import com.muczynski.library.exception.BookHasNoPhotosException;
import com.muczynski.library.exception.GrokCreditsExhaustedException;
import com.muczynski.library.exception.GrokJobsBusyException;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.exception.ResourceNotFoundException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Runs long Grok actions in the background and keeps their status in a small in-memory map.
 *
 * <p>Why: a single Grok call can take minutes (grok-4.7 ~200s), longer than the browser /
 * proxy will keep one fetch open ("Failed to fetch" even though the server succeeded).
 * The start endpoint returns 202 with a job id at once; the browser polls
 * {@code GET /api/grok-jobs/{jobId}} every few seconds.</p>
 *
 * <p>Memory (512Mi container): at most {@link #MAX_JOBS} jobs are kept, finished jobs expire
 * after {@link #FINISHED_TTL}, and only the small result DTO / error text is retained (never
 * photo bytes). At most {@link #WORKER_THREADS} jobs run at once with a short queue.</p>
 *
 * <p>Works because prod runs a single Cloud Run instance with CPU always allocated
 * (deploy.sh: --max-instances 1, --no-cpu-throttling). The job's work must not rely on an
 * ambient DB transaction; the service methods it calls open their own short transactions.</p>
 */
@Service
public class GrokJobService {

    private static final Logger log = LoggerFactory.getLogger(GrokJobService.class);

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_FAILED = "FAILED";

    static final int MAX_JOBS = 50;
    static final int WORKER_THREADS = 3;
    static final int QUEUE_CAPACITY = 20;
    static final Duration FINISHED_TTL = Duration.ofMinutes(15);
    /** Safety net: a job still RUNNING this long is dropped (AskGrok read timeout is 10 minutes). */
    static final Duration RUNNING_TTL = Duration.ofMinutes(45);

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor;
    private final Clock clock;

    public GrokJobService() {
        this(Clock.systemUTC());
    }

    GrokJobService(Clock clock) {
        this.clock = clock;
        AtomicInteger threadNumber = new AtomicInteger();
        this.executor = new ThreadPoolExecutor(
                WORKER_THREADS, WORKER_THREADS, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(runnable, "grok-job-" + threadNumber.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        this.executor.allowCoreThreadTimeOut(true);
    }

    /**
     * Start {@code work} in the background as the current user. Returns the RUNNING status.
     *
     * @throws GrokJobsBusyException when too many jobs are queued or kept
     */
    public GrokJobDto start(String kind, Supplier<?> work) {
        purgeExpired();
        if (jobs.size() >= MAX_JOBS) {
            evictOldestFinished();
        }
        if (jobs.size() >= MAX_JOBS) {
            throw new GrokJobsBusyException();
        }

        Job job = new Job(UUID.randomUUID().toString(), kind, currentOwner(), clock.instant());
        jobs.put(job.id, job);
        Runnable task = new DelegatingSecurityContextRunnable(() -> run(job, work));
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            jobs.remove(job.id);
            throw new GrokJobsBusyException();
        }
        log.info("Started Grok job {} ({})", job.id, kind);
        return job.toDto();
    }

    /** Status of a job owned by the current user; empty when unknown, expired, or someone else's. */
    public Optional<GrokJobDto> get(String jobId) {
        purgeExpired();
        Job job = jobId == null ? null : jobs.get(jobId);
        if (job == null || !Objects.equals(job.owner, currentOwner())) {
            return Optional.empty();
        }
        return Optional.of(job.toDto());
    }

    int jobCount() {
        return jobs.size();
    }

    private void run(Job job, Supplier<?> work) {
        try {
            Object result = work.get();
            job.finish(STATUS_SUCCEEDED, result, null, null, clock.instant());
            log.info("Grok job {} ({}) succeeded", job.id, job.kind);
        } catch (Throwable t) {
            int status = httpStatusFor(t);
            String message = messageFor(t);
            job.finish(STATUS_FAILED, null, message, status, clock.instant());
            if (status >= 500) {
                log.warn("Grok job {} ({}) failed: {}", job.id, job.kind, t.getMessage(), t);
            } else {
                log.info("Grok job {} ({}) failed with {}: {}", job.id, job.kind, status, message);
            }
        }
    }

    /** Same statuses the synchronous endpoints / GlobalExceptionHandler use. */
    static int httpStatusFor(Throwable t) {
        if (t instanceof GrokCreditsExhaustedException) {
            return 402;
        }
        if (t instanceof BookHasNoPhotosException || t instanceof IllegalArgumentException) {
            return 400;
        }
        if (t instanceof ResourceNotFoundException) {
            return 404;
        }
        if (t instanceof LibraryException) {
            return 422;
        }
        return 500;
    }

    static String messageFor(Throwable t) {
        String message = t.getMessage();
        if (message == null || message.isBlank()) {
            return "Grok request failed: " + t.getClass().getSimpleName();
        }
        return message;
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        jobs.values().removeIf(job -> job.isExpired(now));
    }

    private void evictOldestFinished() {
        jobs.values().stream()
                .filter(job -> job.finishedAt != null)
                .min(Comparator.comparing(job -> job.finishedAt))
                .ifPresent(job -> jobs.remove(job.id));
    }

    private static String currentOwner() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static final class Job {
        private final String id;
        private final String kind;
        private final String owner;
        private final Instant startedAt;
        private volatile String status = STATUS_RUNNING;
        private volatile Object result;
        private volatile String error;
        private volatile Integer httpStatus;
        private volatile Instant finishedAt;

        private Job(String id, String kind, String owner, Instant startedAt) {
            this.id = id;
            this.kind = kind;
            this.owner = owner;
            this.startedAt = startedAt;
        }

        private void finish(String status, Object result, String error, Integer httpStatus, Instant at) {
            this.result = result;
            this.error = error;
            this.httpStatus = httpStatus;
            this.finishedAt = at;
            this.status = status;
        }

        private boolean isExpired(Instant now) {
            if (finishedAt != null) {
                return finishedAt.plus(FINISHED_TTL).isBefore(now);
            }
            return startedAt.plus(RUNNING_TTL).isBefore(now);
        }

        private GrokJobDto toDto() {
            String currentStatus = status;
            return GrokJobDto.builder()
                    .jobId(id)
                    .kind(kind)
                    .status(currentStatus)
                    .result(STATUS_SUCCEEDED.equals(currentStatus) ? result : null)
                    .error(error)
                    .httpStatus(httpStatus)
                    .build();
        }
    }
}
