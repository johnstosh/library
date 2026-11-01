/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FavoriteListParamsTest {

    @Test
    void keepsACommaInsideOneListName() {
        assertEquals(
                List.of("ACLA, read in Pittsburgh", "Have Read"),
                FavoriteListParams.split("~ACLA%2C%20read%20in%20Pittsburgh,Have Read"));
    }

    @Test
    void keepsPlainCommaSeparatedNames() {
        assertEquals(List.of("Have Read", "Saints"), FavoriteListParams.split("Have Read, Saints"));
    }

    @Test
    void keepsANameThatStartsWithTheEncodingMark() {
        assertEquals(List.of("~Mine"), FavoriteListParams.split("~~Mine"));
    }

    @Test
    void blankValueIsEmpty() {
        assertEquals(List.of(), FavoriteListParams.split(null));
        assertEquals(List.of(), FavoriteListParams.split("  "));
    }
}
