// (c) Copyright 2025 by Muczynski
package com.muczynski.library.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SecurityUtils.
 */
class SecurityUtilsTest {

    @Test
    void testIsLibrarian_WithLibrarianAuthority_ReturnsTrue() {
        Authentication auth = mock(Authentication.class);
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("LIBRARIAN")
        );
        when(auth.getAuthorities()).thenReturn((Collection) authorities);

        assertTrue(SecurityUtils.isLibrarian(auth));
    }

    @Test
    void testIsLibrarian_WithUserAuthority_ReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("USER")
        );
        when(auth.getAuthorities()).thenReturn((Collection) authorities);

        assertFalse(SecurityUtils.isLibrarian(auth));
    }

    @Test
    void testIsLibrarian_WithMultipleAuthorities_ReturnsTrue() {
        Authentication auth = mock(Authentication.class);
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("USER"));
        authorities.add(new SimpleGrantedAuthority("LIBRARIAN"));
        when(auth.getAuthorities()).thenReturn((Collection) authorities);

        assertTrue(SecurityUtils.isLibrarian(auth));
    }

    @Test
    void testIsLibrarian_WithNoAuthorities_ReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(Collections.emptyList());

        assertFalse(SecurityUtils.isLibrarian(auth));
    }

    @Test
    void testIsLibrarian_WithNullAuthentication_ReturnsFalse() {
        assertFalse(SecurityUtils.isLibrarian(null));
    }

    @Test
    void testIsLibrarian_WithNullAuthorities_ReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(null);

        assertFalse(SecurityUtils.isLibrarian(auth));
    }

    @Test
    void testIsUser_WithUserAuthority_ReturnsTrue() {
        Authentication auth = mock(Authentication.class);
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("USER")
        );
        when(auth.getAuthorities()).thenReturn((Collection) authorities);

        assertTrue(SecurityUtils.isUser(auth));
    }

    @Test
    void testIsUser_WithLibrarianAuthority_ReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("LIBRARIAN")
        );
        when(auth.getAuthorities()).thenReturn((Collection) authorities);

        assertFalse(SecurityUtils.isUser(auth));
    }

    @Test
    void testIsUser_WithNullAuthentication_ReturnsFalse() {
        assertFalse(SecurityUtils.isUser(null));
    }

    @Test
    void testHasAuthority_WithMatchingAuthority_ReturnsTrue() {
        Authentication auth = mock(Authentication.class);
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("CUSTOM_AUTHORITY")
        );
        when(auth.getAuthorities()).thenReturn((Collection) authorities);

        assertTrue(SecurityUtils.hasAuthority(auth, "CUSTOM_AUTHORITY"));
    }

    @Test
    void testHasAuthority_WithNonMatchingAuthority_ReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("USER")
        );
        when(auth.getAuthorities()).thenReturn((Collection) authorities);

        assertFalse(SecurityUtils.hasAuthority(auth, "ADMIN"));
    }

    @Test
    void testHasAuthority_WithNullAuthentication_ReturnsFalse() {
        assertFalse(SecurityUtils.hasAuthority(null, "LIBRARIAN"));
    }

    @Test
    void testHasAuthority_WithNullAuthority_ReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("USER")
        );
        when(auth.getAuthorities()).thenReturn((Collection) authorities);

        assertFalse(SecurityUtils.hasAuthority(auth, null));
    }

    @Test
    void testHasAuthority_WithNullAuthorities_ReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(null);

        assertFalse(SecurityUtils.hasAuthority(auth, "LIBRARIAN"));
    }

    @Test
    void testConstructor_ThrowsException() {
        // Use reflection to access private constructor
        try {
            java.lang.reflect.Constructor<SecurityUtils> constructor = SecurityUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Expected UnsupportedOperationException to be thrown");
        } catch (Exception e) {
            // When using reflection, the exception is wrapped in InvocationTargetException
            assertTrue(e instanceof java.lang.reflect.InvocationTargetException);
            assertTrue(e.getCause() instanceof UnsupportedOperationException);
            assertEquals("Utility class", e.getCause().getMessage());
        }
    }
}
