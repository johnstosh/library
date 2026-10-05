/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

/**
 * xAI refused a Grok call because the team is out of credits or hit its monthly
 * spending limit (HTTP 402, or 403/429 with a credits / spending-limit body).
 * Handled globally as HTTP 402 with a plain message for the user.
 */
public class GrokCreditsExhaustedException extends RuntimeException {

    public static final String MESSAGE =
            "Grok is out of credits. Add credits or raise the spending limit at console.x.ai, then try again.";

    public GrokCreditsExhaustedException() {
        super(MESSAGE);
    }

    public GrokCreditsExhaustedException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
