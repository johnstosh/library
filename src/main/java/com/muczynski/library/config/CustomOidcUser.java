/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

/**
 * Custom OidcUser that uses database user ID as the principal name
 * instead of the OAuth subject ID. This simplifies user lookups.
 */
public class CustomOidcUser extends DefaultOidcUser {
    private final Long userId;

    public CustomOidcUser(Collection<? extends GrantedAuthority> authorities,
                          OidcIdToken idToken,
                          OidcUserInfo userInfo,
                          Long userId) {
        super(authorities, idToken, userInfo);
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
