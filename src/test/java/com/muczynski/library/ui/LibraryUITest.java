package com.muczynski.library.ui;

import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// This class has been split into feature-specific UI test classes:
// - AuthorsUITest
// - BooksUITest
// - LibrariesUITest
// - LoansUITest
// - UsersUITest
// Common setup can be extracted to a base class if needed in the future.

@SpringBootTest(classes = LibraryApplication.class)
class LibraryUITest {

    @Test
    void contextLoads() {
        // Empty after split
    }
}
