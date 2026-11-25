/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.MailingFrequency;
import com.muczynski.library.service.MailingListWindows.InstantWindow;
import com.muczynski.library.service.MailingListWindows.SendOrigin;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailingListIssuesTest {

    private static final String BASE = "https://library.example";

    @Test
    void tagRulesPickOneRandomBookAndSkipBlankTitles() {
        Book onlyChildrens = book(1L, "Only kids", "childrens");
        Book both = book(2L, "Both", "childrens", "fiction");
        Book fiction = book(3L, "Novel", "fiction");
        Book blank = book(4L, "  ", "childrens", "fiction");
        Book adult = book(5L, "Grown", "adult", "fiction");
        Book readAloud = book(6L, "Aloud", "read-aloud");
        Book variant = book(7L, "Variant", "Children's", "Fiction");
        Book noId = new Book();
        noId.setTitle("Missing id");
        noId.setTagsList(List.of("childrens", "fiction"));
        List<Book> books = List.of(onlyChildrens, both, fiction, blank, adult, readAloud, variant, noId);
        Random last = new Random() {
            @Override
            public int nextInt(int bound) {
                return bound - 1;
            }
        };

        MailingListIssues.Issue childrens = MailingListIssues.oneRandom(
                books, List.of("childrens", "fiction"), BASE, last);
        MailingListIssues.Issue adults = MailingListIssues.oneRandom(
                books, List.of("adult", "fiction"), BASE, last);
        MailingListIssues.Issue aloud = MailingListIssues.oneRandom(
                books, List.of("read-aloud"), BASE, last);

        assertEquals(1, childrens.bookCount());
        assertTrue(childrens.html().contains("Variant"));
        assertTrue(childrens.html().contains(BASE + "/books/7"));
        assertFalse(childrens.text().contains("Only kids"));
        assertFalse(childrens.text().contains("Novel"));
        assertFalse(childrens.text().contains("Grown"));
        assertFalse(childrens.text().contains("Aloud"));
        assertFalse(childrens.text().contains("Missing id"));
        assertEquals(1, adults.bookCount());
        assertTrue(adults.text().contains("Grown"));
        assertFalse(adults.text().contains("Both"));
        assertEquals(1, aloud.bookCount());
        assertTrue(aloud.html().contains("Aloud"));
        assertTrue(aloud.html().contains(BASE + "/books/6"));
    }

    @Test
    void saintsIncludeEveryFeastInTheWindowWithTheDateAndExplanation() {
        Book leap = feast(1L, "Leap Saint", MonthDay.of(2, 29), "kept in ordinary February");
        Book late = feast(2L, "February 28", MonthDay.of(2, 28), null);
        Book march = feast(3L, "March Saint", MonthDay.of(3, 1), "not February");
        Book blank = feast(4L, " ", MonthDay.of(2, 29), "no title");
        Book escaped = feast(5L, "Tom & <Jerry>", MonthDay.of(2, 2), "a <note> & more");
        var window = MailingListWindows.saintDays(
                MailingFrequency.MONTHLY, SendOrigin.SIGNUP, LocalDate.of(2026, 3, 1));

        MailingListIssues.Issue issue = MailingListIssues.saints(
                List.of(march, blank, leap, escaped, late), window, BASE);

        assertEquals(3, issue.bookCount());
        assertTrue(issue.text().indexOf("Tom & <Jerry>") < issue.text().indexOf("February 28"));
        assertTrue(issue.text().indexOf("February 28") < issue.text().indexOf("Leap Saint"));
        assertTrue(issue.text().contains("February 29 — kept in ordinary February"));
        assertTrue(issue.text().contains("February 28"));
        assertTrue(issue.text().contains(BASE + "/books/1"));
        assertFalse(issue.text().contains("March Saint"));
        assertFalse(issue.text().contains("no title"));
        assertTrue(issue.html().contains("Tom &amp; &lt;Jerry&gt;"));
        assertTrue(issue.html().contains("a &lt;note&gt; &amp; more"));
        assertFalse(issue.html().contains("Tom & <Jerry>"));
        assertTrue(issue.html().contains("href=\"" + BASE + "/books/5\""));
    }

    @Test
    void newCatalogUsesTheEasternDayAndSkipsBlankTitles() {
        InstantWindow yesterday = MailingListWindows.catalogAdded(
                MailingFrequency.DAILY, SendOrigin.SCHEDULED, LocalDate.of(2026, 11, 25));
        Book included = added(1L, "Inside", yesterday.startUtcInclusive());
        Book end = added(2L, "Last instant", yesterday.endUtcExclusive().minusNanos(1));
        Book tooEarly = added(3L, "Too early", yesterday.startUtcInclusive().minusNanos(1));
        Book today = added(4L, "Today", yesterday.endUtcExclusive());
        Book blank = added(5L, " ", yesterday.startUtcInclusive());
        Book missing = new Book();
        missing.setId(6L);
        missing.setTitle("No date");

        MailingListIssues.Issue issue = MailingListIssues.catalog(
                List.of(end, today, blank, missing, tooEarly, included), yesterday, BASE);

        assertEquals(2, issue.bookCount());
        assertTrue(issue.text().indexOf("Inside") < issue.text().indexOf("Last instant"));
        assertTrue(issue.html().contains(BASE + "/books/1"));
        assertTrue(issue.html().contains(BASE + "/books/2"));
        assertFalse(issue.text().contains("Too early"));
        assertFalse(issue.text().contains("Today"));
        assertFalse(issue.text().contains("No date"));
    }

    @Test
    void emptyIssueSaysThereAreNoBooks() {
        MailingListIssues.Issue issue = MailingListIssues.oneRandom(
                List.of(), List.of("read-aloud"), BASE, new Random(1));

        assertEquals(0, issue.bookCount());
        assertEquals(MailingListIssues.NO_BOOKS, issue.text());
        assertTrue(issue.html().contains(MailingListIssues.NO_BOOKS));
    }

    private static Book book(Long id, String title, String... tags) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setTagsList(List.of(tags));
        return book;
    }

    private static Book feast(Long id, String title, MonthDay feast, String explanation) {
        Book book = book(id, title);
        book.setRelatedFeastDay(feast);
        book.setRelatedFeastDayExplanation(explanation);
        return book;
    }

    private static Book added(Long id, String title, LocalDateTime added) {
        Book book = book(id, title);
        book.setDateAddedToLibrary(added);
        return book;
    }
}
