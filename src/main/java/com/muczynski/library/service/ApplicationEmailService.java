/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import com.muczynski.library.dto.TestEmailResultDto;
import com.muczynski.library.email.ApplicationMailSnapshot;
import com.muczynski.library.email.ChangeDescription;
import com.muczynski.library.email.EmailAddresses;
import com.muczynski.library.email.EmailChangeHandler;
import com.muczynski.library.email.EmailChangeKinds;
import com.muczynski.library.email.EmailMessage;
import com.muczynski.library.email.EmailSendException;
import com.muczynski.library.email.EmailSender;
import com.muczynski.library.email.HtmlText;
import com.muczynski.library.email.PendingApplicationNotice;
import com.muczynski.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dispatches emails for pending library-card applications using the transport
 * selected in global settings. Delivery failures are logged and never thrown
 * to the application-registration path.
 */
@Service
public class ApplicationEmailService implements EmailChangeHandler {

    static final String EVENT_PENDING = "library.application.pending";
    static final String EVENT_TEST = "library.email.test";

    private static final String EMAIL_FONT_FAMILY =
            "Century Schoolbook L, Century Schoolbook, Times New Roman, Times, serif";

    private static final Logger logger = LoggerFactory.getLogger(ApplicationEmailService.class);

    private final GlobalSettingsService globalSettingsService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Map<EmailMethod, EmailSender> senders;

    @Value("${app.external-base-url:https://library.muczynskifamily.com}")
    private String externalBaseUrl;

    public ApplicationEmailService(GlobalSettingsService globalSettingsService,
                                   UserRepository userRepository,
                                   ObjectMapper objectMapper,
                                   List<EmailSender> senderList) {
        this.globalSettingsService = globalSettingsService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.senders = new EnumMap<>(EmailMethod.class);
        for (EmailSender sender : senderList) {
            this.senders.put(sender.getMethod(), sender);
        }
    }

    @Override
    public String kind() {
        return EmailChangeKinds.APPLICATION;
    }

    /**
     * Sends the queued application notice. Delivery failures propagate so the
     * queue can retry.
     */
    @Override
    public void send(String beforeJson, String afterJson) {
        try {
            sendApplicationChange(readSnapshot(beforeJson), readSnapshot(afterJson));
        } catch (JsonProcessingException e) {
            throw new EmailSendException("Could not read queued application JSON", e);
        }
    }

    /**
     * Immediate send used by tests. Production queues the application and sends
     * it from {@link #send(String, String)} after the quiet period.
     */
    public void sendPendingNotifications(PendingApplicationNotice notice) {
        try {
            sendApplicationChange(null, new ApplicationMailSnapshot(
                    notice.getApplicationId(),
                    notice.getApplicantName(),
                    notice.getApplicantEmail(),
                    null,
                    "PENDING"));
        } catch (RuntimeException e) {
            logger.error("Failed to send application email for '{}': {}",
                    notice.getApplicantName(), e.getMessage(), e);
        }
    }

