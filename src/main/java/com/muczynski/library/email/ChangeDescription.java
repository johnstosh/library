/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain-text and HTML bodies that list what changed between two snapshots.
 */
public final class ChangeDescription {

    private static final String FONT =
            "Century Schoolbook L, Century Schoolbook, Times New Roman, Times, serif";

    private ChangeDescription() {
    }

    public record Field(String label, String before, String after) {
    }

    public static List<String> lines(boolean created, boolean removed, Field... fields) {
        List<String> lines = new ArrayList<>();
        for (Field field : fields) {
            String before = HtmlText.blankToEmDash(field.before());
            String after = HtmlText.blankToEmDash(field.after());
            if (created) {
                lines.add(field.label() + ": " + after);
            } else if (removed) {
                lines.add(field.label() + ": " + before);
            } else if (!before.equals(after)) {
                lines.add(field.label() + ": " + before + " → " + after);
            }
        }
        return lines;
    }

    public static String text(String intro, List<String> lines, String link) {
        StringBuilder body = new StringBuilder(intro).append("\n\n");
        for (String line : lines) {
            body.append(line).append('\n');
        }
        if (link != null && !link.isBlank()) {
            body.append('\n').append(link).append('\n');
        }
        return body.toString();
    }

    public static String html(String intro, List<String> lines, String link) {
        StringBuilder items = new StringBuilder();
        for (String line : lines) {
            items.append("<li>").append(HtmlText.escape(line)).append("</li>");
        }
        String linkHtml = "";
        if (link != null && !link.isBlank()) {
            String escaped = HtmlText.escape(link);
            linkHtml = "<p><a href=\"" + escaped + "\">" + escaped + "</a></p>";
        }
        return "<div style=\"font-family:" + FONT + ";\">"
                + "<p>" + HtmlText.escape(intro) + "</p>"
                + "<ul>" + items + "</ul>"
                + linkHtml
                + "</div>";
    }
}
