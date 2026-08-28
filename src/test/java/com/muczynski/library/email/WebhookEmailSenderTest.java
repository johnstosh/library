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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookEmailSenderTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void isConfigured_requiresUrl() {
        WebhookEmailSender sender = new WebhookEmailSender(restTemplate);
        GlobalSettings settings = new GlobalSettings();
        assertEquals(EmailMethod.WEBHOOK, sender.getMethod());
        assertFalse(sender.isConfigured(settings));
        settings.setWebhookUrl("https://hooks.example.com/email");
        assertTrue(sender.isConfigured(settings));
    }

    @Test
    @SuppressWarnings("unchecked")
    void send_postsJsonPayloadWithBearerToken() {
        WebhookEmailSender sender = new WebhookEmailSender(restTemplate);
        GlobalSettings settings = new GlobalSettings();
        settings.setWebhookUrl("https://hooks.example.com/email");
        settings.setWebhookBearerToken("secret-token");
        settings.setEmailFromAddress("library@example.com");

        EmailMessage message = new EmailMessage();
        message.getTo().add("librarian@example.com");
        message.setSubject("Pending");
        message.setTextBody("text");
        message.setHtmlBody("<p>html</p>");
        message.setEvent("library.application.pending");
        message.getEventPayload().put("applicationId", 9L);

        when(restTemplate.exchange(eq("https://hooks.example.com/email"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        sender.send(message, settings);

        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq("https://hooks.example.com/email"), eq(HttpMethod.POST),
                captor.capture(), eq(String.class));

        HttpEntity<Map<String, Object>> entity = captor.getValue();
        assertEquals("Bearer secret-token", entity.getHeaders().getFirst("Authorization"));
        Map<String, Object> body = entity.getBody();
        assertNotNull(body);
        assertEquals("library.application.pending", body.get("event"));
        assertEquals("Pending", body.get("subject"));
        assertEquals("text", body.get("text"));
        Map<String, Object> payload = (Map<String, Object>) body.get("payload");
        assertEquals(9L, payload.get("applicationId"));
    }

    @Test
    void send_withoutUrl_throws() {
        WebhookEmailSender sender = new WebhookEmailSender(restTemplate);
        EmailMessage message = new EmailMessage();
        message.getTo().add("a@b.com");
        assertThrows(EmailSendException.class, () -> sender.send(message, new GlobalSettings()));
    }
}
