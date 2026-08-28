/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

/**
 * Thrown when a chosen email transport cannot deliver a message.
 * Callers that notify on pending applications must catch this so a mail
 * failure never rolls back the application itself.
 */
public class EmailSendException extends RuntimeException {
    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
