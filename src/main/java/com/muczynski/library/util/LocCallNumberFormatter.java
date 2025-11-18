/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for formatting Library of Congress (LOC) call numbers.
 */
public class LocCallNumberFormatter {

    /**
     * Formats a LOC call number for display on a book spine label.
     *
     * Each component of the call number is placed on its own line because
     * library book spine labels have limited horizontal space. Breaking up
     * the call number makes it easier to read vertically on the spine.
     *
     * Examples:
     *   Input:  "BX 4705.M124 A77 2005"
     *   Output: "BX\n4705\n.M124\nA77\n2005"
     *
     *   Input:  "BV210 .3 .B464 2013"
     *   Output: "BV\n210\n.3\n.B464\n2013"
     *
     * The parsing rules are:
     * 1. Split by spaces to get major components
     * 2. For components that start with letters followed by digits (e.g., "BV210"),
     *    split into the letter prefix and the rest (e.g., "BV", "210")
     * 3. For components containing a period (e.g., "4705.M124"), split at the
     *    period keeping the period with the second part (e.g., "4705", ".M124")
     *
     * @param locNumber the LOC call number to format (e.g., "BX 4705.M124 A77 2005")
     * @return the formatted call number with each component on its own line,
     *         or empty string if input is null or blank
     */
    public static String formatForSpine(String locNumber) {
        if (locNumber == null || locNumber.isBlank()) {
            return "";
        }

        List<String> parts = new ArrayList<>();

        // Split by spaces first
        String[] spaceParts = locNumber.trim().split("\\s+");

        for (String part : spaceParts) {
            // First, check if this part starts with letters followed by digits (like "BV210")
            // This handles cases where class letters and numbers are not separated by space
            if (part.length() > 1 && Character.isLetter(part.charAt(0))) {
                int firstDigitIndex = -1;
                for (int i = 0; i < part.length(); i++) {
                    if (Character.isDigit(part.charAt(i))) {
                        firstDigitIndex = i;
                        break;
                    }
                }

                // If we found digits after letters, split there
                if (firstDigitIndex > 0) {
                    String letterPart = part.substring(0, firstDigitIndex);
                    String restPart = part.substring(firstDigitIndex);

                    // Add the letter part
                    parts.add(letterPart);

                    // Now process the rest (which starts with digits) for periods
                    int periodIndex = restPart.indexOf('.');
                    if (periodIndex > 0) {
                        parts.add(restPart.substring(0, periodIndex));
                        parts.add(restPart.substring(periodIndex));
                    } else {
                        parts.add(restPart);
                    }
                    continue;
                }
            }

            // Check if this part contains a period (like "4705.M124")
            int periodIndex = part.indexOf('.');
            if (periodIndex > 0) {
                // Split at the period, keeping the period with the second part
                parts.add(part.substring(0, periodIndex));
                parts.add(part.substring(periodIndex)); // includes the period
            } else {
                parts.add(part);
            }
        }

        return String.join("\n", parts);
    }

    /**
     * Formats a LOC call number for HTML display on a book spine label.
     *
     * Same as {@link #formatForSpine(String)} but uses HTML line breaks
     * instead of newline characters for web display.
     *
     * @param locNumber the LOC call number to format
     * @return the formatted call number with HTML &lt;br&gt; tags between components,
     *         or empty string if input is null or blank
     */
    public static String formatForSpineHtml(String locNumber) {
        if (locNumber == null || locNumber.isBlank()) {
            return "";
        }
        return formatForSpine(locNumber).replace("\n", "<br>");
    }
}
