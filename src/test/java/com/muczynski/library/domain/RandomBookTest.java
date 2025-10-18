package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RandomBookTest {

    @Test
    void testRandomBookTitleGeneration() {
        RandomBook randomBook = new RandomBook();
        String title = randomBook.getTitle();
        assertNotNull(title);
        assertFalse(title.isEmpty());
        assertTrue(title.startsWith("The "));
        String[] titleParts = title.split(" ");
        assertEquals(4, titleParts.length);
    }
}