/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

/**
 * Discriminator stored with a queued before/after snapshot.
 */
public final class EmailChangeKinds {

    public static final String LOAN = "LOAN";
    public static final String APPLICATION = "APPLICATION";

    private EmailChangeKinds() {
    }
}
