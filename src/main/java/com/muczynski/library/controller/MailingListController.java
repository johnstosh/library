/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.MailingListTopic;
import com.muczynski.library.domain.SsoAccounts;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.ErrorResponse;
import com.muczynski.library.dto.MailingListSignupRequest;
import com.muczynski.library.dto.MailingListSignupResponse;
import com.muczynski.library.email.EmailAddresses;
import com.muczynski.library.email.HtmlText;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.MailingListService;
import com.muczynski.library.service.MailingListUnsubscribe;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Logged-in signup and public one-click unsubscribe.
 * Signup uses the account email and ignores any address in the body.
 * The unsubscribe token is a path secret and is not logged.
 */
@RestController
public class MailingListController {

    private static final MediaType HTML = new MediaType("text", "html", StandardCharsets.UTF_8);
    private static final MediaType TEXT = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final MailingListService mailingListService;
    private final UserRepository userRepository;

    public MailingListController(MailingListService mailingListService, UserRepository userRepository) {
        this.mailingListService = mailingListService;
        this.userRepository = userRepository;
    }

    @PostMapping("/api/mailing-lists/subscriptions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> subscribe(@RequestBody MailingListSignupRequest request, Principal principal) {
        if (request == null || request.getFrequency() == null) {
            return bad("INVALID_FREQUENCY", "Choose a frequency.");
        }
        List<MailingListTopic> lists = new ArrayList<>();
        if (request.getLists() != null) {
            request.getLists().stream().filter(Objects::nonNull).distinct().forEach(lists::add);
        }
        if (lists.isEmpty()) {
            return bad("INVALID_LIST", "Choose at least one mailing list.");
        }
        User user = userRepository.findById(extractUserId(principal))
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        if (!EmailAddresses.isValid(user.getEmail())) {
            if (SsoAccounts.isSso(user)) {
                return bad("SSO_EMAIL_REQUIRED", "An email address cannot be added here.");
            }
            return bad("INVALID_EMAIL", "Save an email address before joining a list.");
        }
        MailingListSignupResponse response = mailingListService.subscribe(
                user.getEmail(), lists, request.getFrequency());
        return ResponseEntity.ok(response);
    }

    private Long extractUserId(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return Long.parseLong(principal.getName());
    }

    @GetMapping(value = "/mailing-lists/unsubscribe/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribeByGet(@PathVariable("token") String token) {
        return html(mailingListService.unsubscribe(token));
    }

    /**
     * One-click unsubscribe. Mail clients POST {@code List-Unsubscribe=One-Click}.
     * The body is not required; a repeated POST stays unsubscribed.
     */
    @PostMapping("/mailing-lists/unsubscribe/{token}")
    public ResponseEntity<String> unsubscribeByPost(@PathVariable("token") String token) {
        return text(mailingListService.unsubscribe(token));
    }

    private ResponseEntity<String> html(MailingListUnsubscribe result) {
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

    private ResponseEntity<String> text(MailingListUnsubscribe result) {
        return ResponseEntity.status(status(result)).contentType(TEXT).body(sentence(result));
    }

    private static HttpStatus status(MailingListUnsubscribe result) {
        return result.kind() == MailingListUnsubscribe.Kind.UNKNOWN ? HttpStatus.NOT_FOUND : HttpStatus.OK;
    }

    private static String sentence(MailingListUnsubscribe result) {
        return switch (result.kind()) {
            case UNSUBSCRIBED -> "You have been unsubscribed from " + result.listName() + ".";
            case ALREADY -> "You are already unsubscribed from " + result.listName() + ".";
            case UNKNOWN -> "This unsubscribe link is not valid.";
        };
    }

    private static ResponseEntity<ErrorResponse> bad(String code, String message) {
        return ResponseEntity.badRequest().body(new ErrorResponse(code, message));
    }
}
