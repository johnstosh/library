/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * DTO for global application settings.
 * Includes partial secret display for verification without exposing the full secret.
 */
@Getter
@Setter
public class GlobalSettingsDto {

    /**
     * Google OAuth Client Secret (write-only when updating)
     * Set to empty string or null to keep existing value
     */
    private String googleClientSecret;

    /**
     * Partial Client Secret for verification (read-only)
     * Shows first 8 characters (e.g., "GOCSPX-ab...")
     */
    private String googleClientSecretPartial;

    /**
     * When the Client Secret was last updated
     */
    private Instant googleClientSecretUpdatedAt;

    /**
     * Google OAuth Client ID (read-only, from application.properties)
     */
    private String googleClientId;

    /**
     * Configured redirect URI (read-only)
     */
    private String redirectUri;

    /**
     * Google SSO OAuth Client ID (for user authentication)
     */
    private String googleSsoClientId;

    /**
     * Google SSO OAuth Client Secret (write-only when updating)
     * Set to empty string or null to keep existing value
     */
    private String googleSsoClientSecret;

    /**
     * Partial SSO Client Secret for verification (read-only)
     * Shows first 8 characters
     */
    private String googleSsoClientSecretPartial;

    /**
     * When the SSO credentials were last updated
     */
    private Instant googleSsoCredentialsUpdatedAt;

    /**
     * Whether the SSO Client Secret is configured (has a value)
     */
    private boolean googleSsoClientSecretConfigured;

    /**
     * Whether the SSO Client ID is configured (has a value)
     */
    private boolean googleSsoClientIdConfigured;

    /**
     * Last updated timestamp for any settings change
     */
    private Instant lastUpdated;

    /**
     * Whether the Client Secret is configured (has a value)
     */
    private boolean googleClientSecretConfigured;

    /**
     * Validation message for the Client Secret format
     */
    private String googleClientSecretValidation;

    /**
     * Validation message for the SSO Client Secret format
     */
    private String googleSsoClientSecretValidation;
}
