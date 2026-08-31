/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import com.muczynski.library.dto.TestEmailResultDto;
import com.muczynski.library.email.EmailAddresses;
import com.muczynski.library.email.EmailMessage;
import com.muczynski.library.email.EmailSendException;
import com.muczynski.library.email.EmailSender;
import com.muczynski.library.email.HtmlText;
import com.muczynski.library.email.PendingApplicationNotice;
import com.muczynski.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatches emails for pending library-card applications using the transport
 * selected in global settings. Delivery failures are logged and never thrown
 * to the application-registration path.
 */
@Service
public class ApplicationEmailService {

    static final String EVENT_PENDING = "library.application.pending";
    static final String EVENT_APPLICANT_PENDING = "library.application.pending.applicant";
    static final String EVENT_TEST = "library.email.test";

    private static final String EMAIL_FONT_FAMILY =
            "Century Schoolbook L, Century Schoolbook, Times New Roman, Times, serif";

    private static final Logger logger = LoggerFactory.getLogger(ApplicationEmailService.class);

    private final GlobalSettingsService globalSettingsService;
    private final UserRepository userRepository;
    private final Map<EmailMethod, EmailSender> senders;

    @Value("${app.external-base-url:https://library.muczynskifamily.com}")
    private String externalBaseUrl;

    public ApplicationEmailService(GlobalSettingsService globalSettingsService,
                                   UserRepository userRepository,
                                   List<EmailSender> senderList) {
        this.globalSettingsService = globalSettingsService;
        this.userRepository = userRepository;
        this.senders = new EnumMap<>(EmailMethod.class);
        for (EmailSender sender : senderList) {
            this.senders.put(sender.getMethod(), sender);
        }
    }

    /**
     * Fire-and-forget notification after a card application is saved as PENDING.
     */
    @Async
    public void notifyPendingApplication(PendingApplicationNotice notice) {
        try {
            sendPendingNotifications(notice);
        } catch (Exception e) {
            logger.error("Failed to send pending-application email for '{}': {}",
                    notice.getApplicantName(), e.getMessage(), e);
        }
    }

    /**
     * Synchronous send used by tests and by {@link #notifyPendingApplication}.
     */
    public void sendPendingNotifications(PendingApplicationNotice notice) {
        GlobalSettings settings = globalSettingsService.getGlobalSettings();
        EmailMethod method = effectiveMethod(settings);
        if (method == EmailMethod.DISABLED) {
            logger.debug("Pending-application email skipped: email method is DISABLED");
            return;
        }

        EmailSender sender = requireSender(method);
        if (!sender.isConfigured(settings)) {
            logger.warn("Pending-application email skipped: {}", sender.describeStatus(settings));
            return;
        }

        if (settings.isEmailNotifyLibrariansOnPending()) {
            List<String> librarians = resolveLibrarianRecipients(settings);
            if (librarians.isEmpty()) {
                logger.warn("Pending-application librarian email skipped: no recipients configured");
            } else {
                sendQuietly(sender, composeLibrarianPending(notice, librarians, settings), settings);
            }
        }

        if (settings.isEmailNotifyApplicantOnPending()) {
            if (!EmailAddresses.isValid(notice.getApplicantEmail())) {
                logger.info("Pending-application applicant email skipped: no valid email on application for '{}'",
                        notice.getApplicantName());
            } else {
                sendQuietly(sender, composeApplicantPending(notice, settings), settings);
            }
        }
    }

    public TestEmailResultDto sendTestEmail(String toOverride) {
        GlobalSettings settings = globalSettingsService.getGlobalSettings();
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
            recipients = resolveLibrarianRecipients(settings);
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

    private void sendQuietly(EmailSender sender, EmailMessage message, GlobalSettings settings) {
        try {
            sender.send(message, settings);
        } catch (EmailSendException e) {
            logger.error("Email send failed via {}: {}", sender.getMethod(), e.getMessage());
        }
    }

    EmailMessage composeLibrarianPending(PendingApplicationNotice notice,
                                         List<String> recipients,
                                         GlobalSettings settings) {
        String name = HtmlText.blankToEmDash(notice.getApplicantName());
        String email = HtmlText.blankToEmDash(notice.getApplicantEmail());
        String reviewUrl = reviewApplicationsUrl();

        EmailMessage message = baseMessage(settings, recipients);
        message.setEvent(EVENT_PENDING);
        message.setSubject("Library card application pending: " + name);
        message.setTextBody(
                "A library card application is pending review.\n\n"
                        + "Applicant: " + name + "\n"
                        + "Email: " + email + "\n"
                        + "Application ID: " + notice.getApplicationId() + "\n"
                        + "Review: " + reviewUrl + "\n");
        message.setHtmlBody(htmlBody(
                "<p>A library card application is pending review.</p>"
                        + "<ul>"
                        + "<li><strong>Applicant:</strong> " + HtmlText.escape(name) + "</li>"
                        + "<li><strong>Email:</strong> " + HtmlText.escape(email) + "</li>"
                        + "<li><strong>Application ID:</strong> " + notice.getApplicationId() + "</li>"
                        + "</ul>"
                        + "<p><a href=\"" + HtmlText.escape(reviewUrl) + "\">Review applications</a></p>"));
        message.getEventPayload().put("applicationId", notice.getApplicationId());
        message.getEventPayload().put("applicantName", notice.getApplicantName());
        message.getEventPayload().put("applicantEmail", notice.getApplicantEmail());
        message.getEventPayload().put("status", "PENDING");
        message.getEventPayload().put("reviewUrl", reviewUrl);
        return message;
    }

    EmailMessage composeApplicantPending(PendingApplicationNotice notice, GlobalSettings settings) {
        String name = HtmlText.blankToEmDash(notice.getApplicantName());
        EmailMessage message = baseMessage(settings, List.of(notice.getApplicantEmail().trim()));
        message.setEvent(EVENT_APPLICANT_PENDING);
        message.setSubject("We received your library card application");
        message.setTextBody(
                "Hello " + name + ",\n\n"
                        + "We received your library card application and a librarian will review it shortly.\n"
                        + "You will be able to sign in after it is approved.\n");
        message.setHtmlBody(htmlBody(
                "<p>Hello " + HtmlText.escape(name) + ",</p>"
                        + "<p>We received your library card application and a librarian will review it shortly.</p>"
                        + "<p>You will be able to sign in after it is approved.</p>"));
        message.getEventPayload().put("applicationId", notice.getApplicationId());
        message.getEventPayload().put("applicantName", notice.getApplicantName());
        message.getEventPayload().put("status", "PENDING");
        return message;
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
