/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogEmailSenderTest {

    private final LogEmailSender sender = new LogEmailSender();

    @Test
    void isAlwaysConfigured() {
        GlobalSettings settings = new GlobalSettings();
        assertEquals(EmailMethod.LOG, sender.getMethod());
        assertTrue(sender.isConfigured(settings));
        assertTrue(sender.describeStatus(settings).contains("logs"));
    }

    @Test
    void send_doesNotThrow() {
        EmailMessage message = new EmailMessage();
        message.getTo().add("librarian@example.com");
        message.setSubject("Pending");
        message.setTextBody("A card application is pending");
        message.setEvent("library.application.pending");
        assertDoesNotThrow(() -> sender.send(message, new GlobalSettings()));
    }
}
