/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.domain;

/**
 * How often one subscription is sent. One frequency per email and list.
 * Changing it means unsubscribing and signing up again.
 */
public enum MailingFrequency {
    DAILY,
    WEEKLY,
    MONTHLY;

    public String word() {
        return switch (this) {
            case DAILY -> "daily";
            case WEEKLY -> "weekly";
            case MONTHLY -> "monthly";
        };
    }

    public String sentence() {
        return "Frequency: " + word() + ". To change frequency, unsubscribe and sign up again.";
    }
}
