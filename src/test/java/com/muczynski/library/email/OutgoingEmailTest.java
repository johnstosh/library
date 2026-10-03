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

    private static Library branch(String name, String system) {
        Library library = new Library();
        library.setBranchName(name);
        library.setLibrarySystemName(system);
        return library;
    }
}
