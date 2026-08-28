/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Records the email in application logs. Useful for verifying the pending-
 * application flow in Cloud Run or locally without a mail vendor.
 */
@Component
public class LogEmailSender implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public EmailMethod getMethod() {
        return EmailMethod.LOG;
    }

    @Override
    public boolean isConfigured(GlobalSettings settings) {
        return true;
    }

    @Override
    public String describeStatus(GlobalSettings settings) {
        return "Ready (messages are written to application logs, not delivered)";
    }

    @Override
    public void send(EmailMessage message, GlobalSettings settings) {
        logger.info(
                "Email [{}] from={} <{}> to={} subject={} body={}",
                message.getEvent() != null ? message.getEvent() : "message",
                message.getFromName(),
                message.getFromAddress(),
                message.getTo(),
                message.getSubject(),
                message.getTextBody());
    }
}
