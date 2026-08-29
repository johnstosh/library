/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POSTs the email as JSON to a librarian-configured URL. Covers Zapier, n8n,
 * Make, Google Apps Script, and Cloud Functions without a mail vendor.
 */
@Component
public class WebhookEmailSender implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(WebhookEmailSender.class);

    private final RestTemplate restTemplate;

    public WebhookEmailSender(@Qualifier("emailRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public EmailMethod getMethod() {
        return EmailMethod.WEBHOOK;
    }

    @Override
    public boolean isConfigured(GlobalSettings settings) {
        return hasText(settings.getWebhookUrl());
    }

    @Override
    public String describeStatus(GlobalSettings settings) {
        if (!hasText(settings.getWebhookUrl())) {
            return "Webhook URL is required";
        }
        return "Ready (POST JSON to webhook)";
    }

    @Override
    public void send(EmailMessage message, GlobalSettings settings) {
        if (!isConfigured(settings)) {
            throw new EmailSendException(describeStatus(settings));
        }

        String url = settings.getWebhookUrl().trim();
        Map<String, Object> body = buildPayload(message, settings);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (hasText(settings.getWebhookBearerToken())) {
            headers.setBearerAuth(settings.getWebhookBearerToken().trim());
        }

        try {
            logger.info("Posting email webhook {} to {}", message.getEvent(), url);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new EmailSendException("Webhook returned HTTP " + response.getStatusCode().value());
            }
        } catch (EmailSendException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new EmailSendException(
                    "Webhook returned HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new EmailSendException("Webhook send failed: " + e.getMessage(), e);
        }
    }

    Map<String, Object> buildPayload(EmailMessage message, GlobalSettings settings) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", message.getEvent() != null ? message.getEvent() : "library.email");
        body.put("sentAt", Instant.now().toString());
        body.put("to", message.getTo());
        body.put("fromAddress", firstNonBlank(message.getFromAddress(), settings.getEmailFromAddress()));
        body.put("fromName", firstNonBlank(message.getFromName(), settings.getEmailFromName()));
        body.put("subject", message.getSubject());
        body.put("text", message.getTextBody());
        body.put("html", message.getHtmlBody());
        if (!message.getEventPayload().isEmpty()) {
            body.put("payload", message.getEventPayload());
        }
        return body;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (hasText(primary)) {
            return primary.trim();
        }
        if (hasText(fallback)) {
            return fallback.trim();
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
