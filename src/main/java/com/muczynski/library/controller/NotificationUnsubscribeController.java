/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.email.HtmlText;
import com.muczynski.library.service.NotificationUnsubscribe;
import com.muczynski.library.service.NotificationUnsubscribeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Public one-click unsubscribe for loan and application notices.
 * No login. The token is a path secret and is not logged.
 */
@RestController
public class NotificationUnsubscribeController {

    private static final MediaType HTML = new MediaType("text", "html", StandardCharsets.UTF_8);
    private static final MediaType TEXT = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final NotificationUnsubscribeService unsubscribes;

    public NotificationUnsubscribeController(NotificationUnsubscribeService unsubscribes) {
        this.unsubscribes = unsubscribes;
    }

    @GetMapping(value = "/notifications/unsubscribe/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribeByGet(@PathVariable("token") String token) {
        return html(unsubscribes.unsubscribe(token));
    }

    /**
     * One-click unsubscribe. Mail clients POST {@code List-Unsubscribe=One-Click}.
     * The body is not required. A repeated POST stays unsubscribed.
     */
    @PostMapping("/notifications/unsubscribe/{token}")
    public ResponseEntity<String> unsubscribeByPost(@PathVariable("token") String token) {
        return text(unsubscribes.unsubscribe(token));
    }

    private ResponseEntity<String> html(NotificationUnsubscribe result) {
        String message = sentence(result);
        String title = switch (result.kind()) {
            case UNSUBSCRIBED -> "Unsubscribed";
            case ALREADY -> "Already unsubscribed";
            case UNKNOWN -> "Unsubscribe link";
        };
        String page = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>"
                + HtmlText.escape(title) + "</title></head><body style=\"font-family: Century Schoolbook L, "
                + "Century Schoolbook, Times New Roman, Times, serif;\"><p>"
                + HtmlText.escape(message) + "</p></body></html>";
        return ResponseEntity.status(status(result)).contentType(HTML).body(page);
    }

    private ResponseEntity<String> text(NotificationUnsubscribe result) {
        return ResponseEntity.status(status(result)).contentType(TEXT).body(sentence(result));
    }

    private static HttpStatus status(NotificationUnsubscribe result) {
        return result.kind() == NotificationUnsubscribe.Kind.UNKNOWN ? HttpStatus.NOT_FOUND : HttpStatus.OK;
    }

    private static String sentence(NotificationUnsubscribe result) {
        return switch (result.kind()) {
            case UNSUBSCRIBED -> "You will no longer receive email notifications.";
            case ALREADY -> "You are already unsubscribed from email notifications.";
            case UNKNOWN -> "This unsubscribe link is not valid.";
        };
    }
}
