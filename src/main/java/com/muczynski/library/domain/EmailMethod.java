/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

/**
 * How the library sends outbound email. Configured in global settings so a
 * librarian can pick a transport that fits the deployment (Cloud Run, local
 * Docker, or a third-party automation) without a code change.
 */
public enum EmailMethod {
    /** Do not send email. Safe default until a transport is configured. */
    DISABLED,
    /** Write the message to application logs (Cloud Run / local logs). */
    LOG,
    /** SMTP (Gmail app password, Fastmail, Mailgun SMTP, etc.). */
    SMTP,
    /** SendGrid HTTP API (HTTPS, works well on Cloud Run). */
    SENDGRID,
    /** POST JSON to a URL (Zapier, n8n, Make, Apps Script, Cloud Function). */
    WEBHOOK
}
