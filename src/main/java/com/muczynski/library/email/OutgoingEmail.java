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
