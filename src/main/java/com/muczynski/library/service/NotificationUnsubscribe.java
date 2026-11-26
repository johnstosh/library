/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

/**
 * Result of turning off account email notices. The token is not kept here.
 */
public record NotificationUnsubscribe(Kind kind) {

    public enum Kind {
        UNSUBSCRIBED,
        ALREADY,
        UNKNOWN
    }
}
