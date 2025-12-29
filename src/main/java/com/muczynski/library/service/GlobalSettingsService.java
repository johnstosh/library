/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.GlobalSettings;
import com.muczynski.library.dto.GlobalSettingsDto;
import com.muczynski.library.mapper.GlobalSettingsMapper;
import com.muczynski.library.repository.GlobalSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for managing global application settings.
 * Only librarians should be able to modify these settings.
 */
@Service
@Transactional
public class GlobalSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(GlobalSettingsService.class);

    @Autowired
    private GlobalSettingsRepository globalSettingsRepository;

    @Autowired
    private GlobalSettingsMapper globalSettingsMapper;

    @Value("${google.oauth.client-id}")
    private String configuredClientId;

    @Value("${GOOGLE_CLIENT_ID:}")
    private String envClientId;

    @Value("${GOOGLE_CLIENT_SECRET:}")
    private String envClientSecret;

    @Value("${GOOGLE_SSO_CLIENT_ID:}")
    private String envSsoClientId;

    @Value("${GOOGLE_SSO_CLIENT_SECRET:}")
    private String envSsoClientSecret;

    /**
     * Get the global settings singleton
     */
    public GlobalSettings getGlobalSettings() {
        logger.debug("Fetching global settings");
        return globalSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    logger.info("No global settings found, creating default settings");
                    GlobalSettings settings = new GlobalSettings();
                    settings.setGoogleClientId(configuredClientId);
                    settings.setRedirectUri(""); // Will be set dynamically or by admin
                    return globalSettingsRepository.save(settings);
                });
    }

    /**
     * Get global settings as DTO (for API responses)
     * Includes partial secret display and validation info
     */
    public GlobalSettingsDto getGlobalSettingsDto() {
        logger.debug("Fetching global settings DTO");
        GlobalSettings settings = getGlobalSettings();

        // Use MapStruct mapper for basic field mapping
        GlobalSettingsDto dto = globalSettingsMapper.toDto(settings);

        // Override Client ID with fallback logic
        dto.setGoogleClientId(settings.getGoogleClientId() != null && !settings.getGoogleClientId().isEmpty()
                ? settings.getGoogleClientId()
                : configuredClientId);

        // Determine effective Client Secret (database or environment variable)
        String effectiveSecret = getEffectiveClientSecret();

        // Set partial secret for verification (last 4 characters, matching Google's pattern)
        if (effectiveSecret != null && !effectiveSecret.trim().isEmpty()) {
            dto.setGoogleClientSecretConfigured(true);
            if (effectiveSecret.length() >= 4) {
                String lastFour = effectiveSecret.substring(effectiveSecret.length() - 4);
                dto.setGoogleClientSecretPartial("..." + lastFour);
            } else {
                dto.setGoogleClientSecretPartial("(too short)");
            }

            // Validate format
            String validation = validateClientSecretFormat(effectiveSecret);
            dto.setGoogleClientSecretValidation(validation);
        } else {
            dto.setGoogleClientSecretConfigured(false);
            dto.setGoogleClientSecretPartial("(not configured)");
            dto.setGoogleClientSecretValidation("Client Secret not configured");
        }

        // Don't include the actual secret in responses
        dto.setGoogleClientSecret(null);

        // Handle Google SSO credentials
        String effectiveSsoClientId = getEffectiveSsoClientId();
        String effectiveSsoClientSecret = getEffectiveSsoClientSecret();

        dto.setGoogleSsoClientId(effectiveSsoClientId);
        dto.setGoogleSsoCredentialsUpdatedAt(settings.getGoogleSsoCredentialsUpdatedAt());

        // Set SSO Client ID configured status
        dto.setGoogleSsoClientIdConfigured(effectiveSsoClientId != null && !effectiveSsoClientId.isEmpty());

        // Set partial SSO secret for verification
        if (effectiveSsoClientSecret != null && !effectiveSsoClientSecret.trim().isEmpty()) {
            dto.setGoogleSsoClientSecretConfigured(true);
            if (effectiveSsoClientSecret.length() >= 4) {
                String lastFour = effectiveSsoClientSecret.substring(effectiveSsoClientSecret.length() - 4);
                dto.setGoogleSsoClientSecretPartial("..." + lastFour);
            } else {
                dto.setGoogleSsoClientSecretPartial("(too short)");
            }
        } else {
            dto.setGoogleSsoClientSecretConfigured(false);
            dto.setGoogleSsoClientSecretPartial("(not configured)");
        }

        // Don't include the actual SSO secret in responses
        dto.setGoogleSsoClientSecret(null);

        return dto;
    }

    /**
     * Update global settings
     * Only updates non-null/non-empty values
     */
    public GlobalSettingsDto updateGlobalSettings(GlobalSettingsDto dto) {
        logger.info("Updating global settings");
        GlobalSettings settings = getGlobalSettings();

        boolean updated = false;

        // Update Client Secret if provided
        if (dto.getGoogleClientSecret() != null && !dto.getGoogleClientSecret().trim().isEmpty()) {
            String newSecret = dto.getGoogleClientSecret().trim();

            // Validate format
            String validation = validateClientSecretFormat(newSecret);
            if (!validation.equals("Valid")) {
                logger.warn("Client Secret validation warning: {}", validation);
            }

            logger.info("Updating Google Client Secret (length: {} chars)", newSecret.length());
            logger.debug("New Client Secret starts with: {}...",
                    newSecret.substring(0, Math.min(8, newSecret.length())));

            settings.setGoogleClientSecret(newSecret);
            settings.setGoogleClientSecretUpdatedAt(Instant.now());
            updated = true;
        }

        // Update Client ID if provided (for reference)
        if (dto.getGoogleClientId() != null && !dto.getGoogleClientId().trim().isEmpty()) {
            logger.info("Updating Google Client ID reference");
            settings.setGoogleClientId(dto.getGoogleClientId().trim());
            updated = true;
        }

        // Update redirect URI if provided (for reference/display)
        if (dto.getRedirectUri() != null && !dto.getRedirectUri().trim().isEmpty()) {
            logger.info("Updating redirect URI reference: {}", dto.getRedirectUri());
            settings.setRedirectUri(dto.getRedirectUri().trim());
            updated = true;
        }

        // Update SSO Client ID if provided
        if (dto.getGoogleSsoClientId() != null && !dto.getGoogleSsoClientId().trim().isEmpty()) {
            logger.info("Updating Google SSO Client ID");
            settings.setGoogleSsoClientId(dto.getGoogleSsoClientId().trim());
            settings.setGoogleSsoCredentialsUpdatedAt(Instant.now());
            updated = true;
        }

        // Update SSO Client Secret if provided
        if (dto.getGoogleSsoClientSecret() != null && !dto.getGoogleSsoClientSecret().trim().isEmpty()) {
            String newSecret = dto.getGoogleSsoClientSecret().trim();
            logger.info("Updating Google SSO Client Secret (length: {} chars)", newSecret.length());
            settings.setGoogleSsoClientSecret(newSecret);
            settings.setGoogleSsoCredentialsUpdatedAt(Instant.now());
            updated = true;
        }

        if (updated) {
            settings = globalSettingsRepository.save(settings);
            logger.info("Global settings updated successfully");
        } else {
            logger.info("No changes to global settings");
        }

        return getGlobalSettingsDto();
    }

    /**
     * Get the effective Client ID (from database, environment variable, or config file)
     */
    public String getEffectiveClientId() {
        GlobalSettings settings = getGlobalSettings();
        String dbClientId = settings.getGoogleClientId();

        // Prefer database value over environment variable over config file
        if (dbClientId != null && !dbClientId.trim().isEmpty()) {
            logger.debug("Using Client ID from database");
            return dbClientId.trim();
        } else if (envClientId != null && !envClientId.trim().isEmpty()) {
            logger.debug("Using Client ID from environment variable");
            return envClientId.trim();
        } else if (configuredClientId != null && !configuredClientId.trim().isEmpty()) {
            logger.debug("Using Client ID from application.properties");
            return configuredClientId.trim();
        } else {
            logger.warn("No Client ID configured in database, environment variable, or config file");
            return null;
        }
    }

    public String getEffectiveClientSecret() {
        GlobalSettings settings = getGlobalSettings();
        String dbSecret = settings.getGoogleClientSecret();

        // Prefer database value over environment variable
        if (dbSecret != null && !dbSecret.trim().isEmpty()) {
            logger.debug("Using Client Secret from database (length: {} chars)", dbSecret.length());
            return dbSecret.trim();
        } else if (envClientSecret != null && !envClientSecret.trim().isEmpty()) {
            logger.debug("Using Client Secret from environment variable (length: {} chars)", envClientSecret.length());
            return envClientSecret.trim();
        } else {
            logger.warn("No Client Secret configured in database or environment variable");
            return null;
        }
    }

    /**
     * Validate Client Secret format
     * Returns "Valid" if OK, otherwise returns a warning message
     */
    private String validateClientSecretFormat(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            return "Client Secret is empty";
        }

        String trimmed = secret.trim();

        if (trimmed.length() < 20) {
            return "Warning: Client Secret is suspiciously short (" + trimmed.length() + " chars). Expected 30+ characters.";
        }

        if (!trimmed.startsWith("GOCSPX-")) {
            return "Warning: Client Secret does not start with 'GOCSPX-'. This may not be a valid Google OAuth client secret.";
        }

        return "Valid";
    }

    /**
     * Check if Client Secret is configured
     */
    public boolean isClientSecretConfigured() {
        String secret = getEffectiveClientSecret();
        return secret != null && !secret.isEmpty();
    }

    /**
     * Get the effective SSO Client ID (from database or environment variable)
     */
    public String getEffectiveSsoClientId() {
        GlobalSettings settings = getGlobalSettings();
        String dbClientId = settings.getGoogleSsoClientId();

        // Prefer database value over environment variable
        if (dbClientId != null && !dbClientId.trim().isEmpty()) {
            logger.debug("Using SSO Client ID from database");
            return dbClientId.trim();
        } else if (envSsoClientId != null && !envSsoClientId.trim().isEmpty()) {
            logger.debug("Using SSO Client ID from environment variable");
            return envSsoClientId.trim();
        } else {
            logger.debug("No SSO Client ID configured");
            return "";
        }
    }

    /**
     * Get the effective SSO Client Secret (from database or environment variable)
     */
    public String getEffectiveSsoClientSecret() {
        GlobalSettings settings = getGlobalSettings();
        String dbSecret = settings.getGoogleSsoClientSecret();

        // Prefer database value over environment variable
        if (dbSecret != null && !dbSecret.trim().isEmpty()) {
            logger.debug("Using SSO Client Secret from database (length: {} chars)", dbSecret.length());
            return dbSecret.trim();
        } else if (envSsoClientSecret != null && !envSsoClientSecret.trim().isEmpty()) {
            logger.debug("Using SSO Client Secret from environment variable (length: {} chars)", envSsoClientSecret.length());
            return envSsoClientSecret.trim();
        } else {
            logger.debug("No SSO Client Secret configured");
            return "";
        }
    }

    /**
     * Check if SSO credentials are configured
     */
    public boolean isSsoCredentialsConfigured() {
        String clientId = getEffectiveSsoClientId();
        String clientSecret = getEffectiveSsoClientSecret();
        return clientId != null && !clientId.isEmpty() && clientSecret != null && !clientSecret.isEmpty();
    }
}
