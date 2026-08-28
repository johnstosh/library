/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Global application settings that apply to all users.
 * Only librarians can modify these settings.
 */
@Entity
@Table(name = "global_settings")
@Getter
@Setter
public class GlobalSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Google OAuth Client Secret (application-wide)
     * This is the same for all users
     */
    @Column(length = 500)
    private String googleClientSecret = "";

    /**
     * Timestamp when the Google Client Secret was last updated
     */
    private Instant googleClientSecretUpdatedAt;

    /**
     * Google OAuth Client ID (for reference/display)
     * This should match what's in application.properties
     */
    @Column(length = 500)
    private String googleClientId = "";

    /**
     * Application-wide redirect URI (for reference/display)
     */
    @Column(length = 500)
    private String redirectUri = "";

    /**
     * Google SSO OAuth Client ID (for user authentication)
     * Separate from Google Photos OAuth credentials
     */
    @Column(length = 500)
    private String googleSsoClientId = "";

    /**
     * Google SSO OAuth Client Secret (for user authentication)
     * Separate from Google Photos OAuth credentials
     */
    @Column(length = 500)
    private String googleSsoClientSecret = "";

    /**
     * Timestamp when the Google SSO credentials were last updated
     */
    private Instant googleSsoCredentialsUpdatedAt;

    /**
     * Last updated timestamp for any settings change
     */
    private Instant lastUpdated;

    /**
     * How outbound email is sent. Default DISABLED so a fresh install never
     * tries to deliver mail until a librarian picks a transport.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private EmailMethod emailMethod = EmailMethod.DISABLED;

    @Column(length = 320)
    private String emailFromAddress = "";

    @Column(length = 200)
    private String emailFromName = "";

    /**
     * Email librarians when a card application is saved as PENDING.
     */
    @Column(columnDefinition = "boolean default true")
    private boolean emailNotifyLibrariansOnPending = true;

    /**
     * Email the applicant a confirmation when they submit (requires email on the application).
     */
    @Column(columnDefinition = "boolean default false")
    private boolean emailNotifyApplicantOnPending = false;

    /**
     * Extra librarian notification addresses, comma/semicolon/whitespace separated.
     */
    @Column(length = 2000)
    private String emailLibrarianRecipients = "";

    /**
     * Also notify librarian users who have an email stored on their account.
     */
    @Column(columnDefinition = "boolean default true")
    private boolean emailIncludeLibrarianUserEmails = true;

    @Column(length = 255)
    private String smtpHost = "";

    private Integer smtpPort = 587;

    @Column(length = 320)
    private String smtpUsername = "";

    @Column(length = 500)
    private String smtpPassword = "";

    @Column(columnDefinition = "boolean default true")
    private boolean smtpStartTls = true;

    @Column(columnDefinition = "boolean default false")
    private boolean smtpSsl = false;

    @Column(length = 500)
    private String sendGridApiKey = "";

    @Column(length = 2000)
    private String webhookUrl = "";

    @Column(length = 500)
    private String webhookBearerToken = "";

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
}
