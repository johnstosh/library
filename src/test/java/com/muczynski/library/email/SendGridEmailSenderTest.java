/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendGridEmailSenderTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void isConfigured_requiresApiKeyAndFrom() {
        SendGridEmailSender sender = new SendGridEmailSender(restTemplate);
        GlobalSettings settings = new GlobalSettings();
        assertEquals(EmailMethod.SENDGRID, sender.getMethod());
        assertFalse(sender.isConfigured(settings));
        settings.setSendGridApiKey("SG.abc");
        settings.setEmailFromAddress("library@example.com");
        assertTrue(sender.isConfigured(settings));
    }

    @Test
    @SuppressWarnings("unchecked")
    void send_postsSendGridPayload() {
        SendGridEmailSender sender = new SendGridEmailSender(restTemplate);
        GlobalSettings settings = new GlobalSettings();
        settings.setSendGridApiKey("SG.test-key");
        settings.setEmailFromAddress("library@example.com");
        settings.setEmailFromName("Library");

        EmailMessage message = new EmailMessage();
        message.getTo().add("librarian@example.com");
        message.setSubject("Pending");
        message.setTextBody("text body");
        message.setHtmlBody("<p>html</p>");

        when(restTemplate.exchange(eq(SendGridEmailSender.SENDGRID_URL), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("", HttpStatus.ACCEPTED));

        sender.send(message, settings);

        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(SendGridEmailSender.SENDGRID_URL), eq(HttpMethod.POST),
                captor.capture(), eq(String.class));

        HttpEntity<Map<String, Object>> entity = captor.getValue();
        assertEquals("Bearer SG.test-key", entity.getHeaders().getFirst("Authorization"));
        Map<String, Object> body = entity.getBody();
        assertNotNull(body);
        assertEquals("Pending", body.get("subject"));
        Map<String, String> from = (Map<String, String>) body.get("from");
        assertEquals("library@example.com", from.get("email"));
        List<Map<String, Object>> personalizations = (List<Map<String, Object>>) body.get("personalizations");
        List<Map<String, String>> to = (List<Map<String, String>>) personalizations.get(0).get("to");
        assertEquals("librarian@example.com", to.get(0).get("email"));
    }

    @Test
    void send_withoutConfig_throws() {
        SendGridEmailSender sender = new SendGridEmailSender(restTemplate);
        EmailMessage message = new EmailMessage();
        message.getTo().add("a@b.com");
        assertThrows(EmailSendException.class, () -> sender.send(message, new GlobalSettings()));
    }
}
