/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookLabels;
import com.muczynski.library.email.HtmlText;
import com.muczynski.library.service.MailingListWindows.DayWindow;
import com.muczynski.library.service.MailingListWindows.InstantWindow;

import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Picks the books for one issue and renders each book as its own HTML block.
 * Book has no deleted or removed flag; blank titles are skipped.
 */
public final class MailingListIssues {

    public static final String NO_BOOKS = "There are no books in this issue.";

    public record Issue(String text, String html, int bookCount) {
        public static Issue none() {
            return new Issue(NO_BOOKS, "<p>" + NO_BOOKS + "</p>", 0);
        }
    }

    private MailingListIssues() {
    }

    public static Issue saints(List<Book> books, DayWindow window, String baseUrl) {
        List<Book> matches = new ArrayList<>();
        if (books != null) {
            for (Book book : books) {
                if (eligible(book) && MailingListWindows.feastInWindow(book.getRelatedFeastDay(), window)) {
                    matches.add(book);
                }
            }
        }
        matches.sort(Comparator
                .comparing(Book::getRelatedFeastDay, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MailingListIssues::title, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Book::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return render(matches, true, baseUrl);
    }

    public static Issue catalog(List<Book> books, InstantWindow window, String baseUrl) {
        List<Book> matches = new ArrayList<>();
        if (books != null) {
            for (Book book : books) {
                if (eligible(book) && addedIn(book, window)) {
                    matches.add(book);
                }
            }
        }
        matches.sort(Comparator
                .comparing(Book::getDateAddedToLibrary, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MailingListIssues::title, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Book::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return render(matches, false, baseUrl);
    }

    public static Issue oneRandom(List<Book> books, List<String> requiredTags, String baseUrl, Random random) {
        List<Book> matches = tagged(books, requiredTags);
        if (matches.isEmpty()) {
            return Issue.none();
        }
        Random source = random == null ? new Random() : random;
        Book picked = matches.get(source.nextInt(matches.size()));
        return render(List.of(picked), false, baseUrl);
    }

    static List<Book> tagged(List<Book> books, List<String> requiredTags) {
        List<Book> matches = new ArrayList<>();
        if (books == null || requiredTags == null || requiredTags.isEmpty()) {
            return matches;
        }
        for (Book book : books) {
            if (eligible(book) && hasAllTags(book, requiredTags)) {
                matches.add(book);
            }
        }
        matches.sort(Comparator.comparing(Book::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return matches;
    }

    static boolean eligible(Book book) {
        return book != null
                && book.getId() != null
                && book.getTitle() != null
                && !book.getTitle().isBlank();
    }

    static boolean hasAllTags(Book book, List<String> requiredTags) {
        Set<String> have = new HashSet<>();
        if (book.getTagsList() != null) {
            for (String tag : book.getTagsList()) {
                String canonical = BookLabels.getCanonicalLabel(tag);
                if (canonical != null) {
                    have.add(canonical);
                }
            }
        }
        for (String required : requiredTags) {
            if (!have.contains(required)) {
                return false;
            }
        }
        return true;
    }

    private static boolean addedIn(Book book, InstantWindow window) {
        if (window == null) {
            return false;
        }
        LocalDateTime added = book.getDateAddedToLibrary();
        if (added == null) {
            return false;
        }
        return !added.isBefore(window.startUtcInclusive()) && added.isBefore(window.endUtcExclusive());
    }

    private static Issue render(List<Book> books, boolean includeFeast, String baseUrl) {
        if (books.isEmpty()) {
            return Issue.none();
        }
        String base = baseUrl == null ? "" : baseUrl;
        StringBuilder text = new StringBuilder();
        StringBuilder html = new StringBuilder();
        for (Book book : books) {
            String title = book.getTitle().trim();
            String url = base + "/books/" + book.getId();
            if (text.length() > 0) {
                text.append("\n\n");
            }
            text.append(title).append('\n').append(url);
            html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:0 0 12px 0;\">");
            html.append("<tr><td style=\"padding:12px 14px;background-color:#FFFFFF;border:1px solid #DCC4C9;\">");
            html.append("<a href=\"").append(HtmlText.escape(url))
                    .append("\" style=\"color:#6B2D3C;font-weight:bold;text-decoration:none;\">");
            html.append(HtmlText.escape(title)).append("</a>");
            String author = authorName(book);
            if (!author.isEmpty()) {
                html.append("<br><span style=\"color:#3E3B36;\">")
                        .append(HtmlText.escape(author)).append("</span>");
            }
            if (includeFeast && book.getRelatedFeastDay() != null) {
                String date = feastDate(book.getRelatedFeastDay());
                text.append('\n').append(date);
                html.append("<br><span style=\"color:#2F664B;\">")
                        .append(HtmlText.escape(date)).append("</span>");
                String explanation = book.getRelatedFeastDayExplanation();
                if (explanation != null && !explanation.isBlank()) {
                    String note = explanation.trim();
                    text.append(" — ").append(note);
                    html.append("<br><span style=\"color:#6B665E;\">")
                            .append(HtmlText.escape(note)).append("</span>");
                }
            }
            html.append("</td></tr></table>");
        }
        return new Issue(text.toString(), html.toString(), books.size());
    }

    private static String authorName(Book book) {
        Author author = book.getAuthor();
        if (author == null || author.getName() == null || author.getName().isBlank()) {
            return "";
        }
        return author.getName().trim();
    }

    private static String feastDate(MonthDay feast) {
        String month = feast.getMonth().getDisplayName(TextStyle.FULL, Locale.US);
        return month + " " + feast.getDayOfMonth();
    }

    private static String title(Book book) {
        return book.getTitle() == null ? "" : book.getTitle();
    }
}
