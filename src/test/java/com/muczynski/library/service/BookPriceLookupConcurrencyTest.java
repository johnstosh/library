/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.Library;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.BranchRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue 334: a long AbeBooks lookup must not hold the only Hikari connection,
 * or a second tab cannot load Data Management.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookPriceLookupConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookPriceService bookPriceService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private AbeBooksClient abeBooksClient;

    private Book book;

    @BeforeEach
    void setUp() {
        Author author = new Author();
        author.setName("Jane Austen " + UUID.randomUUID());
        author = authorRepository.save(author);

        Library library = new Library();
        library.setBranchName("Price Lookup " + UUID.randomUUID());
        library.setLibrarySystemName("Test System");
        library = branchRepository.save(library);

        book = new Book();
        book.setTitle("Pride and Prejudice " + UUID.randomUUID());
        book.setAuthor(author);
        book.setLibrary(library);
        book.setStatus(BookStatus.ACTIVE);
        book = bookRepository.save(book);
    }

    @AfterEach
    void tearDown() {
        if (book != null && book.getId() != null) {
            bookRepository.deleteById(book.getId());
        }
    }

    @Test
    void lookupDoesNotHoldPoolConnectionDuringAbeBooksHttp() throws Exception {
        CountDownLatch inHttp = new CountDownLatch(1);
        CountDownLatch releaseHttp = new CountDownLatch(1);
        AtomicInteger activeDuringHttp = new AtomicInteger(-1);

        when(abeBooksClient.findCheapestGoodOrBetter(any(), any())).thenAnswer(invocation -> {
            activeDuringHttp.set(activeConnections());
            inHttp.countDown();
            assertTrue(releaseHttp.await(10, TimeUnit.SECONDS), "test did not release AbeBooks latch");
            return AbeBooksCoverListings.builder().build();
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> lookup = executor.submit(() -> bookPriceService.lookupAndUpdateBook(book.getId()));
            assertTrue(inHttp.await(5, TimeUnit.SECONDS), "AbeBooks HTTP never started");
            assertEquals(0, activeDuringHttp.get(),
                    "lookup must release the pool connection during AbeBooks HTTP, active="
                            + activeDuringHttp.get());

            long started = System.nanoTime();
            mockMvc.perform(get("/api/import/stats")
                            .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN"))))
                    .andExpect(status().isOk());
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            assertTrue(elapsedMs < 1500,
                    "Data Management stats should not wait for AbeBooks HTTP, elapsedMs=" + elapsedMs);

            releaseHttp.countDown();
            lookup.get(10, TimeUnit.SECONDS);
        } finally {
            releaseHttp.countDown();
            executor.shutdownNow();
        }
    }

    private int activeConnections() throws Exception {
        HikariDataSource hikari = dataSource instanceof HikariDataSource hikariDataSource
                ? hikariDataSource
                : dataSource.unwrap(HikariDataSource.class);
        HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
        return pool.getActiveConnections();
    }
}
