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

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
}
