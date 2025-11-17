/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import com.muczynski.library.service.GlobalSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

/**
 * Custom ClientRegistrationRepository that reads Google SSO credentials
 * from GlobalSettings (database) instead of static configuration.
 * This allows librarians to configure SSO credentials via the admin UI.
 */
@Component
public class DynamicClientRegistrationRepository implements ClientRegistrationRepository {

    private static final Logger logger = LoggerFactory.getLogger(DynamicClientRegistrationRepository.class);

    private final GlobalSettingsService globalSettingsService;

    public DynamicClientRegistrationRepository(GlobalSettingsService globalSettingsService) {
        this.globalSettingsService = globalSettingsService;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (!"google".equals(registrationId)) {
            logger.debug("Unsupported registration ID: {}", registrationId);
            return null;
        }

        String clientId = globalSettingsService.getEffectiveSsoClientId();
        String clientSecret = globalSettingsService.getEffectiveSsoClientSecret();

        if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
            logger.warn("Google SSO credentials not configured in GlobalSettings");
            return null;
        }

        logger.debug("Building Google SSO ClientRegistration with Client ID: {}", clientId);

        return ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/google")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build();
    }
}
