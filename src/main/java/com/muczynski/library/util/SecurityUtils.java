// (c) Copyright 2025 by Muczynski
package com.muczynski.library.util;

import org.springframework.security.core.Authentication;

/**
 * Utility class for common security and authentication operations.
 */
public class SecurityUtils {

    private SecurityUtils() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Checks if the authenticated user has LIBRARIAN authority.
     * <p>
     * This method uses a stream-based approach to check the authority string value,
     * which is reliable regardless of how authorities are loaded (database entities
     * vs SimpleGrantedAuthority instances).
     * <p>
     * IMPORTANT: Do NOT use authentication.getAuthorities().contains(new SimpleGrantedAuthority("LIBRARIAN"))
     * as it relies on object equality which can fail depending on the authentication method.
     *
     * @param authentication the Spring Security Authentication object
     * @return true if the user has LIBRARIAN authority, false otherwise
     */
    public static boolean isLibrarian(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "LIBRARIAN".equals(auth.getAuthority()));
    }

    /**
     * Checks if the authenticated user has USER authority.
     * <p>
     * This method uses a stream-based approach to check the authority string value,
     * which is reliable regardless of how authorities are loaded.
     *
     * @param authentication the Spring Security Authentication object
     * @return true if the user has USER authority, false otherwise
     */
    public static boolean isUser(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "USER".equals(auth.getAuthority()));
    }

    /**
     * Checks if the authenticated user has the specified authority.
     * <p>
     * This method uses a stream-based approach to check the authority string value,
     * which is reliable regardless of how authorities are loaded.
     *
     * @param authentication the Spring Security Authentication object
     * @param authority the authority name to check (e.g., "LIBRARIAN", "USER")
     * @return true if the user has the specified authority, false otherwise
     */
    public static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authentication.getAuthorities() == null || authority == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> authority.equals(auth.getAuthority()));
    }
}
