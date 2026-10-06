/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

/**
 * Too many background Grok jobs are queued or running. Handled globally as HTTP 503.
 */
public class GrokJobsBusyException extends RuntimeException {

    public static final String MESSAGE = "Too many Grok requests are running right now. Try again in a minute.";

    public GrokJobsBusyException() {
        super(MESSAGE);
    }
}
