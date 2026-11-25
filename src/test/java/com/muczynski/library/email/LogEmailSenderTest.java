/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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

    @Test
    void send_recordsListUnsubscribeHeaderNamesWithoutTheToken() {
        Logger logger = (Logger) LoggerFactory.getLogger(LogEmailSender.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            EmailMessage message = new EmailMessage();
            message.getTo().add("patron@example.com");
            message.setSubject("Lives of Saints");
            message.setTextBody("Unsubscribe\nhttps://library.example/mailing-lists/unsubscribe/abc123secret");
            message.setListUnsubscribe("https://library.example/mailing-lists/unsubscribe/abc123secret");

            sender.send(message, new GlobalSettings());

            assertEquals(1, appender.list.size());
            String logged = appender.list.get(0).getFormattedMessage();
            assertTrue(logged.contains(EmailMessage.LIST_UNSUBSCRIBE));
            assertTrue(logged.contains(EmailMessage.LIST_UNSUBSCRIBE_POST));
            assertFalse(logged.contains("abc123secret"));
            assertTrue(logged.contains("/mailing-lists/unsubscribe/[redacted]"));
        } finally {
            logger.detachAppender(appender);
        }
    }
}
