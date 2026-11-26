/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.domain;

/**
 * An SSO account has a provider other than {@code local}, or an SSO subject id.
 * A missing provider and a {@code local} provider are local accounts unless a subject id is set.
 */
public final class SsoAccounts {

    private SsoAccounts() {
    }

    public static boolean isSso(User user) {
        if (user == null) {
            return false;
        }
        String provider = user.getSsoProvider();
        if (provider != null && !provider.equals("local")) {
            return true;
        }
        String subject = user.getSsoSubjectId();
        return subject != null && !subject.isBlank();
    }
}
