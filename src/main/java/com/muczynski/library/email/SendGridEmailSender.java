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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SendGrid v3 HTTP API. HTTPS outbound works on Cloud Run where SMTP port 25
 * is blocked.
 */
@Component
public class SendGridEmailSender implements EmailSender {

    static final String SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send";

    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailSender.class);

    private final RestTemplate restTemplate;

    public SendGridEmailSender(@Qualifier("emailRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public EmailMethod getMethod() {
        return EmailMethod.SENDGRID;
    }

    @Override
    public boolean isConfigured(GlobalSettings settings) {
        return hasText(settings.getSendGridApiKey()) && hasText(settings.getEmailFromAddress());
    }

    @Override
    public String describeStatus(GlobalSettings settings) {
        if (!hasText(settings.getSendGridApiKey())) {
            return "SendGrid API key is required";
        }
        if (!hasText(settings.getEmailFromAddress())) {
            return "From address is required";
        }
        return "Ready (SendGrid HTTP API)";
    }

    @Override
    public void send(EmailMessage message, GlobalSettings settings) {
        if (!isConfigured(settings)) {
            throw new EmailSendException(describeStatus(settings));
        }

        Map<String, Object> body = buildRequestBody(message, settings);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(settings.getSendGridApiKey().trim());

        try {
            logger.info("Sending SendGrid email to {}", message.getTo());
            ResponseEntity<String> response = restTemplate.exchange(
                    SENDGRID_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new EmailSendException("SendGrid returned HTTP " + response.getStatusCode().value());
            }
        } catch (EmailSendException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new EmailSendException(
                    "SendGrid returned HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new EmailSendException("SendGrid send failed: " + e.getMessage(), e);
        }
    }

    Map<String, Object> buildRequestBody(EmailMessage message, GlobalSettings settings) {
        List<Map<String, String>> to = new ArrayList<>();
        for (String address : message.getTo()) {
            to.add(Map.of("email", address));
        }
        Map<String, Object> personalization = new LinkedHashMap<>();
        personalization.put("to", to);

        Map<String, String> from = new LinkedHashMap<>();
        from.put("email", settings.getEmailFromAddress().trim());
        if (hasText(settings.getEmailFromName())) {
            from.put("name", settings.getEmailFromName().trim());
        }

        List<Map<String, String>> content = new ArrayList<>();
        content.add(Map.of(
                "type", "text/plain",
                "value", message.getTextBody() != null ? message.getTextBody() : ""));
        if (hasText(message.getHtmlBody())) {
            content.add(Map.of("type", "text/html", "value", message.getHtmlBody()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("personalizations", List.of(personalization));
        body.put("from", from);
        body.put("subject", message.getSubject() != null ? message.getSubject() : "");
        body.put("content", content);
        return body;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
