/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sends queued mail after the quiet period. CPU is throttled between requests,
 * so this timer runs when a request wakes the instance. If the instance stopped
 * first, the row stays in the database and this job sends it the next time the
 * process is up and the quiet period has already passed.
 */
@Component
public class EmailChangeFlushJob {

    private final EmailChangeQueue emailChangeQueue;

    public EmailChangeFlushJob(EmailChangeQueue emailChangeQueue) {
        this.emailChangeQueue = emailChangeQueue;
    }

    @Scheduled(fixedDelayString = "${app.email.flush-interval-ms:15000}")
    public void flush() {
        emailChangeQueue.flushDue();
    }
}
