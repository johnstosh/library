// (c) Copyright 2025 by Muczynski
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RandomAuthorTest {

    @Test
    void testRandomAuthorCreation() {
        RandomAuthor randomAuthor = new RandomAuthor();
        Author author = randomAuthor.create();
        assertNotNull(author);
        assertNotNull(author.getName());
        assertFalse(author.getName().isEmpty());
    }
}