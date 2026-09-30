/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.EmailChangeQueueEntry;
import com.muczynski.library.email.EmailChangeHandler;
import com.muczynski.library.email.EmailSendException;
import com.muczynski.library.repository.EmailChangeQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Holds before/after JSON until {@code app.email.quiet-period} has passed
 * since the last edit of that loan or application. A change that is undone
 * in the quiet period deletes the row and sends nothing.
 *
 * <p>The JSON lives in the database. Cloud Run's container disk is memory and
 * disappears when the instance stops, so a file would not survive the restart
 * this queue is here to outlast.
 */
@Service
public class EmailChangeQueue {

    private static final Logger logger = LoggerFactory.getLogger(EmailChangeQueue.class);

    private final EmailChangeQueueRepository repository;
    private final ObjectMapper objectMapper;
    private final Map<String, EmailChangeHandler> handlers;
    private final Duration quietPeriod;
    private final Clock clock;

    @Autowired
    public EmailChangeQueue(EmailChangeQueueRepository repository,
                             ObjectMapper objectMapper,
                             List<EmailChangeHandler> handlerList,
                             @Value("${app.email.quiet-period:5m}") Duration quietPeriod) {
        this(repository, objectMapper, handlerList, quietPeriod, Clock.systemUTC());
    }

    EmailChangeQueue(EmailChangeQueueRepository repository,
                     ObjectMapper objectMapper,
                     List<EmailChangeHandler> handlerList,
                     Duration quietPeriod,
                     Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.quietPeriod = quietPeriod;
        this.clock = clock;
        this.handlers = new HashMap<>();
        for (EmailChangeHandler handler : handlerList) {
            this.handlers.put(handler.kind(), handler);
        }
    }

    /**
     * Record a change. {@code before} is kept only for the first edit in the
     * quiet period. Later edits replace {@code after} and restart the wait.
     */
    @Transactional
    public void stage(String subjectKey, String kind, Object before, Object after) {
        String beforeJson = write(before);
        String afterJson = write(after);
        EmailChangeQueueEntry existing = repository.findBySubjectKey(subjectKey).orElse(null);
        if (existing == null) {
            if (same(beforeJson, afterJson)) {
                return;
            }
            EmailChangeQueueEntry created = new EmailChangeQueueEntry();
            created.setSubjectKey(subjectKey);
            created.setKind(kind);
            created.setBeforeJson(beforeJson);
            created.setAfterJson(afterJson);
            created.setSendAfter(clock.instant().plus(quietPeriod));
            repository.save(created);
            return;
        }
        existing.setAfterJson(afterJson);
        if (same(existing.getBeforeJson(), afterJson)) {
            repository.delete(existing);
            logger.info("Email queue dropped {}: the change was undone", subjectKey);
            return;
        }
        existing.setSendAfter(clock.instant().plus(quietPeriod));
        repository.save(existing);
    }

    /**
     * Send every row whose quiet period has ended. A delivery failure leaves
     * that row in place for the next pass.
     */
    @Transactional
    public void flushDue() {
        Instant now = clock.instant();
        for (EmailChangeQueueEntry row : repository.findBySendAfterLessThanEqualOrderBySendAfterAsc(now)) {
            EmailChangeHandler handler = handlers.get(row.getKind());
            if (handler == null) {
                logger.error("No email handler for queued kind {} ({})", row.getKind(), row.getSubjectKey());
                repository.delete(row);
                continue;
            }
            if (same(row.getBeforeJson(), row.getAfterJson())) {
                repository.delete(row);
                continue;
            }
            try {
                handler.send(row.getBeforeJson(), row.getAfterJson());
                repository.delete(row);
            } catch (RuntimeException e) {
                logger.error("Email queue send failed for {}: {}", row.getSubjectKey(), e.getMessage());
            }
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new EmailSendException("Could not store email queue JSON", e);
        }
    }

    private boolean same(String left, String right) {
        try {
            JsonNode a = objectMapper.readTree(left == null ? "null" : left);
            JsonNode b = objectMapper.readTree(right == null ? "null" : right);
            return a.equals(b);
        } catch (JsonProcessingException e) {
            return Objects.equals(left, right);
        }
    }
}
