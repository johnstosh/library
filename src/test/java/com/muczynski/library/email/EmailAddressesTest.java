/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmailAddressesTest {

    @Test
    void isValid_acceptsTypicalAddress() {
        assertTrue(EmailAddresses.isValid("librarian@example.com"));
        assertTrue(EmailAddresses.isValid("  a.b+tag@mail.example.org  "));
    }

    @Test
    void isValid_rejectsBlankAndMalformed() {
        assertFalse(EmailAddresses.isValid(null));
        assertFalse(EmailAddresses.isValid(""));
        assertFalse(EmailAddresses.isValid("   "));
        assertFalse(EmailAddresses.isValid("not-an-email"));
        assertFalse(EmailAddresses.isValid("missing-domain@"));
        assertFalse(EmailAddresses.isValid("spaces nina.v@example.com"));
    }

    @Test
    void parseRecipientList_splitsAndDeduplicates() {
        List<String> parsed = EmailAddresses.parseRecipientList(
                "one@example.com, two@example.com; one@example.com\nthree@example.com");
        assertEquals(List.of("one@example.com", "two@example.com", "three@example.com"), parsed);
    }

    @Test
    void parseRecipientList_skipsInvalid() {
        List<String> parsed = EmailAddresses.parseRecipientList("good@example.com, nope, also@ok.org");
        assertEquals(List.of("good@example.com", "also@ok.org"), parsed);
    }

    @Test
    void mergeUnique_isCaseInsensitive() {
        List<String> merged = EmailAddresses.mergeUnique(
                List.of("A@example.com"),
                List.of("a@example.com", "b@example.com"));
        assertEquals(List.of("A@example.com", "b@example.com"), merged);
    }
}
