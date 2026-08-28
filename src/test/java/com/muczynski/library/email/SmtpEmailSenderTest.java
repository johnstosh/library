/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    @Mock
    private MailTransport mailTransport;

    @Test
    void isConfigured_requiresHostAndFrom() {
        SmtpEmailSender sender = new SmtpEmailSender(mailTransport);
        GlobalSettings settings = new GlobalSettings();
        assertEquals(EmailMethod.SMTP, sender.getMethod());
        assertFalse(sender.isConfigured(settings));

        settings.setSmtpHost("smtp.gmail.com");
        assertFalse(sender.isConfigured(settings));

        settings.setEmailFromAddress("library@example.com");
        assertTrue(sender.isConfigured(settings));
        assertTrue(sender.describeStatus(settings).contains("smtp.gmail.com"));
    }

    @Test
    void send_buildsMimeMessage() throws Exception {
        SmtpEmailSender sender = new SmtpEmailSender(mailTransport);
        GlobalSettings settings = new GlobalSettings();
        settings.setSmtpHost("smtp.example.com");
        settings.setSmtpPort(587);
        settings.setEmailFromAddress("library@example.com");
        settings.setEmailFromName("Library");
        settings.setSmtpUsername("user");
        settings.setSmtpPassword("pass");

        EmailMessage message = new EmailMessage();
        message.getTo().add("librarian@example.com");
        message.setSubject("Library card application pending: Jane");
        message.setTextBody("text");
        message.setHtmlBody("<p>html</p>");

        sender.send(message, settings);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailTransport).send(captor.capture());
        MimeMessage mime = captor.getValue();
        assertEquals("Library card application pending: Jane", mime.getSubject());
        assertEquals("library@example.com", mime.getFrom()[0].toString().contains("library@example.com")
                ? "library@example.com"
                : mime.getFrom()[0].toString());
        assertTrue(mime.getAllRecipients()[0].toString().contains("librarian@example.com"));
    }

    @Test
    void send_withoutConfig_throws() {
        SmtpEmailSender sender = new SmtpEmailSender(mailTransport);
        EmailMessage message = new EmailMessage();
        message.getTo().add("a@b.com");
        EmailSendException ex = assertThrows(EmailSendException.class,
                () -> sender.send(message, new GlobalSettings()));
        assertTrue(ex.getMessage().contains("SMTP host"));
    }
}
