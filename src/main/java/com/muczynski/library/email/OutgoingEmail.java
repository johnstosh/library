/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.email;

import com.muczynski.library.domain.Library;
import com.muczynski.library.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Letterhead for every outgoing library email. Transport senders do not brand.
 * The name and sub-name follow the same rule as frontend libraryBrand():
 * a library-dev host uses fixed names; otherwise the first branch, or
 * "Library" with no sub-name when there is no branch.
 */
@Component
public class OutgoingEmail {

    static final String FONT =
            "Century Schoolbook L, Century Schoolbook, Times New Roman, Times, serif";

    static final String SIGN_OFF_TEXT = "God bless,\n-Saint Martin de Porres";

    static final String SIGN_OFF_HTML = "God bless,<br>-Saint Martin de Porres";

    private static final String HEADING_STYLE = "font-size:22px;font-weight:bold;";

    private final BranchRepository branchRepository;

    @Value("${app.external-base-url:https://library.muczynskifamily.com}")
    private String externalBaseUrl;

    public OutgoingEmail(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public void apply(EmailMessage message, String textBody, String htmlBody) {
        Brand brand = currentBrand();
        message.setTextBody(text(brand.name(), brand.subName(), textBody));
        message.setHtmlBody(html(brand.name(), brand.subName(), htmlBody));
    }

    /**
     * Mailing-list letter. {@link #apply} puts the sign-off and then repeats the
     * heading, which would bury the final unsubscribe link, so this path does
     * not call it. Loan and application mail keep using {@link #apply}.
     * Order: library name, website, unsubscribe, topic, frequency, issue,
     * sign-off, and a last Unsubscribe link.
     */
    public void composeMailingList(EmailMessage message,
                                   String topic,
                                   String frequencyLine,
                                   String issueText,
                                   String issueHtml,
                                   String unsubscribeUrl) {
        Brand brand = currentBrand();
        String site = website();
        message.setTextBody(mailingListText(
                brand.name(), brand.subName(), site, topic, frequencyLine, issueText, unsubscribeUrl));
        message.setHtmlBody(mailingListHtml(
                brand.name(), brand.subName(), site, topic, frequencyLine, issueHtml, unsubscribeUrl));
    }

    public String website() {
        String base = externalBaseUrl == null ? "" : externalBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            return "https://library.muczynskifamily.com";
        }
        return base;
    }

    Brand currentBrand() {
        return brandFor(externalBaseUrl, branchRepository.findAll());
    }

    /**
     * Same host test as frontend isDevSite: the external base URL contains
     * "library-dev". Does not create a branch when none exists.
     */
    static Brand brandFor(String externalBaseUrl, List<Library> branches) {
        if (externalBaseUrl != null && externalBaseUrl.contains("library-dev")) {
            return new Brand("library-dev", "DEV");
        }
        if (branches == null || branches.isEmpty() || branches.get(0) == null) {
            return new Brand("Library", "");
        }
        Library first = branches.get(0);
        String name = first.getBranchName() == null ? "" : first.getBranchName();
        String subName = first.getLibrarySystemName() == null ? "" : first.getLibrarySystemName();
        return new Brand(name, subName);
    }

    static String text(String name, String subName, String body) {
        String header = plainHeader(name, subName);
        String content = stripTrailingNewlines(body == null ? "" : body);
        StringBuilder letter = new StringBuilder();
        if (!header.isEmpty()) {
            letter.append(header).append("\n\n");
        }
        letter.append(content);
        letter.append("\n\n");
        letter.append(SIGN_OFF_TEXT);
        if (!header.isEmpty()) {
            letter.append("\n\n").append(header);
        }
        return letter.toString();
    }

    static String html(String name, String subName, String bodyHtml) {
        String heading = htmlHeading(name, subName);
        String content = bodyHtml == null ? "" : bodyHtml;
        return "<div style=\"font-family:" + FONT + ";\">"
                + heading
                + content
                + "<p>" + SIGN_OFF_HTML + "</p>"
                + heading
                + "</div>";
    }

    private static String plainHeader(String name, String subName) {
        String safeName = name == null ? "" : name;
        boolean hasSub = subName != null && !subName.isBlank();
        if (safeName.isEmpty() && !hasSub) {
            return "";
        }
        if (!hasSub) {
            return safeName;
        }
        if (safeName.isEmpty()) {
            return subName.trim();
        }
        return safeName + "\n" + subName.trim();
    }

    private static String htmlHeading(String name, String subName) {
        String safeName = name == null ? "" : name;
        boolean hasSub = subName != null && !subName.isBlank();
        if (safeName.isEmpty() && !hasSub) {
            return "";
        }
        StringBuilder heading = new StringBuilder();
        heading.append("<div style=\"").append(HEADING_STYLE).append("\">");
        heading.append(HtmlText.escape(safeName));
        if (hasSub) {
            if (!safeName.isEmpty()) {
                heading.append("<br>");
            }
            heading.append(HtmlText.escape(subName.trim()));
        }
        heading.append("</div>");
        return heading.toString();
    }

    static String mailingListText(String name,
                                  String subName,
                                  String website,
                                  String topic,
                                  String frequencyLine,
                                  String issueText,
                                  String unsubscribeUrl) {
        String books = issueText == null || issueText.isBlank()
                ? "There are no books in this issue."
                : stripTrailingNewlines(issueText);
        StringBuilder letter = new StringBuilder();
        String header = plainHeader(name, subName);
        if (!header.isEmpty()) {
            letter.append(header).append("\n");
        }
        letter.append(website == null ? "" : website).append("\n\n");
        letter.append("Unsubscribe\n").append(unsubscribeUrl == null ? "" : unsubscribeUrl).append("\n\n");
        letter.append(topic == null ? "" : topic).append("\n\n");
        letter.append(frequencyLine == null ? "" : frequencyLine).append("\n\n");
        letter.append(books).append("\n\n");
        letter.append(SIGN_OFF_TEXT).append("\n\n");
        letter.append("Unsubscribe\n").append(unsubscribeUrl == null ? "" : unsubscribeUrl);
        return letter.toString();
    }

    static String mailingListHtml(String name,
                                  String subName,
                                  String website,
                                  String topic,
                                  String frequencyLine,
                                  String issueHtml,
                                  String unsubscribeUrl) {
        String site = website == null ? "" : website;
        String unsub = unsubscribeUrl == null ? "" : unsubscribeUrl;
        String books = issueHtml == null || issueHtml.isBlank()
                ? "<p>There are no books in this issue.</p>"
                : issueHtml;
        return "<div style=\"font-family:" + FONT + ";\">"
                + htmlHeading(name, subName)
                + "<p><a href=\"" + HtmlText.escape(site) + "\">" + HtmlText.escape(site) + "</a></p>"
                + "<p><a href=\"" + HtmlText.escape(unsub) + "\">Unsubscribe</a></p>"
                + "<p>" + HtmlText.escape(topic) + "</p>"
                + "<p>" + HtmlText.escape(frequencyLine) + "</p>"
                + books
                + "<p>" + SIGN_OFF_HTML + "</p>"
                + "<p><a href=\"" + HtmlText.escape(unsub) + "\">Unsubscribe</a></p>"
                + "</div>";
    }

    private static String stripTrailingNewlines(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '\n') {
            end--;
        }
        return value.substring(0, end);
    }

    record Brand(String name, String subName) {
    }
}