    /**
     * One message, every recipient in To. Librarians and the applicant are not
     * split into separate emails.
     */
    public void sendApplicationChange(ApplicationMailSnapshot before, ApplicationMailSnapshot after) {
        if (before == null && after == null) {
            return;
        }
        GlobalSettings settings = globalSettingsService.settingsForEmail();
        EmailMethod method = effectiveMethod(settings);
        if (method == EmailMethod.DISABLED) {
            logger.debug("Application email skipped: email method is DISABLED");
            return;
        }
        EmailSender sender = requireSender(method);
        if (!sender.isConfigured(settings)) {
            logger.warn("Application email skipped: {}", sender.describeStatus(settings));
            return;
        }

        ApplicationMailSnapshot shown = after != null ? after : before;
        List<String> recipients = new ArrayList<>();
        if (settings.isEmailNotifyLibrariansOnPending()) {
            recipients.addAll(resolveLibrarianRecipients(settings));
        }
        if (settings.isEmailNotifyApplicantOnPending()) {
            String applicantEmail = firstValidEmail(
                    after != null ? after.email() : null,
                    before != null ? before.email() : null);
            if (applicantEmail != null) {
                recipients = EmailAddresses.mergeUnique(recipients, List.of(applicantEmail));
            } else {
                logger.info("Application applicant email skipped: no valid email for '{}'", shown.name());
            }
        }
        recipients = withoutOptedOutUsers(recipients);
        if (recipients.isEmpty()) {
            logger.info("Application email skipped: no recipients");
            return;
        }

        boolean created = before == null;
        boolean removed = after == null;
        String name = HtmlText.blankToEmDash(shown.name());
        String intro;
        String subject;
        String event;
        if (created) {
            intro = "A library card application was submitted.";
            if ("PENDING".equals(shown.status())) {
                intro = intro + " A librarian will review it shortly.";
            }
            subject = "Library card application: " + name;
            event = EVENT_PENDING;
        } else if (removed) {
            intro = "A library card application was removed.";
            subject = "Library card application removed: " + name;
            event = "library.application.removed";
        } else {
            intro = "A library card application was changed.";
            subject = "Library card application changed: " + name;
            event = "library.application.changed";
        }
        List<String> lines = ChangeDescription.lines(created, removed,
                new ChangeDescription.Field("Applicant", value(before, ApplicationMailSnapshot::name),
                        value(after, ApplicationMailSnapshot::name)),
                new ChangeDescription.Field("Email", value(before, ApplicationMailSnapshot::email),
                        value(after, ApplicationMailSnapshot::email)),
                new ChangeDescription.Field("Phone", value(before, ApplicationMailSnapshot::phone),
                        value(after, ApplicationMailSnapshot::phone)),
                new ChangeDescription.Field("Status", value(before, ApplicationMailSnapshot::status),
                        value(after, ApplicationMailSnapshot::status)));
        if (!created && !removed && lines.isEmpty()) {
            return;
        }
        String reviewUrl = reviewApplicationsUrl();
        EmailMessage message = baseMessage(settings, recipients);
        message.setEvent(event);
        message.setSubject(subject);
        message.setTextBody(ChangeDescription.text(intro, lines, reviewUrl));
        message.setHtmlBody(ChangeDescription.html(intro, lines, reviewUrl));
        message.getEventPayload().put("applicationId", shown.id());
        message.getEventPayload().put("applicantName", shown.name());
        message.getEventPayload().put("applicantEmail", shown.email());
        message.getEventPayload().put("status", shown.status());
        sender.send(message, settings);
    }

    public TestEmailResultDto sendTestEmail(String toOverride) {
        GlobalSettings settings = globalSettingsService.settingsForEmail();
        EmailMethod method = effectiveMethod(settings);
        TestEmailResultDto result = new TestEmailResultDto();
        result.setMethod(method);

        if (method == EmailMethod.DISABLED) {
            result.setSent(false);
            result.setMessage("Email method is DISABLED. Choose LOG, SMTP, SendGrid, or Webhook in Global Settings.");
            return result;
        }

        EmailSender sender = requireSender(method);
        if (!sender.isConfigured(settings)) {
            result.setSent(false);
            result.setMessage(sender.describeStatus(settings));
            return result;
        }

        List<String> recipients;
        if (EmailAddresses.isValid(toOverride)) {
            recipients = List.of(toOverride.trim());
        } else {
            recipients = withoutOptedOutUsers(resolveLibrarianRecipients(settings));
        }
        if (recipients.isEmpty()) {
            result.setSent(false);
            result.setMessage("No recipients. Enter a To address or configure librarian notification emails.");
            return result;
        }

        try {
            sender.send(composeTestMessage(recipients, settings), settings);
            result.setSent(true);
            result.setRecipients(recipients);
            result.setMessage("Test email sent via " + method + " to " + recipients);
            return result;
        } catch (EmailSendException e) {
            logger.warn("Test email failed: {}", e.getMessage());
            result.setSent(false);
            result.setRecipients(recipients);
            result.setMessage(e.getMessage());
            return result;
        }
    }

