/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.GlobalSettings;
import com.muczynski.library.email.EmailMessage;
import com.muczynski.library.email.EmailSender;
import com.muczynski.library.email.OutgoingEmail;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * One notice message per recipient. A user gets an account unsubscribe link
 * and the one-click headers. An address that is not a user is sent without them.
 */
@Component
public class AccountNoticeMail {

    private final OutgoingEmail outgoingEmail;
    private final NotificationUnsubscribeService unsubscribes;

    public AccountNoticeMail(OutgoingEmail outgoingEmail, NotificationUnsubscribeService unsubscribes) {
        this.outgoingEmail = outgoingEmail;
        this.unsubscribes = unsubscribes;
    }

    public void send(EmailSender sender, GlobalSettings settings, List<String> recipients, Draft draft) {
        if (recipients == null || recipients.isEmpty() || draft == null) {
            return;
        }
        for (String recipient : recipients) {
            if (recipient == null || recipient.isBlank()) {
                continue;
            }
            EmailMessage message = new EmailMessage();
            message.getTo().add(recipient.trim());
            message.setFromAddress(draft.fromAddress());
            message.setFromName(draft.fromName());
            message.setEvent(draft.event());
            message.setSubject(draft.subject());
            if (draft.payload() != null) {
                message.getEventPayload().putAll(draft.payload());
            }
            String url = unsubscribes.urlForAddress(recipient);
            if (url != null) {
                message.setListUnsubscribe(url);
            }
            outgoingEmail.apply(message, draft.textBody(), draft.htmlBody(), url);
            sender.send(message, settings);
        }
    }

    public record Draft(String fromAddress,
                        String fromName,
                        String event,
                        String subject,
                        Map<String, Object> payload,
                        String textBody,
                        String htmlBody) {
    }
}
