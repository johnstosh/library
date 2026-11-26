/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MainCharacterSexTest {

    @Test
    void blankIsNullAndNamesAreAcceptedInEitherCase() {
        assertNull(MainCharacterSex.fromString(null));
        assertNull(MainCharacterSex.fromString(""));
        assertNull(MainCharacterSex.fromString("  "));
        assertEquals(MainCharacterSex.MALE, MainCharacterSex.fromString("male"));
        assertEquals(MainCharacterSex.FEMALE, MainCharacterSex.fromString("Female"));
        assertEquals("Male", MainCharacterSex.MALE.displayName());
        assertEquals("Female", MainCharacterSex.FEMALE.displayName());
    }

    @Test
    void anythingElseIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> MainCharacterSex.fromString("both"));
    }
}
