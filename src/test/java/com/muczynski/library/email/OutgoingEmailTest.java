/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.email;

import com.muczynski.library.domain.Library;
import com.muczynski.library.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutgoingEmailTest {

    private static final String PROD_HEADER = "St. Martin de Porres\nSacred Heart Library System";
    private static final String SIGN_OFF = "God bless,\n-Saint Martin de Porres";

    @Mock
    private BranchRepository branchRepository;

    private OutgoingEmail outgoingEmail;

    @BeforeEach
    void setUp() {
        outgoingEmail = new OutgoingEmail(branchRepository);
        ReflectionTestUtils.setField(outgoingEmail, "externalBaseUrl", "https://library.muczynskifamily.com");
    }

    @Test
    void productionUsesTheFirstBranchNameAndSystem() {
        when(branchRepository.findAll()).thenReturn(List.of(branch("St. Martin de Porres", "Sacred Heart Library System"),
                branch("Other", "Ignored")));

        OutgoingEmail.Brand brand = outgoingEmail.currentBrand();

        assertEquals("St. Martin de Porres", brand.name());
        assertEquals("Sacred Heart Library System", brand.subName());
    }

    @Test
    void devHostUsesFixedNamesEvenWhenABranchExists() {
        ReflectionTestUtils.setField(outgoingEmail, "externalBaseUrl",
                "https://library-dev.muczynskifamily.com");
        when(branchRepository.findAll()).thenReturn(List.of(branch("St. Martin de Porres", "Sacred Heart Library System")));

        OutgoingEmail.Brand brand = outgoingEmail.currentBrand();

        assertEquals("library-dev", brand.name());
        assertEquals("DEV", brand.subName());
    }

    @Test
    void missingBranchUsesLibraryAndNoSubName() {
        when(branchRepository.findAll()).thenReturn(List.of());

        OutgoingEmail.Brand brand = outgoingEmail.currentBrand();

        assertEquals("Library", brand.name());
        assertEquals("", brand.subName());
    }

    @Test
    void plainTextWrapsBodyWithHeaderSignOffAndFooter() {
        String wrapped = OutgoingEmail.text(
                "St. Martin de Porres",
                "Sacred Heart Library System",
                "A library card application was submitted.\n");

        assertEquals(PROD_HEADER
                + "\n\nA library card application was submitted.\n\n"
                + SIGN_OFF
                + "\n\n"
                + PROD_HEADER, wrapped);
    }

    @Test
    void plainTextOmitsABlankLineWhenSubNameIsEmpty() {
        String wrapped = OutgoingEmail.text("Library", "   ", "Hello\n");

        assertEquals("Library\n\nHello\n\n" + SIGN_OFF + "\n\nLibrary", wrapped);
        assertFalse(wrapped.contains("Library\n\n\n"));
    }

    @Test
    void htmlUsesBold22pxNameAndSubNameAndRepeatsThemAfterTheSignOff() {
        String wrapped = OutgoingEmail.html(
                "St. Martin de Porres",
                "Sacred Heart Library System",
                "<p>Hello</p>");

        String heading = "<div style=\"font-size:22px;font-weight:bold;\">"
                + "St. Martin de Porres<br>Sacred Heart Library System</div>";
        assertEquals("<div style=\"font-family:" + OutgoingEmail.FONT + ";\">"
                + heading
                + "<p>Hello</p>"
                + "<p>God bless,<br>-Saint Martin de Porres</p>"
                + heading
                + "</div>", wrapped);
    }

    @Test
    void htmlOmitsTheSubNameBreakWhenSubNameIsBlankAndEscapesTheName() {
        String wrapped = OutgoingEmail.html("Library <dev>", "", "<p>Hi</p>");

        assertFalse(wrapped.contains("Library &lt;dev&gt;<br>"));
        assertEquals("<div style=\"font-family:" + OutgoingEmail.FONT + ";\">"
                + "<div style=\"font-size:22px;font-weight:bold;\">Library &lt;dev&gt;</div>"
                + "<p>Hi</p>"
                + "<p>God bless,<br>-Saint Martin de Porres</p>"
                + "<div style=\"font-size:22px;font-weight:bold;\">Library &lt;dev&gt;</div>"
                + "</div>", wrapped);
    }

    @Test
    void applyBrandsBothBodiesFromTheHostAndTheFirstBranch() {
        ReflectionTestUtils.setField(outgoingEmail, "externalBaseUrl",
                "https://library-dev-abc123-uc.a.run.app");
        EmailMessage message = new EmailMessage();

        outgoingEmail.apply(message, "Body\n", "<p>Body</p>");

        assertEquals("library-dev\nDEV\n\nBody\n\n" + SIGN_OFF + "\n\nlibrary-dev\nDEV",
                message.getTextBody());
        assertTrue(message.getHtmlBody().startsWith(
                "<div style=\"font-family:" + OutgoingEmail.FONT + ";\">"
                        + "<div style=\"font-size:22px;font-weight:bold;\">library-dev<br>DEV</div>"));
        assertTrue(message.getHtmlBody().endsWith(
                "<p>God bless,<br>-Saint Martin de Porres</p>"
                        + "<div style=\"font-size:22px;font-weight:bold;\">library-dev<br>DEV</div></div>"));
    }

    @Test
    void applyPutsUnsubscribeAfterTheSignOffAndTheRepeatedBrand() {
        when(branchRepository.findAll()).thenReturn(List.of(
                branch("St. Martin de Porres", "Sacred Heart Library System")));
        EmailMessage message = new EmailMessage();
        String url = "https://library.muczynskifamily.com/notifications/unsubscribe/abc";

        outgoingEmail.apply(message, "A library loan was changed.\n", "<p>A library loan was changed.</p>", url);

        String text = message.getTextBody();
        int signOff = text.indexOf(SIGN_OFF);
        int brandAgain = text.indexOf(PROD_HEADER, signOff + SIGN_OFF.length());
        int unsub = text.lastIndexOf("Unsubscribe");
        assertTrue(signOff >= 0);
        assertTrue(brandAgain > signOff);
        assertTrue(unsub > brandAgain);
        assertTrue(text.endsWith("Unsubscribe\n" + url));
        assertFalse(text.substring(unsub).contains("St. Martin"));

        String html = message.getHtmlBody();
        int htmlSign = html.indexOf("-Saint Martin de Porres");
        int htmlBrand = html.lastIndexOf("font-size:22px");
        int htmlUnsub = html.lastIndexOf(">Unsubscribe</a>");
        assertTrue(htmlSign < htmlBrand && htmlBrand < htmlUnsub);
        assertTrue(html.endsWith("<p><a href=\"" + url + "\">Unsubscribe</a></p></div>"));
        assertEquals(1, count(html, ">Unsubscribe</a>"));
    }

    @Test
    void mailingListHtmlPutsTheLastUnsubscribeAfterTheClosing() {
        when(branchRepository.findAll()).thenReturn(List.of(
                branch("St. Martin de Porres", "Sacred Heart Library System")));
        EmailMessage message = new EmailMessage();

        outgoingEmail.composeMailingList(
                message,
                "Lives of Saints",
                "Frequency: weekly. To change frequency, unsubscribe and sign up again.",
                "A Book\nhttps://library.muczynskifamily.com/books/3\nFebruary 3",
                "<p><a href=\"https://library.muczynskifamily.com/books/3\">A Book</a></p>",
                "https://library.muczynskifamily.com/mailing-lists/unsubscribe/abc");

        String html = message.getHtmlBody();
        int nameAt = html.indexOf("St. Martin de Porres");
        int siteAt = html.indexOf("https://library.muczynskifamily.com");
        int firstUnsub = html.indexOf(">Unsubscribe</a>");
        int topicAt = html.indexOf("Lives of Saints");
        int frequencyAt = html.indexOf("Frequency: weekly.");
        int changeAt = html.indexOf("unsubscribe and sign up again");
        int bookAt = html.indexOf("A Book");
        int closingAt = html.indexOf("God bless,");
        int signOffAt = html.indexOf("-Saint Martin de Porres");
        int lastUnsub = html.lastIndexOf(">Unsubscribe</a>");

        assertTrue(nameAt >= 0 && nameAt < siteAt);
        assertTrue(siteAt < firstUnsub);
        assertTrue(firstUnsub < topicAt);
        assertTrue(topicAt < frequencyAt);
        assertTrue(frequencyAt < changeAt);
        assertTrue(changeAt < bookAt);
        assertTrue(bookAt < closingAt);
        assertTrue(closingAt < signOffAt);
        assertTrue(signOffAt < lastUnsub);
        assertEquals(2, count(html, ">Unsubscribe</a>"));
        assertEquals(html.indexOf("font-size:22px"), html.lastIndexOf("font-size:22px"));
        assertTrue(html.contains("Sacred Heart Library System"));
        assertTrue(html.contains(OutgoingEmail.PAPER));
        assertTrue(html.contains(OutgoingEmail.BURGUNDY));
        assertTrue(html.contains(">Open the library</a>"));
        assertTrue(html.contains("width=\"600\""));
        String text = message.getTextBody();
        assertTrue(text.indexOf("God bless,") < text.lastIndexOf("Unsubscribe"));
        assertEquals(1, count(text, "God bless,"));
    }

    @Test
    void mailingListOnTheDevHostUsesTheDevBrandAndDoesNotRepeatTheHeading() {
        ReflectionTestUtils.setField(outgoingEmail, "externalBaseUrl", "https://library-dev.example");
        when(branchRepository.findAll()).thenReturn(List.of(branch("St. Martin de Porres", "Ignored")));
        EmailMessage message = new EmailMessage();

        outgoingEmail.composeMailingList(message, "Adult Fiction",
                "Frequency: monthly. To change frequency, unsubscribe and sign up again.",
                "There are no books in this issue.",
                "<p>There are no books in this issue.</p>",
                "https://library-dev.example/mailing-lists/unsubscribe/abc");

        String html = message.getHtmlBody();
        assertTrue(html.contains("library-dev"));
        assertTrue(html.contains("DEV"));
        assertTrue(html.contains("https://library-dev.example"));
        assertFalse(html.contains("St. Martin de Porres"));
        assertEquals(1, count(html, "font-size:22px"));
        assertTrue(html.lastIndexOf(">Unsubscribe</a>") > html.indexOf("-Saint Martin de Porres"));
    }

    private static int count(String text, String needle) {
        int found = 0;
        int from = 0;
        while (from <= text.length()) {
            int at = text.indexOf(needle, from);
            if (at < 0) {
                return found;
            }
            found++;
            from = at + needle.length();
        }
        return found;
    }

    private static Library branch(String name, String system) {
        Library library = new Library();
        library.setBranchName(name);
        library.setLibrarySystemName(system);
        return library;
    }
}
