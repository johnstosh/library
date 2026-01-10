/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Custom OAuth2User that uses database user ID as the principal name
 * instead of the OAuth subject ID. This simplifies user lookups and
 * ensures consistency with CustomOidcUser for OIDC logins.
 */
public class CustomOAuth2User extends DefaultOAuth2User {
    private final Long userId;

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey,
                            Long userId) {
        super(authorities, attributes, nameAttributeKey);
        this.userId = userId;
    }

    @Override
    public String getName() {
        // Return database user ID instead of OAuth subject ID
        return userId.toString();
    }

    public Long getUserId() {
        return userId;
    }
}
