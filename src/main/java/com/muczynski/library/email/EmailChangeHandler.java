/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

/**
 * Turns one queued before/after JSON pair into an email.
 * Implementations throw {@link EmailSendException} when delivery fails so the
 * queue can retry. Returning normally means the row is done, even when no
 * message was sent (email disabled, no recipients, or no net change).
 */
public interface EmailChangeHandler {

    String kind();

    void send(String beforeJson, String afterJson);
}
