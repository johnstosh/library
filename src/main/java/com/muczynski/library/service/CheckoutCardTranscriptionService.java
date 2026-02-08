/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.muczynski.library.dto.CheckoutCardTranscriptionDto;
import com.muczynski.library.exception.LibraryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for transcribing library checkout cards from photos using Grok AI.
 * Extracts book information and last checkout details from card images.
 */
@Service
@Slf4j
public class CheckoutCardTranscriptionService {

    private static final String TRANSCRIPTION_PROMPT = """
        You are transcribing data from a library book checkout card image. The image shows a pocket or slip from a book,
        typically arranged as follows:

        - At the top: The library name, such as "St. Martin de Porres Branch of the Sacred Heart Library System"
        (printed in black or red text), often with a small cross symbol below it.
        - Below that: A table with three columns labeled "Date", "Issued To", and "Due". The table has multiple blank rows
        (usually 4-6) for entries. These rows are expected to be empty except possibly the last one, which may have
        handwritten entries for the most recent checkout.
        - At the bottom: A sticker or label with the book's details, printed in black text. This includes:
          - The book title on the first line (e.g., "The Pushcart War").
          - The author's name on the second line (e.g., "Jean Merrill").
          - A call number in a boxed section on the right, often in the format like "PZ 7 .M5453 5"
          (split across lines, with the main code on top and a number below).

        Your task is to extract ONLY the following information from the image:
        - The book title (printed).
        - The author name (printed).
        - The call number (printed, including any dots, letters, numbers, or formatting).
        - From the table's LAST ROW only: The "Date" (handwritten or blank), "Issued To" (handwritten or blank), and "Due" (handwritten or blank). Ignore all other rows.

        Handle variations: The table entries may be handwritten and slightly messy, but focus on the bottommost filled or visible row. The overall image may be slightly tilted, cropped, or have a beige background, but the structure is consistent.

        Output the extracted data in JSON format exactly like this, with no additional text:

        {
          "title": "extracted title",
          "author": "extracted author",
          "call_number": "extracted call number",
          "last_date": "extracted last Date or N/A",
          "last_issued_to": "extracted last Issued To or N/A",
          "last_due": "extracted last Due or N/A"
        }

        Analyze the image and transcribe accordingly.
        """;

    private final AskGrok askGrok;
    private final ObjectMapper snakeCaseMapper;

    public CheckoutCardTranscriptionService(AskGrok askGrok) {
        this.askGrok = askGrok;
        // Create ObjectMapper configured for snake_case to parse Grok's response
        this.snakeCaseMapper = new ObjectMapper();
        this.snakeCaseMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    /**
     * Transcribe a checkout card photo and extract book/checkout information.
     *
     * @param imageBytes Photo bytes
     * @param contentType Image content type (e.g., "image/jpeg")
     * @return Transcription result with book and checkout details
     * @throws LibraryException if transcription fails or JSON parsing fails
     */
    public CheckoutCardTranscriptionDto transcribeCheckoutCard(byte[] imageBytes, String contentType) {
        log.info("Transcribing checkout card photo ({} bytes, type: {})", imageBytes.length, contentType);

        // Call Grok AI with the transcription prompt 
        String grokResponse = askGrok.analyzePhoto(imageBytes, contentType, TRANSCRIPTION_PROMPT, AskGrok.MODEL_GROK_4_FAST);
        log.debug("Grok response: {}", grokResponse);

        // Parse JSON response
        try {
            // Extract JSON from response (Grok might include extra text)
            String jsonResponse = extractJson(grokResponse);
            log.info("Extracted JSON: {}", jsonResponse);
            CheckoutCardTranscriptionDto result = snakeCaseMapper.readValue(jsonResponse, CheckoutCardTranscriptionDto.class);
            log.info("Successfully transcribed checkout card - title: {}, author: {}", result.getTitle(), result.getAuthor());
            return result;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Grok response as JSON: {}", grokResponse, e);
            throw new LibraryException("Failed to parse transcription result: " + e.getMessage());
        }
    }

    /**
     * Extract JSON object from Grok response text.
     * Handles cases where Grok includes extra text before/after the JSON.
     */
    private String extractJson(String response) {
        if (response == null || response.isEmpty()) {
            log.warn("Empty or null response from Grok");
            throw new LibraryException("Empty response from Grok AI");
        }

        // Look for JSON object in the response
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');

        if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) {
            log.warn("Could not find JSON object in response, returning as-is: {}", response);
            return response;
        }

        String json = response.substring(startIndex, endIndex + 1);
        log.debug("Extracted JSON: {}", json);
        return json;
    }
}
