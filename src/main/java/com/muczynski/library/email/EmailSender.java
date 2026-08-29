/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;

/**
 * One outbound-email transport. Implementations are selected at send time from
 * {@link GlobalSettings#getEmailMethod()}.
 */
public interface EmailSender {

    EmailMethod getMethod();

    /**
     * Whether this transport has the credentials/host it needs on the given settings.
     */
    boolean isConfigured(GlobalSettings settings);

    /**
     * Human-readable reason this transport is not ready, or "Ready" if it is.
     */
    String describeStatus(GlobalSettings settings);

    void send(EmailMessage message, GlobalSettings settings);
}
