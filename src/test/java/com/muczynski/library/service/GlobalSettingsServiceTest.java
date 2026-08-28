/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import com.muczynski.library.dto.GlobalSettingsDto;
import com.muczynski.library.email.EmailSender;
import com.muczynski.library.mapper.GlobalSettingsMapper;
import com.muczynski.library.repository.GlobalSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalSettingsService
 * Tests business logic with mocked repository
 */
@ExtendWith(MockitoExtension.class)
class GlobalSettingsServiceTest {

    @Mock
    private GlobalSettingsRepository globalSettingsRepository;

    @Mock
    private GlobalSettingsMapper globalSettingsMapper;

    @InjectMocks
    private GlobalSettingsService globalSettingsService;

    @Test
    void testGetGlobalSettings_ExistingSettings() {
        // Arrange
        GlobalSettings existingSettings = new GlobalSettings();
        existingSettings.setId(1L);
        existingSettings.setGoogleClientId("test-client-id.apps.googleusercontent.com");
        existingSettings.setGoogleClientSecret("GOCSPX-testSecret123456");

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existingSettings));

        // Act
        GlobalSettings result = globalSettingsService.getGlobalSettings();

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test-client-id.apps.googleusercontent.com", result.getGoogleClientId());
        verify(globalSettingsRepository, times(1)).findFirstByOrderByIdAsc();
        verify(globalSettingsRepository, never()).save(any(GlobalSettings.class)); // Should not save when exists
    }

    @Test
    void testGetGlobalSettings_CreatesDefaultWhenNoneExist() {
        // Arrange
        ReflectionTestUtils.setField(globalSettingsService, "configuredClientId", "default-client-id.apps.googleusercontent.com");

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        GlobalSettings newSettings = new GlobalSettings();
        newSettings.setId(1L);
        newSettings.setGoogleClientId("default-client-id.apps.googleusercontent.com");
        newSettings.setRedirectUri("");

        when(globalSettingsRepository.save(any(GlobalSettings.class))).thenReturn(newSettings);

        // Act
        GlobalSettings result = globalSettingsService.getGlobalSettings();

        // Assert
        assertNotNull(result);
        verify(globalSettingsRepository, times(1)).save(any(GlobalSettings.class));
    }

    @Test
    void testGetGlobalSettingsDto_WithSecretMasking() {
        // Arrange
        GlobalSettings settings = new GlobalSettings();
        settings.setId(1L);
        settings.setGoogleClientId("test-client-id.apps.googleusercontent.com");
        settings.setGoogleClientSecret("GOCSPX-testSecretValue1234");
        settings.setRedirectUri("https://library.example.com/oauth/callback");
        settings.setGoogleClientSecretUpdatedAt(Instant.now());
        settings.setLastUpdated(Instant.now());

        GlobalSettingsDto mappedDto = new GlobalSettingsDto();
        mappedDto.setGoogleClientId("test-client-id.apps.googleusercontent.com");
        mappedDto.setRedirectUri("https://library.example.com/oauth/callback");
        mappedDto.setGoogleClientSecretUpdatedAt(settings.getGoogleClientSecretUpdatedAt());
        mappedDto.setLastUpdated(settings.getLastUpdated());

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        when(globalSettingsMapper.toDto(settings)).thenReturn(mappedDto);

        // Act
        GlobalSettingsDto result = globalSettingsService.getGlobalSettingsDto();

        // Assert
        assertNotNull(result);
        assertEquals("test-client-id.apps.googleusercontent.com", result.getGoogleClientId());
        assertEquals("...1234", result.getGoogleClientSecretPartial()); // Last 4 chars
        assertTrue(result.isGoogleClientSecretConfigured());
        assertEquals("Valid", result.getGoogleClientSecretValidation()); // GOCSPX- prefix is valid
        assertNull(result.getGoogleClientSecret()); // Full secret should never be exposed
    }

    @Test
    void testGetGlobalSettingsDto_WithoutSecret() {
        // Arrange
        GlobalSettings settings = new GlobalSettings();
        settings.setId(1L);
        settings.setGoogleClientId("test-client-id.apps.googleusercontent.com");
        settings.setGoogleClientSecret(""); // No secret set

        GlobalSettingsDto mappedDto = new GlobalSettingsDto();
        mappedDto.setGoogleClientId("test-client-id.apps.googleusercontent.com");

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        when(globalSettingsMapper.toDto(settings)).thenReturn(mappedDto);

        // Act
        GlobalSettingsDto result = globalSettingsService.getGlobalSettingsDto();

        // Assert
        assertNotNull(result);
        assertFalse(result.isGoogleClientSecretConfigured());
        assertEquals("(not configured)", result.getGoogleClientSecretPartial());
        assertEquals("Client Secret not configured", result.getGoogleClientSecretValidation());
    }

    @Test
    void testGetGlobalSettingsDto_InvalidSecretFormat() {
        // Arrange
        GlobalSettings settings = new GlobalSettings();
        settings.setId(1L);
        settings.setGoogleClientId("test-client-id.apps.googleusercontent.com");
        settings.setGoogleClientSecret("invalid-secret-without-prefix"); // Missing GOCSPX- prefix

        GlobalSettingsDto mappedDto = new GlobalSettingsDto();
        mappedDto.setGoogleClientId("test-client-id.apps.googleusercontent.com");

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        when(globalSettingsMapper.toDto(settings)).thenReturn(mappedDto);

        // Act
        GlobalSettingsDto result = globalSettingsService.getGlobalSettingsDto();

        // Assert
        assertNotNull(result);
        assertTrue(result.isGoogleClientSecretConfigured());
        assertTrue(result.getGoogleClientSecretValidation().contains("Warning"));
        assertTrue(result.getGoogleClientSecretValidation().contains("GOCSPX-"));
    }

    @Test
    void testUpdateGlobalSettings_UpdatesClientSecret() {
        // Arrange
        GlobalSettings existingSettings = new GlobalSettings();
        existingSettings.setId(1L);
        existingSettings.setGoogleClientId("existing-client-id.apps.googleusercontent.com");
        existingSettings.setGoogleClientSecret("GOCSPX-oldSecret");

        GlobalSettingsDto updateDto = new GlobalSettingsDto();
        updateDto.setGoogleClientSecret("GOCSPX-newSecret123456789");

        GlobalSettingsDto mappedDto = new GlobalSettingsDto();
        mappedDto.setGoogleClientId("existing-client-id.apps.googleusercontent.com");

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existingSettings));
        when(globalSettingsMapper.toDto(any(GlobalSettings.class))).thenReturn(mappedDto);
        when(globalSettingsRepository.save(any(GlobalSettings.class))).thenReturn(existingSettings);

        // Act
        GlobalSettingsDto result = globalSettingsService.updateGlobalSettings(updateDto);

        // Assert
        assertNotNull(result);
        verify(globalSettingsRepository, atLeastOnce()).save(any(GlobalSettings.class)); // Save called when updating
        assertNotNull(existingSettings.getGoogleClientSecretUpdatedAt());
    }

    @Test
    void testUpdateGlobalSettings_EmptySecretDoesNotUpdate() {
        // Arrange
        GlobalSettings existingSettings = new GlobalSettings();
        existingSettings.setId(1L);
        existingSettings.setGoogleClientSecret("GOCSPX-existingSecret");
        String originalSecret = existingSettings.getGoogleClientSecret();

        GlobalSettingsDto updateDto = new GlobalSettingsDto();
        updateDto.setGoogleClientSecret(""); // Empty secret should not update

        GlobalSettingsDto mappedDto = new GlobalSettingsDto();

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existingSettings));
        when(globalSettingsMapper.toDto(any(GlobalSettings.class))).thenReturn(mappedDto);

        // Act
        GlobalSettingsDto result = globalSettingsService.updateGlobalSettings(updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(originalSecret, existingSettings.getGoogleClientSecret()); // Should remain unchanged
    }

    @Test
    void testUpdateGlobalSettings_UpdatesSsoCredentials() {
        // Arrange
        GlobalSettings existingSettings = new GlobalSettings();
        existingSettings.setId(1L);
        existingSettings.setGoogleSsoClientId("old-sso-client-id.apps.googleusercontent.com");
        existingSettings.setGoogleSsoClientSecret("GOCSPX-oldSsoSecret");

        GlobalSettingsDto updateDto = new GlobalSettingsDto();
        updateDto.setGoogleSsoClientId("new-sso-client-id.apps.googleusercontent.com");
        updateDto.setGoogleSsoClientSecret("GOCSPX-newSsoSecret123");

        GlobalSettingsDto mappedDto = new GlobalSettingsDto();

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existingSettings));
        when(globalSettingsMapper.toDto(any(GlobalSettings.class))).thenReturn(mappedDto);
        when(globalSettingsRepository.save(any(GlobalSettings.class))).thenReturn(existingSettings);

        // Act
        GlobalSettingsDto result = globalSettingsService.updateGlobalSettings(updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("new-sso-client-id.apps.googleusercontent.com", existingSettings.getGoogleSsoClientId());
        assertEquals("GOCSPX-newSsoSecret123", existingSettings.getGoogleSsoClientSecret());
        assertNotNull(existingSettings.getGoogleSsoCredentialsUpdatedAt());
    }

    @Test
    void testGetEffectiveClientId_PrefersDatabase() {
        // Arrange
        GlobalSettings settings = new GlobalSettings();
        settings.setGoogleClientId("db-client-id.apps.googleusercontent.com");

        ReflectionTestUtils.setField(globalSettingsService, "envClientId", "env-client-id.apps.googleusercontent.com");
        ReflectionTestUtils.setField(globalSettingsService, "configuredClientId", "config-client-id.apps.googleusercontent.com");

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

        // Act
        String result = globalSettingsService.getEffectiveClientId();

        // Assert
        assertEquals("db-client-id.apps.googleusercontent.com", result); // Should prefer database value
    }

    @Test
    void testGetEffectiveClientSecret_FallsBackToEnvironment() {
        // Arrange
        GlobalSettings settings = new GlobalSettings();
        settings.setGoogleClientSecret(""); // Empty in database

        ReflectionTestUtils.setField(globalSettingsService, "envClientSecret", "GOCSPX-envSecret123");

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

        // Act
        String result = globalSettingsService.getEffectiveClientSecret();

        // Assert
        assertEquals("GOCSPX-envSecret123", result); // Should fall back to environment variable
    }

    @Test
    void testIsSsoCredentialsConfigured_BothPresent() {
        // Arrange
        GlobalSettings settings = new GlobalSettings();
        settings.setGoogleSsoClientId("sso-client-id.apps.googleusercontent.com");
        settings.setGoogleSsoClientSecret("GOCSPX-ssoSecret");

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

        // Act
        boolean result = globalSettingsService.isSsoCredentialsConfigured();

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsSsoCredentialsConfigured_MissingClientId() {
        // Arrange
        GlobalSettings settings = new GlobalSettings();
        settings.setGoogleSsoClientId(""); // Missing
        settings.setGoogleSsoClientSecret("GOCSPX-ssoSecret");

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

        // Act
        boolean result = globalSettingsService.isSsoCredentialsConfigured();

        // Assert
        assertFalse(result); // Should be false if either is missing
    }

    @Test
    void testIsSsoCredentialsConfigured_MissingClientSecret() {
        // Arrange
        GlobalSettings settings = new GlobalSettings();
        settings.setGoogleSsoClientId("sso-client-id.apps.googleusercontent.com");
        settings.setGoogleSsoClientSecret(""); // Missing

        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

        // Act
        boolean result = globalSettingsService.isSsoCredentialsConfigured();

        // Assert
        assertFalse(result); // Should be false if either is missing
    }

    @Test
    void testUpdateGlobalSettings_UpdatesEmailMethod() {
        GlobalSettings existingSettings = new GlobalSettings();
        existingSettings.setId(1L);
        existingSettings.setEmailMethod(EmailMethod.DISABLED);

        GlobalSettingsDto updateDto = new GlobalSettingsDto();
        updateDto.setEmailMethod(EmailMethod.LOG);
        updateDto.setEmailFromAddress("library@example.com");
        updateDto.setEmailNotifyLibrariansOnPending(true);
        updateDto.setEmailLibrarianRecipients("staff@example.com");

        GlobalSettingsDto mappedDto = new GlobalSettingsDto();
        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existingSettings));
        when(globalSettingsMapper.toDto(any(GlobalSettings.class))).thenReturn(mappedDto);
        when(globalSettingsRepository.save(any(GlobalSettings.class))).thenReturn(existingSettings);

        GlobalSettingsDto result = globalSettingsService.updateGlobalSettings(updateDto);

        assertNotNull(result);
        assertEquals(EmailMethod.LOG, existingSettings.getEmailMethod());
        assertEquals("library@example.com", existingSettings.getEmailFromAddress());
        assertEquals("staff@example.com", existingSettings.getEmailLibrarianRecipients());
        verify(globalSettingsRepository, atLeastOnce()).save(any(GlobalSettings.class));
    }

    @Test
    void testGetGlobalSettingsDto_DoesNotExposeEmailSecrets() {
        GlobalSettings settings = new GlobalSettings();
        settings.setId(1L);
        settings.setEmailMethod(EmailMethod.SMTP);
        settings.setSmtpPassword("super-secret-pass");
        settings.setSendGridApiKey("SG.secretkeyXXXX");
        settings.setWebhookBearerToken("webhook-token-9999");
        settings.setSmtpHost("smtp.example.com");
        settings.setEmailFromAddress("library@example.com");

        GlobalSettingsDto mappedDto = new GlobalSettingsDto();
        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        when(globalSettingsMapper.toDto(settings)).thenReturn(mappedDto);

        EmailSender smtpSender = mock(EmailSender.class);
        when(smtpSender.getMethod()).thenReturn(EmailMethod.SMTP);
        when(smtpSender.isConfigured(settings)).thenReturn(true);
        when(smtpSender.describeStatus(settings)).thenReturn("Ready (smtp://smtp.example.com:587)");
        globalSettingsService.setEmailSenders(List.of(smtpSender));

        GlobalSettingsDto result = globalSettingsService.getGlobalSettingsDto();

        assertNull(result.getSmtpPassword());
        assertNull(result.getSendGridApiKey());
        assertNull(result.getWebhookBearerToken());
        assertTrue(result.isSmtpPasswordConfigured());
        assertEquals("...pass", result.getSmtpPasswordPartial());
        assertEquals("...XXXX", result.getSendGridApiKeyPartial());
        assertEquals("...9999", result.getWebhookBearerTokenPartial());
        assertEquals(EmailMethod.SMTP, result.getEmailMethod());
        assertTrue(result.isEmailMethodConfigured());
    }

    @Test
    void testUpdateGlobalSettings_EmptySmtpPasswordDoesNotUpdate() {
        GlobalSettings existingSettings = new GlobalSettings();
        existingSettings.setId(1L);
        existingSettings.setSmtpPassword("keep-me");

        GlobalSettingsDto updateDto = new GlobalSettingsDto();
        updateDto.setSmtpPassword("");

        GlobalSettingsDto mappedDto = new GlobalSettingsDto();
        when(globalSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existingSettings));
        when(globalSettingsMapper.toDto(any(GlobalSettings.class))).thenReturn(mappedDto);

        globalSettingsService.updateGlobalSettings(updateDto);

        assertEquals("keep-me", existingSettings.getSmtpPassword());
    }
}
