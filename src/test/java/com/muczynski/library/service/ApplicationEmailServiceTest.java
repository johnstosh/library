/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import com.muczynski.library.dto.TestEmailResultDto;
import com.muczynski.library.email.EmailMessage;
import com.muczynski.library.email.EmailSendException;
import com.muczynski.library.email.EmailSender;
import com.muczynski.library.email.PendingApplicationNotice;
import com.muczynski.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationEmailServiceTest {

    @Mock
    private GlobalSettingsService globalSettingsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailSender logSender;

    private ApplicationEmailService service;
    private GlobalSettings settings;

    @BeforeEach
    void setUp() {
        when(logSender.getMethod()).thenReturn(EmailMethod.LOG);
        service = new ApplicationEmailService(globalSettingsService, userRepository, List.of(logSender));
        ReflectionTestUtils.setField(service, "externalBaseUrl", "https://library.example.com");
        settings = new GlobalSettings();
        settings.setEmailMethod(EmailMethod.LOG);
        settings.setEmailFromAddress("library@example.com");
        settings.setEmailNotifyLibrariansOnPending(true);
        settings.setEmailNotifyApplicantOnPending(false);
        settings.setEmailIncludeLibrarianUserEmails(false);
        settings.setEmailLibrarianRecipients("librarian@example.com");
        lenient().when(globalSettingsService.getGlobalSettings()).thenReturn(settings);
        lenient().when(logSender.isConfigured(settings)).thenReturn(true);
    }

    @Test
    void disabled_doesNotSend() {
        settings.setEmailMethod(EmailMethod.DISABLED);
        service.sendPendingNotifications(notice());
        verify(logSender, never()).send(any(), any());
    }

    @Test
    void pending_notifiesLibrarians() {
        service.sendPendingNotifications(notice());

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(logSender).send(captor.capture(), eq(settings));
        EmailMessage message = captor.getValue();
        assertEquals(List.of("librarian@example.com"), message.getTo());
        assertTrue(message.getSubject().contains("Jane Doe"));
        assertEquals("library.application.pending", message.getEvent());
        assertTrue(message.getTextBody().contains("https://library.example.com/applications"));
        assertEquals(7L, message.getEventPayload().get("applicationId"));
    }

    @Test
    void pending_includesLibrarianUserEmails() {
        settings.setEmailIncludeLibrarianUserEmails(true);
        when(userRepository.findLibrarianEmails()).thenReturn(List.of("staff@example.com", "librarian@example.com"));

        service.sendPendingNotifications(notice());

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(logSender).send(captor.capture(), eq(settings));
        assertEquals(List.of("librarian@example.com", "staff@example.com"), captor.getValue().getTo());
    }

    @Test
    void pending_notifiesApplicantWhenEnabled() {
        settings.setEmailNotifyApplicantOnPending(true);
        service.sendPendingNotifications(notice());

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(logSender, times(2)).send(captor.capture(), eq(settings));
        List<EmailMessage> sent = captor.getAllValues();
        assertEquals("library.application.pending", sent.get(0).getEvent());
        assertEquals("library.application.pending.applicant", sent.get(1).getEvent());
        assertEquals(List.of("jane@example.com"), sent.get(1).getTo());
        assertTrue(sent.get(1).getSubject().contains("received"));
    }

    @Test
    void pending_skipsApplicantWithoutEmail() {
        settings.setEmailNotifyApplicantOnPending(true);
        service.sendPendingNotifications(new PendingApplicationNotice(7L, "Jane Doe", null));
        verify(logSender, times(1)).send(any(), eq(settings));
    }

    @Test
    void pending_sendFailureDoesNotThrow() {
        doThrow(new EmailSendException("boom")).when(logSender).send(any(), any());
        assertDoesNotThrow(() -> service.sendPendingNotifications(notice()));
    }

    @Test
    void testEmail_disabledExplainsWhy() {
        settings.setEmailMethod(EmailMethod.DISABLED);
        TestEmailResultDto result = service.sendTestEmail("me@example.com");
        assertFalse(result.isSent());
        assertEquals(EmailMethod.DISABLED, result.getMethod());
        assertTrue(result.getMessage().contains("DISABLED"));
    }

    @Test
    void testEmail_sendsToOverride() {
        TestEmailResultDto result = service.sendTestEmail("override@example.com");
        assertTrue(result.isSent());
        assertEquals(List.of("override@example.com"), result.getRecipients());
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(logSender).send(captor.capture(), eq(settings));
        assertEquals("library.email.test", captor.getValue().getEvent());
    }

    @Test
    void testEmail_reportsTransportError() {
        doThrow(new EmailSendException("SMTP down")).when(logSender).send(any(), any());
        TestEmailResultDto result = service.sendTestEmail("me@example.com");
        assertFalse(result.isSent());
        assertEquals("SMTP down", result.getMessage());
    }

    private static PendingApplicationNotice notice() {
        return new PendingApplicationNotice(7L, "Jane Doe", "jane@example.com");
    }
}