    public List<String> resolveLibrarianRecipients(GlobalSettings settings) {
        List<String> extra = EmailAddresses.parseRecipientList(settings.getEmailLibrarianRecipients());
        List<String> fromUsers = List.of();
        if (settings.isEmailIncludeLibrarianUserEmails()) {
            fromUsers = userRepository.findLibrarianEmails().stream()
                    .filter(EmailAddresses::isValid)
                    .map(String::trim)
                    .toList();
        }
        return EmailAddresses.mergeUnique(extra, fromUsers);
    }

    /**
     * Drops addresses that belong to a user who turned off email notifications.
     * Addresses that do not match a user are kept.
     */
    public List<String> withoutOptedOutUsers(List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }
        List<String> declined = userRepository.findEmailsDecliningNotifications();
        if (declined == null || declined.isEmpty()) {
            return recipients;
        }
        Set<String> optedOut = new HashSet<>();
        for (String address : declined) {
            if (address != null && !address.isBlank()) {
                optedOut.add(address.trim().toLowerCase(Locale.ROOT));
            }
        }
        if (optedOut.isEmpty()) {
            return recipients;
        }
        return recipients.stream()
                .filter(address -> address != null
                        && !optedOut.contains(address.trim().toLowerCase(Locale.ROOT)))
                .toList();
    }

    public EmailMethod effectiveMethod(GlobalSettings settings) {
        EmailMethod method = settings.getEmailMethod();
        return method != null ? method : EmailMethod.DISABLED;
    }

    public EmailSender senderFor(EmailMethod method) {
        return senders.get(method);
    }

    private EmailSender requireSender(EmailMethod method) {
        EmailSender sender = senders.get(method);
        if (sender == null) {
            throw new EmailSendException("No email sender registered for method " + method);
        }
        return sender;
    }

    EmailMessage composeTestMessage(List<String> recipients, GlobalSettings settings) {
        EmailMessage message = baseMessage(settings, recipients);
        message.setEvent(EVENT_TEST);
        message.setSubject("Library email test");
        message.setTextBody(
                "This is a test message from the library application.\n"
                        + "Email method: " + effectiveMethod(settings) + "\n"
                        + "If you received this, outbound email is working.\n");
        message.setHtmlBody(htmlBody(
                "<p>This is a test message from the library application.</p>"
                        + "<p>Email method: <strong>" + HtmlText.escape(effectiveMethod(settings).name())
                        + "</strong></p>"
                        + "<p>If you received this, outbound email is working.</p>"));
        return message;
    }

    private static String htmlBody(String innerHtml) {
        return "<div style=\"font-family:" + EMAIL_FONT_FAMILY + ";\">" + innerHtml + "</div>";
    }

    private ApplicationMailSnapshot readSnapshot(String json) throws JsonProcessingException {
        if (json == null || json.isBlank() || "null".equals(json.trim())) {
            return null;
        }
        return objectMapper.readValue(json, ApplicationMailSnapshot.class);
    }

    private static String firstValidEmail(String preferred, String fallback) {
        if (EmailAddresses.isValid(preferred)) {
            return preferred.trim();
        }
        if (EmailAddresses.isValid(fallback)) {
            return fallback.trim();
        }
        return null;
    }

    private static String value(ApplicationMailSnapshot snapshot,
                                java.util.function.Function<ApplicationMailSnapshot, String> getter) {
        return snapshot == null ? null : getter.apply(snapshot);
    }

    private EmailMessage baseMessage(GlobalSettings settings, List<String> recipients) {
        EmailMessage message = new EmailMessage();
        message.getTo().addAll(recipients);
        if (settings.getEmailFromAddress() != null) {
            message.setFromAddress(settings.getEmailFromAddress().trim());
        }
        if (settings.getEmailFromName() != null) {
            message.setFromName(settings.getEmailFromName().trim());
        }
        return message;
    }

    private String reviewApplicationsUrl() {
        String base = externalBaseUrl != null ? externalBaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/applications";
    }
}
