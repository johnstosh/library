/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Thin wrapper around Jakarta Mail {@code Transport} so SMTP sending can be
 * unit-tested without a real server.
 */
public interface MailTransport {
    void send(MimeMessage message) throws MessagingException;
}
