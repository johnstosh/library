/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * SMTP transport using Jakarta Mail, configured at send time from global
 * settings (not application.properties) so librarians can change host/port
 * without a redeploy.
 *
 * <p>Cloud Run typically blocks port 25; use 587 (STARTTLS) or 465 (SSL).
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final MailTransport mailTransport;

    public SmtpEmailSender(MailTransport mailTransport) {
        this.mailTransport = mailTransport;
    }

    @Override
    public EmailMethod getMethod() {
        return EmailMethod.SMTP;
    }

    @Override
    public boolean isConfigured(GlobalSettings settings) {
        return hasText(settings.getSmtpHost()) && hasText(settings.getEmailFromAddress());
    }

    @Override
    public String describeStatus(GlobalSettings settings) {
        if (!hasText(settings.getSmtpHost())) {
            return "SMTP host is required";
        }
        if (!hasText(settings.getEmailFromAddress())) {
            return "From address is required";
        }
        int port = effectivePort(settings);
        return "Ready (smtp://" + settings.getSmtpHost().trim() + ":" + port + ")";
    }

    @Override
    public void send(EmailMessage message, GlobalSettings settings) {
        if (!isConfigured(settings)) {
            throw new EmailSendException(describeStatus(settings));
        }
        try {
            Session session = createSession(settings);
            MimeMessage mime = new MimeMessage(session);
            InternetAddress from = new InternetAddress(
                    settings.getEmailFromAddress().trim(),
                    hasText(settings.getEmailFromName()) ? settings.getEmailFromName().trim() : null,
                    StandardCharsets.UTF_8.name());
            mime.setFrom(from);
            for (String to : message.getTo()) {
                mime.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            }
            mime.setSubject(message.getSubject() != null ? message.getSubject() : "", StandardCharsets.UTF_8.name());

            MimeMultipart multipart = new MimeMultipart("alternative");
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(message.getTextBody() != null ? message.getTextBody() : "", StandardCharsets.UTF_8.name());
            multipart.addBodyPart(textPart);
            if (hasText(message.getHtmlBody())) {
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(message.getHtmlBody(), "text/html; charset=UTF-8");
                multipart.addBodyPart(htmlPart);
            }
            mime.setContent(multipart);

            logger.info("Sending SMTP email to {} via {}:{}", message.getTo(),
                    settings.getSmtpHost(), effectivePort(settings));
            mailTransport.send(mime);
        } catch (EmailSendException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailSendException("SMTP send failed: " + e.getMessage(), e);
        }
    }

    Session createSession(GlobalSettings settings) {
        Properties props = new Properties();
        String host = settings.getSmtpHost().trim();
        int port = effectivePort(settings);
        boolean startTls = settings.isSmtpStartTls();
        boolean ssl = settings.isSmtpSsl();
        boolean auth = hasText(settings.getSmtpUsername());

        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        props.put("mail.smtp.starttls.required", String.valueOf(startTls));
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");

        if (!auth) {
            return Session.getInstance(props);
        }
        String username = settings.getSmtpUsername().trim();
        String password = settings.getSmtpPassword() != null ? settings.getSmtpPassword() : "";
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private static int effectivePort(GlobalSettings settings) {
        if (settings.getSmtpPort() != null && settings.getSmtpPort() > 0) {
            return settings.getSmtpPort();
        }
        return settings.isSmtpSsl() ? 465 : 587;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
