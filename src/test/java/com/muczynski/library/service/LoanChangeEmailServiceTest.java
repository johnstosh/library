/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import com.muczynski.library.email.EmailMessage;
import com.muczynski.library.email.EmailSender;
import com.muczynski.library.email.LoanMailSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanChangeEmailServiceTest {

    @Mock
    private GlobalSettingsService globalSettingsService;

    @Mock
    private ApplicationEmailService applicationEmailService;

    @Mock
    private EmailSender logSender;

    private LoanChangeEmailService service;
    private GlobalSettings settings;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new LoanChangeEmailService(globalSettingsService, applicationEmailService, objectMapper);
        ReflectionTestUtils.setField(service, "externalBaseUrl", "https://library.example.com");
        settings = new GlobalSettings();
        settings.setEmailMethod(EmailMethod.LOG);
        settings.setEmailFromAddress("library@example.com");
        settings.setEmailFromName("Library");
        settings.setEmailNotifyLibrariansOnLoanChange(true);
        settings.setEmailNotifyBorrowerOnLoanChange(true);
        lenient().when(globalSettingsService.settingsForEmail()).thenReturn(settings);
        lenient().when(logSender.getMethod()).thenReturn(EmailMethod.LOG);
        lenient().when(logSender.isConfigured(settings)).thenReturn(true);
        lenient().when(applicationEmailService.effectiveMethod(settings)).thenReturn(EmailMethod.LOG);
        lenient().when(applicationEmailService.senderFor(EmailMethod.LOG)).thenReturn(logSender);
        lenient().when(applicationEmailService.resolveLibrarianRecipients(settings))
                .thenReturn(List.of("librarian@example.com"));
        lenient().when(applicationEmailService.withoutOptedOutUsers(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void changedLoanListsTheDifferenceAndBothRecipients() throws Exception {
        LoanMailSnapshot before = loan(LocalDate.of(2026, 10, 1), null);
        LoanMailSnapshot after = loan(LocalDate.of(2026, 10, 8), null);

        service.send(objectMapper.writeValueAsString(before), objectMapper.writeValueAsString(after));

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(logSender).send(captor.capture(), eq(settings));
        EmailMessage message = captor.getValue();
        assertEquals(List.of("librarian@example.com", "pat@example.com"), message.getTo());
        assertEquals("Library loan changed: The Hobbit", message.getSubject());
        assertTrue(message.getTextBody().contains("Due date: 2026-10-01 → 2026-10-08"));
        assertFalse(message.getTextBody().contains("Loan date:"));
        assertTrue(message.getTextBody().contains("https://library.example.com/loans/7"));
        assertTrue(message.getHtmlBody().contains("Century Schoolbook L"));
    }

    @Test
    void borrowerChangeIncludesThePreviousBorrower() throws Exception {
        LoanMailSnapshot before = loan(LocalDate.of(2026, 10, 1), null);
        LoanMailSnapshot after = new LoanMailSnapshot(7L, "The Hobbit", "Sam", "sam@example.com",
                LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1), null);

        service.send(objectMapper.writeValueAsString(before), objectMapper.writeValueAsString(after));

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(logSender).send(captor.capture(), eq(settings));
        assertEquals(List.of("librarian@example.com", "pat@example.com", "sam@example.com"),
                captor.getValue().getTo());
        assertTrue(captor.getValue().getTextBody().contains("Borrower: Pat → Sam"));
    }

    @Test
    void disabledSendsNothing() throws Exception {
        settings.setEmailMethod(EmailMethod.DISABLED);
        when(applicationEmailService.effectiveMethod(settings)).thenReturn(EmailMethod.DISABLED);
        LoanMailSnapshot after = loan(LocalDate.of(2026, 10, 1), null);

        service.send("null", objectMapper.writeValueAsString(after));

        verify(logSender, never()).send(any(), any());
    }

    private static LoanMailSnapshot loan(LocalDate due, LocalDate returned) {
        return new LoanMailSnapshot(7L, "The Hobbit", "Pat", "pat@example.com",
                LocalDate.of(2026, 9, 30), due, returned);
    }
}
