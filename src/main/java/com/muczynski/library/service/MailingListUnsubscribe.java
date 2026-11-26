/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

/**
 * Result of one unsubscribe. The token that was presented is not kept here.
 */
public record MailingListUnsubscribe(Kind kind, String listName) {

    public enum Kind {
        UNSUBSCRIBED,
        ALREADY,
        UNKNOWN
    }
}
