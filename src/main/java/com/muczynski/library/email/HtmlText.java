/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

/**
 * Tiny HTML helpers for email bodies. Keep this local so email composition does
 * not depend on a templating library.
 */
public final class HtmlText {

    private HtmlText() {
    }

    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static String blankToEmDash(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value.trim();
    }
}
