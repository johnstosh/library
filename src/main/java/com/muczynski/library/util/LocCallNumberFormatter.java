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
     * Formats a LOC call number for display on a book pocket label.
     *
     * Each component of the call number is placed on its own line because
     * library book pocket labels have limited horizontal space. Breaking up
     * the call number makes it easier to read on the label.
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
     * 2. For the FIRST component only, if it starts with letters followed by digits
     *    (e.g., "BV210"), split into the letter prefix and the rest (e.g., "BV", "210")
     * 3. For components containing a period followed by a LETTER (e.g., "4705.M124"),
     *    split at that period keeping the period with the second part (e.g., "4705", ".M124")
     * 4. Periods followed by digits are NOT split (e.g., "1009.5" stays together)
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

        for (int componentIndex = 0; componentIndex < spaceParts.length; componentIndex++) {
            String part = spaceParts[componentIndex];

            // Only for the FIRST component: check if it starts with letters followed by digits
            // This handles cases where class letters and numbers are not separated by space
            if (componentIndex == 0 && part.length() > 1 && Character.isLetter(part.charAt(0))) {
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

                    // Now process the rest for periods followed by letters
                    splitAtLetterPeriods(restPart, parts);
                    continue;
                }
            }

            // Process for periods followed by letters
            splitAtLetterPeriods(part, parts);
        }

        return String.join("\n", parts);
    }

    /**
     * Splits a string at periods that are followed by letters, adding results to parts list.
     * Periods followed by digits are NOT split points (e.g., "1009.5" stays together).
     */
    private static void splitAtLetterPeriods(String part, List<String> parts) {
        if (part.isEmpty()) {
            return;
        }

        // Find all period positions where the period is followed by a letter
        int startIndex = 0;
        for (int i = 0; i < part.length(); i++) {
            if (part.charAt(i) == '.' && i + 1 < part.length() && Character.isLetter(part.charAt(i + 1))) {
                // Found a period followed by a letter - split here
                if (i > startIndex) {
                    parts.add(part.substring(startIndex, i));
                }
                startIndex = i; // Start next part from the period
            }
        }

        // Add the remaining part
        if (startIndex < part.length()) {
            parts.add(part.substring(startIndex));
        }
    }

    /**
     * Formats a LOC call number for HTML display on a book pocket label.
     *
     * Same as {@link #formatForSpine(String)} but uses HTML line breaks
     * instead of newline characters for web display.
     *
     * Note: The method name references "spine" for historical reasons, but
     * this formatting is actually used for book pocket labels.
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
