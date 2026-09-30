/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import com.muczynski.library.email.ChangeDescription;
import com.muczynski.library.email.EmailAddresses;
import com.muczynski.library.email.EmailChangeHandler;
import com.muczynski.library.email.EmailChangeKinds;
import com.muczynski.library.email.EmailMessage;
import com.muczynski.library.email.EmailSendException;
import com.muczynski.library.email.EmailSender;
import com.muczynski.library.email.HtmlText;
import com.muczynski.library.email.LoanMailSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * One email for a loan, with the borrower and the librarians together in To.
 */
@Service
public class LoanChangeEmailService implements EmailChangeHandler {

    static final String EVENT = "library.loan.changed";

    private static final Logger logger = LoggerFactory.getLogger(LoanChangeEmailService.class);

    private final GlobalSettingsService globalSettingsService;
    private final ApplicationEmailService applicationEmailService;
    private final ObjectMapper objectMapper;

    @Value("${app.external-base-url:https://library.muczynskifamily.com}")
    private String externalBaseUrl;

    public LoanChangeEmailService(GlobalSettingsService globalSettingsService,
                                  ApplicationEmailService applicationEmailService,
                                  ObjectMapper objectMapper) {
        this.globalSettingsService = globalSettingsService;
        this.applicationEmailService = applicationEmailService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String kind() {
        return EmailChangeKinds.LOAN;
    }

    @Override
    public void send(String beforeJson, String afterJson) {
        LoanMailSnapshot before = read(beforeJson);
        LoanMailSnapshot after = read(afterJson);
        if (before == null && after == null) {
            return;
        }
        GlobalSettings settings = globalSettingsService.getGlobalSettings();
        EmailMethod method = applicationEmailService.effectiveMethod(settings);
        if (method == EmailMethod.DISABLED) {
            logger.debug("Loan email skipped: email method is DISABLED");
            return;
        }
        EmailSender sender = applicationEmailService.senderFor(method);
        if (sender == null) {
            throw new EmailSendException("No email sender registered for method " + method);
        }
        if (!sender.isConfigured(settings)) {
            logger.warn("Loan email skipped: {}", sender.describeStatus(settings));
            return;
        }

        List<String> recipients = new ArrayList<>();
        if (settings.isEmailNotifyLibrariansOnLoanChange()) {
            recipients.addAll(applicationEmailService.resolveLibrarianRecipients(settings));
        }
        if (settings.isEmailNotifyBorrowerOnLoanChange()) {
            recipients = EmailAddresses.mergeUnique(recipients, borrowerEmails(before, after));
        }
        if (recipients.isEmpty()) {
            logger.info("Loan email skipped: no recipients");
            return;
        }

        boolean created = before == null;
        boolean removed = after == null;
        LoanMailSnapshot shown = after != null ? after : before;
        List<String> lines = ChangeDescription.lines(created, removed,
                new ChangeDescription.Field("Book", title(before), title(after)),
                new ChangeDescription.Field("Borrower", name(before), name(after)),
                new ChangeDescription.Field("Borrower email", email(before), email(after)),
                new ChangeDescription.Field("Loan date", date(before == null ? null : before.loanDate()),
                        date(after == null ? null : after.loanDate())),
                new ChangeDescription.Field("Due date", date(before == null ? null : before.dueDate()),
                        date(after == null ? null : after.dueDate())),
                new ChangeDescription.Field("Returned", date(before == null ? null : before.returnDate()),
                        date(after == null ? null : after.returnDate())));
        if (!created && !removed && lines.isEmpty()) {
            return;
        }

        String title = HtmlText.blankToEmDash(shown.bookTitle());
        String intro;
        String subject;
        if (created) {
            intro = "A library loan was created.";
            subject = "Library loan: " + title;
        } else if (removed) {
            intro = "A library loan was removed.";
            subject = "Library loan removed: " + title;
        } else {
            intro = "A library loan was changed.";
            subject = "Library loan changed: " + title;
        }
        String link = loanUrl(shown.id());

        EmailMessage message = new EmailMessage();
        message.getTo().addAll(recipients);
        message.setFromAddress(settings.getEmailFromAddress() != null ? settings.getEmailFromAddress().trim() : null);
        message.setFromName(settings.getEmailFromName() != null ? settings.getEmailFromName().trim() : null);
        message.setEvent(EVENT);
        message.setSubject(subject);
        message.setTextBody(ChangeDescription.text(intro, lines, link));
        message.setHtmlBody(ChangeDescription.html(intro, lines, link));
        message.getEventPayload().put("loanId", shown.id());
        message.getEventPayload().put("bookTitle", shown.bookTitle());
        sender.send(message, settings);
    }

    private LoanMailSnapshot read(String json) {
        if (json == null || json.isBlank() || "null".equals(json.trim())) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LoanMailSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new EmailSendException("Could not read queued loan JSON", e);
        }
    }

    private static List<String> borrowerEmails(LoanMailSnapshot before, LoanMailSnapshot after) {
        List<String> emails = new ArrayList<>();
        if (before != null && EmailAddresses.isValid(before.borrowerEmail())) {
            emails.add(before.borrowerEmail().trim());
        }
        if (after != null && EmailAddresses.isValid(after.borrowerEmail())) {
            emails.add(after.borrowerEmail().trim());
        }
        return emails;
    }

    private static String title(LoanMailSnapshot snapshot) {
        return snapshot == null ? null : snapshot.bookTitle();
    }

    private static String name(LoanMailSnapshot snapshot) {
        return snapshot == null ? null : snapshot.borrowerName();
    }

    private static String email(LoanMailSnapshot snapshot) {
        return snapshot == null ? null : snapshot.borrowerEmail();
    }

    private static String date(java.time.LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String loanUrl(Long id) {
        if (id == null) {
            return null;
        }
        String base = externalBaseUrl != null ? externalBaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/loans/" + id;
    }
}
