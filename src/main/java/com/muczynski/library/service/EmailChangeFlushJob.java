/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sends queued mail after the quiet period. Cloud Run keeps an idle instance
 * for a while after the last request (at most 15 minutes). The service is
 * deployed with CPU allocated while idle so this timer can run during that
 * window. If the instance is stopped first, the row stays in the database and
 * this job sends it the next time the process is up and the quiet period has
 * already passed.
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
