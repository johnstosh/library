package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RandomAuthorTest {

    @Test
    void testRandomAuthorNameGeneration() {
        RandomAuthor randomAuthor = new RandomAuthor();
        String name = randomAuthor.getName();
        assertNotNull(name);
        assertFalse(name.isEmpty());
        String[] nameParts = name.split(" ");
        assertEquals(3, nameParts.length);
    }
}