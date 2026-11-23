/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.GrokCreditsExhaustedException;
import com.muczynski.library.exception.LibraryException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.BookLabels;
import com.muczynski.library.domain.ReadingDifficulty;
import com.muczynski.library.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

@Service
public class AskGrok {

    private static final Logger log = LoggerFactory.getLogger(AskGrok.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private UserSettingsService userSettingsService;

    private final RestTemplate restTemplate;

    /** Test seam. Production waits before a Gemini retry or a rate-limit cooldown. */
    private LongConsumer geminiPause = this::sleepBeforeGeminiRetry;

    /** Test seam. Production reads the wall clock. */
    private LongSupplier geminiNow = System::currentTimeMillis;

    /**
     * Models that returned HTTP 429, with the epoch millis when they may be tried again.
     * A bulk genre run otherwise calls the same exhausted model on every book.
     */
    private final Map<String, Long> geminiLimitedUntil = new ConcurrentHashMap<>();

    public AskGrok() {
        this.restTemplate = new RestTemplate();
        // Configure long timeout for xAI API calls (10 minutes)
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(600000); // 10 minutes
        factory.setReadTimeout(600000); // 10 minutes
        this.restTemplate.setRequestFactory(factory);
    }

    // Model constants.
    // xAI does not currently offer a rolling flagship alias (grok-latest / grok-default 404).
    // grok-4 and grok-4-latest resolve to grok-4.3. Pin the current flagship explicitly.
    public static final String MODEL_GROK_FLAGSHIP = "grok-4.7";
    public static final String MODEL_GROK_4 = MODEL_GROK_FLAGSHIP;
    public static final String MODEL_GROK_4_FAST = "grok-4.3";
    public static final String IMAGE_DETAIL_HIGH = "high";
    /**
     * Preferred free-tier Gemini model. On the Gemini API pricing page (November 2026),
     * Gemini 4 Hydrogen, Gemini 3.8 Flash, and Gemini 3.5 Flash-Lite input and output
     * are free of charge. Gemini 4 Carbon, Argon, and Neon are not on the free tier.
     * Hydrogen accepts text and images. A busy Hydrogen call falls through the free list.
     */
    public static final String MODEL_GEMINI_FREE = "gemini-4-hydrogen";
    static final List<String> GEMINI_FREE_MODELS = List.of(
            MODEL_GEMINI_FREE,
            "gemini-3.8-flash",
            "gemini-3.5-flash-lite");
    public static final String GEMINI_GENERATE_URL = geminiGenerateUrl(MODEL_GEMINI_FREE);
    /** One same-model retry after a busy or dropped call. A 429 is not retried on that model. */
    private static final long GEMINI_RETRY_PAUSE_MS = 1000L;
    /**
     * How long every free model stays uncalled after any model returns HTTP 429.
     * The web request does not sleep for this pause. The caller waits, then tries again.
     */
    static final long GEMINI_RATE_LIMIT_COOLDOWN_MS = 60_000L;
    /** Fast prompts ask for 500 tokens. Thinking shares that budget, so Gemini gets more room. */
    private static final int GEMINI_FAST_OUTPUT_TOKENS = 2048;
    private static final int GEMINI_OUTPUT_TOKEN_CEILING = 8192;
    /** Max books per reading-difficulty Grok prompt. */
    public static final int READING_DIFFICULTY_BATCH_SIZE = 10;

    private static final int CATALOG_MAX_COMPLETION_TOKENS = 8000;
    private static final int FAST_MAX_COMPLETION_TOKENS = 500;

    /**
     * Analyze a single photo using Grok AI vision model with grok-4.3 (the less expensive model).
     * @param imageBytes Photo bytes
     * @param contentType Image content type (e.g., "image/jpeg")
     * @param prompt The analysis prompt/question for the AI
     * @return AI response as String
     */
    public String analyzePhoto(byte[] imageBytes, String contentType, String prompt) {
        return analyzePhoto(imageBytes, contentType, prompt, MODEL_GROK_4_FAST);
    }

    /**
     * Analyze a single photo using Grok AI vision model with specified model.
     * @param imageBytes Photo bytes
     * @param contentType Image content type (e.g., "image/jpeg")
     * @param prompt The analysis prompt/question for the AI
     * @param model The Grok model to use
     * @return AI response as String
     */
    public String analyzePhoto(byte[] imageBytes, String contentType, String prompt, String model) {
        return analyzePhoto(imageBytes, contentType, prompt, model, null, null);
    }

    /**
     * Analyze a single photo using Grok AI vision model with specified model and optional system prompt.
     */
    public String analyzePhoto(byte[] imageBytes, String contentType, String prompt, String model, String systemPrompt) {
        return analyzePhoto(imageBytes, contentType, prompt, model, systemPrompt, null);
    }

    /**
     * Analyze a single photo using Grok AI vision model with specified model, optional system prompt,
     * and optional image detail level ({@code auto}, {@code low}, or {@code high}).
     */
    public String analyzePhoto(byte[] imageBytes, String contentType, String prompt, String model,
                               String systemPrompt, String imageDetail) {
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);

        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        String base64Image = encoder.encodeToString(imageBytes);

        Map<String, Object> imageUrlPart = new HashMap<>();
        imageUrlPart.put("url", "data:" + contentType + ";base64," + base64Image);
        if (imageDetail != null && !imageDetail.isBlank()) {
            imageUrlPart.put("detail", imageDetail);
        }

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrlPart);

        List<Object> content = Arrays.asList(textPart, imagePart);
        return complete(model, chatMessages(systemPrompt, content), CATALOG_MAX_COMPLETION_TOKENS, 0.7);
    }

    /**
     * Analyze multiple photos using Grok AI vision model with grok-4.3 (the less expensive model).
     * @param photoDataList List of maps containing "imageBytes" (byte[]) and "contentType" (String)
     * @param prompt The analysis prompt/question for the AI
     * @return AI response as String
     */
    public String analyzePhotos(List<Map<String, Object>> photoDataList, String prompt) {
        return analyzePhotos(photoDataList, prompt, MODEL_GROK_4_FAST);
    }

    /**
     * Analyze multiple photos using Grok AI vision model with specified model.
     * @param photoDataList List of maps containing "imageBytes" (byte[]) and "contentType" (String)
     * @param prompt The analysis prompt/question for the AI
     * @param model The Grok model to use
     * @return AI response as String
     */
    public String analyzePhotos(List<Map<String, Object>> photoDataList, String prompt, String model) {
        return analyzePhotos(photoDataList, prompt, model, null);
    }

    /**
     * Analyze multiple photos using Grok AI vision model with specified model and optional system prompt.
     */
    public String analyzePhotos(List<Map<String, Object>> photoDataList, String prompt, String model, String systemPrompt) {
        // Build content array: first the text question, then all images
        List<Object> content = new ArrayList<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);
        content.add(textPart);

        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        for (Map<String, Object> photoData : photoDataList) {
            byte[] imageBytes = (byte[]) photoData.get("imageBytes");
            String contentType = (String) photoData.get("contentType");

            if (imageBytes == null || imageBytes.length == 0) {
                continue;
            }

            String base64Image = encoder.encodeToString(imageBytes);

            Map<String, Object> imageUrlPart = new HashMap<>();
            imageUrlPart.put("url", "data:" + contentType + ";base64," + base64Image);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", imageUrlPart);

            content.add(imagePart);
        }

        return complete(model, chatMessages(systemPrompt, content), CATALOG_MAX_COMPLETION_TOKENS, 0.7);
    }

    /**
     * Ask a text-only question to Grok AI (no images).
     * @param question The question to ask
     * @return AI response as String
     */
    public String askQuestion(String question) {
        return askQuestion(question, null);
    }

    /**
     * Ask a text-only question to Grok AI with an optional system prompt.
     */
    public String askQuestion(String question, String systemPrompt) {
        return complete(MODEL_GROK_FLAGSHIP, chatMessages(systemPrompt, question), CATALOG_MAX_COMPLETION_TOKENS, 0.7);
    }

    /**
     * Get Library of Congress call number suggestion for a book
     * @param title Book title
     * @param author Author name
     * @return Suggested LOC call number
     */
    public String suggestLocNumber(String title, String author) {
        String authorLabel = author != null && !author.isEmpty() ? author : "Unknown Author";
        String prompt = String.format(
                "Return a JSON array of Library of Congress Classification call numbers for the book \"%s\" by %s.\n" +
                "Each distinct call number must be its own string. Do not concatenate two numbers into one string.\n" +
                "Sort the array alphabetically. If uncertain, still list each estimate as a separate string.\n" +
                "Respond with only the JSON array, no other text. Example: [\"BX4700.T4\",\"PS3511.I9 G7\"]",
                title,
                authorLabel
        );
        String response = askQuestion(prompt);
        List<String> callNumbers = parseCallNumberArray(response);
        if (callNumbers.isEmpty()) {
            log.warn("Could not parse LOC call number array for \"{}\" by {}: {}", title, authorLabel, response);
            return null;
        }
        log.info("LOC call number array for \"{}\" by {}: {}", title, authorLabel, callNumbers);
        return callNumbers.get(0);
    }

    /**
     * Ask Grok for Grokipedia article URLs for a book, using the author as context.
     * The model must reply with a JSON array of URL strings and nothing else.
     */
    public List<String> suggestGrokipediaUrlsForBook(String title, String authorName) {
        String asciiTitle = toAscii(title);
        String asciiAuthor = toAscii(authorName);
        String goalSubject = asciiAuthor != null && !asciiAuthor.isBlank()
                ? "\"" + asciiTitle + "\" by " + asciiAuthor
                : "\"" + asciiTitle + "\"";
        String prompt = """
                This task is about authors and books.

                Goal: find Grokipedia article URL(s) for the book %s.

                Procedure:
                1. Treat Grokipedia search as the source of truth.
                   Search URL: https://grokipedia.com/search?q=%s
                2. Prefer the primary article about the book, not an author article, film, bibliography, course, or compilation unless no book page exists.
                3. Grokipedia slugs are inconsistent. Common patterns:
                   - The_Song_of_Bernadette_(novel)
                   - The_Song_of_Bernadette
                   - Song_of_Bernadette
                   - Title_With_Underscores
                   - Title_(novel) or (book) or (film)
                4. If you cannot verify a live page, return the Grokipedia search URL plus the best candidate page URLs rather than inventing a single slug.
                5. Do not invent Wikipedia-style slugs unless they also appear in Grokipedia search results.

                Respond in JSON only, with nothing before or after the JSON.
                Return a JSON array of URL strings, most relevant first.
                Example: ["https://grokipedia.com/page/The_Song_of_Bernadette_(novel)"]
                """.formatted(goalSubject, asciiTitle);
        String response = askQuestionFast(prompt);
        List<String> urls = parseJsonStringArray(response);
        if (urls.isEmpty()) {
            log.warn("Could not parse Grokipedia URL array for book {}: {}", goalSubject, response);
        }
        return urls;
    }

    /**
     * Ask Grok for Grokipedia article URLs for an author, using a book as context.
     * The model must reply with a JSON array of URL strings and nothing else.
     */
    public List<String> suggestGrokipediaUrlsForAuthor(String authorName, String bookTitle) {
        String asciiAuthor = toAscii(authorName);
        String asciiBook = toAscii(bookTitle);
        String goalSubject = asciiBook != null && !asciiBook.isBlank()
                ? asciiAuthor + ", known for the book \"" + asciiBook + "\""
                : asciiAuthor;
        String prompt = """
                This task is about authors and books.

                Goal: find Grokipedia article URL(s) for the author %s.

                Procedure:
                1. Treat Grokipedia search as the source of truth.
                   Search URL: https://grokipedia.com/search?q=%s
                2. Prefer the primary biographical article about the person, not a book article, bibliography, course, or compilation unless no person page exists.
                3. Grokipedia slugs are inconsistent. Common patterns:
                   - C._S._Lewis
                   - CS_Lewis
                   - C_S_Lewis
                   - First_Last
                   - First_Last_(writer) or (author) or (book)
                4. If you cannot verify a live page, return the Grokipedia search URL plus the best candidate page URLs rather than inventing a single slug.
                5. Do not invent Wikipedia-style slugs unless they also appear in Grokipedia search results.

                Respond in JSON only, with nothing before or after the JSON.
                Return a JSON array of URL strings, most relevant first.
                Example: ["https://grokipedia.com/page/C._S._Lewis"]
                """.formatted(goalSubject, asciiAuthor);
        String response = askQuestionFast(prompt);
        List<String> urls = parseJsonStringArray(response);
        if (urls.isEmpty()) {
            log.warn("Could not parse Grokipedia URL array for author {}: {}", authorName, response);
        }
        return urls;
    }

    /**
     * Strip diacritics and fold Latin letters to ASCII, e.g. {@code Karol Józef Wojtyła}
     * becomes {@code Karol Jozef Wojtyla}. Null is returned unchanged.
     */
    static String toAscii(String input) {
        if (input == null) {
            return null;
        }
        String folded = Normalizer.normalize(input, Normalizer.Form.NFKD)
                .replace("ł", "l").replace("Ł", "L")
                .replace("ø", "o").replace("Ø", "O")
                .replace("đ", "d").replace("Đ", "D")
                .replace("æ", "ae").replace("Æ", "AE")
                .replace("œ", "oe").replace("Œ", "OE")
                .replace("ß", "ss")
                .replace("ð", "d").replace("Ð", "D")
                .replace("þ", "th").replace("Þ", "Th")
                .replace("ı", "i").replace("İ", "I")
                .replaceAll("\\p{M}+", "");
        StringBuilder ascii = new StringBuilder(folded.length());
        for (int i = 0; i < folded.length(); i++) {
            char c = folded.charAt(i);
            if (c <= 0x7F) {
                ascii.append(c);
            }
        }
        return ascii.toString();
    }

    /**
     * Parses a JSON array of strings from a model reply, ignoring blank entries.
     * Does not sort; callers that need a specific order must sort themselves.
     */
    static List<String> parseJsonStringArray(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        String trimmed = response.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return List.of();
        }
        try {
            List<String> parsed = OBJECT_MAPPER.readValue(
                    trimmed.substring(start, end + 1),
                    new TypeReference<List<String>>() {});
            List<String> cleaned = new ArrayList<>();
            if (parsed != null) {
                for (String item : parsed) {
                    if (item != null && !item.isBlank()) {
                        cleaned.add(item.trim());
                    }
                }
            }
            return cleaned;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Parses a JSON array of call-number strings from a model reply and sorts
     * them alphabetically, ignoring blank entries.
     */
    static List<String> parseCallNumberArray(String response) {
        List<String> cleaned = new ArrayList<>(parseJsonStringArray(response));
        cleaned.sort(String.CASE_INSENSITIVE_ORDER);
        return cleaned;
    }

    /**
     * Classify reading difficulty for up to {@link #READING_DIFFICULTY_BATCH_SIZE} books.
     * The model must reply with a JSON array of candidate-key arrays (most appropriate first);
     * this method returns the first key of each inner array, aligned to the input order.
     *
     * @param bookJsons JSON object strings, one per book (id, title, author, etc.)
     * @return one suggested key per book; null when that book had no usable answer
     */
    public List<String> suggestReadingDifficulties(List<String> bookJsons) {
        if (bookJsons == null || bookJsons.isEmpty()) {
            return List.of();
        }
        if (bookJsons.size() > READING_DIFFICULTY_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "At most " + READING_DIFFICULTY_BATCH_SIZE + " books per reading-difficulty prompt");
        }

        StringBuilder levelLines = new StringBuilder();
        for (ReadingDifficulty rd : ReadingDifficulty.values()) {
            if (rd == ReadingDifficulty.UNSET) {
                continue;
            }
            levelLines.append("- ").append(rd.getKey()).append(": ").append(rd.getDescription()).append('\n');
        }

        String prompt = """
                Classify the reading difficulty of each book.

                Valid keys (use only these; do not use unset):
                %s
                For each book, return an array of applicable keys, most appropriate first.
                Respond with only a JSON array of those arrays, in the same order as the books, nothing else.
                Example: [["children"],["moderate","accessible"],["demanding","advanced"]]

                Books:
                [%s]
                """.formatted(levelLines, String.join(",\n", bookJsons));

        String response = askQuestionFast(prompt);
        List<List<String>> parsed = parseJsonStringArrayOfArrays(response);
        List<String> firsts = new ArrayList<>(bookJsons.size());
        for (int i = 0; i < bookJsons.size(); i++) {
            if (i < parsed.size() && !parsed.get(i).isEmpty()) {
                firsts.add(parsed.get(i).get(0));
            } else {
                firsts.add(null);
            }
        }
        if (parsed.isEmpty()) {
            log.warn("Could not parse reading-difficulty array for {} book(s): {}", bookJsons.size(), response);
        }
        return firsts;
    }

    /**
     * Parses a JSON array whose elements are either strings or arrays of strings.
     * Textual elements are wrapped as a one-element list so a flat array of answers
     * still works. Blank entries are dropped. Invalid JSON returns an empty list.
     */
    static List<List<String>> parseJsonStringArrayOfArrays(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        String trimmed = response.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return List.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(trimmed.substring(start, end + 1));
            if (root == null || !root.isArray()) {
                return List.of();
            }
            List<List<String>> result = new ArrayList<>();
            for (JsonNode item : root) {
                if (item == null || item.isNull()) {
                    result.add(List.of());
                } else if (item.isTextual()) {
                    String text = item.asText();
                    result.add(text == null || text.isBlank() ? List.of() : List.of(text.trim()));
                } else if (item.isArray()) {
                    List<String> inner = new ArrayList<>();
                    for (JsonNode el : item) {
                        if (el != null && el.isTextual() && !el.asText().isBlank()) {
                            inner.add(el.asText().trim());
                        }
                    }
                    result.add(inner);
                } else {
                    result.add(List.of());
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Suggest genres/tags for a book based on its catalog card information.
     * Uses grok-4.3 (the less expensive model) for faster responses.
     *
     * @param bookJson JSON string containing book information
     * @param authorJson JSON string containing author information (can be null)
     * @return Comma-separated list of suggested genre tags
     */
    public String suggestGenres(String bookJson, String authorJson) {
        String allowedTags = String.join(", ", BookLabels.ALL_LABELS);
        String prompt = String.format(
                "Based on the following book and author information from a library card catalog, " +
                "suggest genre/category tags for this book. " +
                "Use ONLY these predefined tags where applicable: %s. " +
                "Note: science-fiction should be categorized under 'fantasy'. " +
                "Return ONLY a comma-separated list of applicable tags, nothing else. " +
                "For example: fiction, fantasy, childrens\nIf hagiography is used, don't use saint nor biography nor history. Classic should only be used for classic books as in classic literature. Use discernment for the discernment of spirits in the sense of Ignatius. Use read-aloud for books meant to be read aloud to children, and childrens for books a child reads on their own.\n" +
                "Book Information:\n%s\n\n" +
                "%s",
                allowedTags,
                bookJson,
                authorJson != null && !authorJson.isBlank() ? "Author Information:\n" + authorJson : ""
        );
        return askQuestionFast(prompt);
    }

    /**
     * Ask a text-only question to Grok AI using grok-4.3 (the less expensive model).
     * @param question The question to ask
     * @return AI response as String
     */
    public String askQuestionFast(String question) {
        return complete(MODEL_GROK_4_FAST, chatMessages(null, question), FAST_MAX_COMPLETION_TOKENS, 0.3);
    }

    private List<Map<String, Object>> chatMessages(String systemPrompt, Object userContent) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, Object> system = new HashMap<>();
            system.put("role", "system");
            system.put("content", systemPrompt);
            messages.add(system);
        }
        Map<String, Object> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", userContent);
        messages.add(user);
        return messages;
    }

    private enum AiProvider {
        XAI, GEMINI
    }

    private record AiCall(AiProvider provider, String apiKey) {}

    private AiCall resolveCall() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LibraryException("No authenticated user found");
        }
        Long userId = Long.parseLong(authentication.getName());
        UserDto userDto = userSettingsService.getUserSettings(userId);
        boolean useXai = userDto.isUseXaiApiKey();
        boolean useGoogle = userDto.isUseGoogleApiKey();
        if (useXai && useGoogle) {
            throw new LibraryException("Enable only one AI provider in Settings");
        }
        if (useGoogle) {
            String apiKey = userDto.getGoogleApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new LibraryException("Google API key not configured. Add it in Settings.");
            }
            return new AiCall(AiProvider.GEMINI, apiKey.trim());
        }
        if (useXai) {
            String apiKey = userDto.getXaiApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new LibraryException("xAI API key not configured for user ID: " + userId);
            }
            return new AiCall(AiProvider.XAI, apiKey.trim());
        }
        throw new LibraryException("No AI provider is enabled. Turn on the xAI or Google API key in Settings.");
    }

    /**
     * True when the provider rejected the credential itself: HTTP 401, or a body that
     * names an invalid API key. A spending-limit 403 is not an invalid key.
     */
    static boolean isInvalidApiKey(int status, String body) {
        if (status == 401) {
            return true;
        }
        if (body == null || body.isBlank()) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("api_key_invalid")
                || lower.contains("api key not valid")
                || lower.contains("invalid api key")
                || (lower.contains("api key") && (lower.contains("invalid") || lower.contains("not valid")));
    }

    /**
     * True when an xAI error means the team is out of credits or hit its spending limit:
     * HTTP 402, or HTTP 403/429 whose body mentions credits or a spending limit.
     * A plain 429 rate limit (no credits wording) is not treated as out of credits.
     */
    static boolean isCreditsExhausted(int status, String body) {
        if (status == 402) {
            return true;
        }
        if (status != 403 && status != 429) {
            return false;
        }
        if (body == null || body.isBlank()) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("credit")
                || lower.contains("spending limit")
                || lower.contains("spending-limit")
                || lower.contains("spending_limit");
    }

    private String complete(String model, List<Map<String, Object>> messages, int maxCompletionTokens, double temperature) {
        AiCall call = resolveCall();
        if (call.provider == AiProvider.GEMINI) {
            return completeGemini(call.apiKey, messages, maxCompletionTokens);
        }
        return completeXai(call.apiKey, model, messages, maxCompletionTokens, temperature);
    }

    private String completeXai(String apiKey, String model, List<Map<String, Object>> messages,
                               int maxCompletionTokens, double temperature) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("messages", messages);
        request.put("max_completion_tokens", maxCompletionTokens);
        request.put("temperature", temperature);
        request.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(
                    "https://api.x.ai/v1/chat/completions",
                    entity,
                    Map.class
            );
        } catch (HttpClientErrorException e) {
            if (isCreditsExhausted(e.getStatusCode().value(), e.getResponseBodyAsString())) {
                log.warn("xAI refused the Grok call: out of credits or spending limit reached (HTTP {})",
                        e.getStatusCode().value());
                throw new GrokCreditsExhaustedException(e);
            }
            if (isInvalidApiKey(e.getStatusCode().value(), e.getResponseBodyAsString())) {
                throw new LibraryException("xAI API key is invalid", e);
            }
            throw e;
        }

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("choices")) {
                List<Map> choices = (List<Map>) body.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    if (choice.containsKey("message")) {
                        Map<String, Object> messageResponse = (Map<String, Object>) choice.get("message");
                        return (String) messageResponse.get("content");
                    }
                }
            }
            throw new LibraryException("Unexpected response format from xAI API");
        } else {
            throw new LibraryException("xAI API call failed: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    /**
     * Gemini 4 Hydrogen ignores custom temperature, and thinkingBudget is rejected.
     * Thinking tokens count against maxOutputTokens, so every call uses thinking level
     * low and a budget large enough for the answer. A 503 or an empty thought-only
     * reply is retried once, then the next free-tier model is tried. A 429 stops the
     * call immediately. No other model is called until {@link #GEMINI_RATE_LIMIT_COOLDOWN_MS}
     * has passed, and this request does not sleep for that pause. The message returned
     * to the catalog stays short and does not include the request URL.
     */
    private String completeGemini(String apiKey, List<Map<String, Object>> messages, int maxCompletionTokens) {
        int outputTokens = geminiOutputBudget(maxCompletionTokens);
        long now = geminiNow.getAsLong();
        for (String model : GEMINI_FREE_MODELS) {
            if (geminiLimitedUntil.getOrDefault(model, 0L) > now) {
                throw new LibraryException("Gemini rate limit reached. Try again later.");
            }
        }
        GeminiOutcome last = null;
        for (String model : GEMINI_FREE_MODELS) {
            GeminiOutcome outcome = postGemini(apiKey, messages, model, outputTokens, true);
            outcome = recoverGemini(apiKey, messages, model, outputTokens, outcome);
            if (outcome.text != null) {
                return outcome.text;
            }
            if (outcome.fatal != null) {
                throw outcome.fatal;
            }
            if (outcome.failure == GeminiFailure.RATE_LIMIT) {
                long resumeAt = geminiNow.getAsLong() + GEMINI_RATE_LIMIT_COOLDOWN_MS;
                geminiLimitedUntil.merge(model, resumeAt, Math::max);
                throw new LibraryException("Gemini rate limit reached. Try again later.");
            }
            if (outcome.failure != GeminiFailure.SKIP) {
                last = outcome;
            }
        }
        if (last == null) {
            throw new LibraryException("Gemini rejected the request (404).");
        }
        throw terminalGeminiFailure(last);
    }

    private GeminiOutcome recoverGemini(String apiKey, List<Map<String, Object>> messages, String model,
                                        int outputTokens, GeminiOutcome outcome) {
        if (outcome.text != null || outcome.fatal != null || outcome.failure == null) {
            return outcome;
        }
        if (outcome.failure == GeminiFailure.BUSY || outcome.failure == GeminiFailure.IO) {
            pauseBeforeGeminiRetry();
            return postGemini(apiKey, messages, model, outputTokens, true);
        }
        if (outcome.failure == GeminiFailure.EMPTY) {
            int raised = geminiRaisedBudget(outputTokens);
            if (raised > outputTokens) {
                return postGemini(apiKey, messages, model, raised, true);
            }
        }
        if (outcome.failure == GeminiFailure.THINKING_REJECTED) {
            return postGemini(apiKey, messages, model, outputTokens, false);
        }
        return outcome;
    }

    private void pauseBeforeGeminiRetry() {
        geminiPause.accept(GEMINI_RETRY_PAUSE_MS);
    }

    private void sleepBeforeGeminiRetry(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LibraryException("Gemini request was interrupted", e);
        }
    }

    @SuppressWarnings("unchecked")
    private GeminiOutcome postGemini(String apiKey, List<Map<String, Object>> messages, String model,
                                     int maxOutputTokens, boolean includeThinking) {
        Map<String, Object> request = geminiRequest(messages, maxOutputTokens, includeThinking);
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-goog-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(geminiGenerateUrl(model), entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return parseGeminiBody(model, response.getBody());
            }
            String body = response.getBody() == null ? "" : response.getBody().toString();
            return classifyGeminiStatus(model, response.getStatusCode().value(), body, includeThinking, null);
        } catch (RestClientResponseException e) {
            return classifyGeminiStatus(model, e.getStatusCode().value(), e.getResponseBodyAsString(), includeThinking, e);
        } catch (ResourceAccessException e) {
            log.warn("Gemini {} request failed ({})", model, e.getClass().getSimpleName());
            return GeminiOutcome.failure(GeminiFailure.IO, null);
        }
    }

    private GeminiOutcome classifyGeminiStatus(String model, int status, String body, boolean includeThinking,
                                               RestClientResponseException cause) {
        if (isInvalidApiKey(status, body)) {
            log.warn("Gemini rejected the API key (HTTP {})", status);
            return GeminiOutcome.fatal(new LibraryException("Google API key is invalid", cause));
        }
        if (status == 429) {
            log.warn("Gemini {} returned HTTP 429", model);
            return GeminiOutcome.failure(GeminiFailure.RATE_LIMIT, null);
        }
        if (status == 500 || status == 502 || status == 503 || status == 504) {
            log.warn("Gemini {} returned HTTP {}", model, status);
            return GeminiOutcome.failure(GeminiFailure.BUSY, null);
        }
        if (status == 404) {
            log.warn("Gemini {} returned HTTP 404", model);
            return GeminiOutcome.failure(GeminiFailure.SKIP, null);
        }
        if (status == 400 && includeThinking && isThinkingConfigRejected(body)) {
            log.warn("Gemini {} rejected thinkingConfig", model);
            return GeminiOutcome.failure(GeminiFailure.THINKING_REJECTED, null);
        }
        log.warn("Gemini {} returned HTTP {}", model, status);
        String message = "Gemini rejected the request (" + status + ").";
        LibraryException fatal = cause == null
                ? new LibraryException(message)
                : new LibraryException(message, cause);
        return GeminiOutcome.fatal(fatal);
    }

    private GeminiOutcome parseGeminiBody(String model, Map<String, Object> body) {
        String block = geminiBlockReason(body);
        if (block != null) {
            String token = safeGeminiToken(block);
            log.warn("Gemini {} blocked the prompt ({})", model, token);
            return GeminiOutcome.fatal(new LibraryException("Gemini blocked the prompt (" + token + ")."));
        }
        String finish = geminiFinishReason(body);
        if (finish != null && "SAFETY".equalsIgnoreCase(finish.trim())) {
            log.warn("Gemini {} blocked the prompt (SAFETY)", model);
            return GeminiOutcome.fatal(new LibraryException("Gemini blocked the prompt (SAFETY)."));
        }
        String text = body == null ? null : geminiText(body);
        if (text != null) {
            return GeminiOutcome.text(text);
        }
        String reason = safeGeminiToken(finish);
        log.warn("Gemini {} returned no answer (finishReason={}, parts={})", model, reason, geminiPartCount(body));
        return GeminiOutcome.failure(GeminiFailure.EMPTY, reason);
    }

    private static LibraryException terminalGeminiFailure(GeminiOutcome last) {
        if (last.failure == GeminiFailure.RATE_LIMIT) {
            return new LibraryException("Gemini rate limit reached. Try again later.");
        }
        if (last.failure == GeminiFailure.EMPTY) {
            return new LibraryException("Gemini returned no answer (" + safeGeminiToken(last.finishReason) + ").");
        }
        return new LibraryException("Gemini is busy right now. Try again later.");
    }

    static String geminiGenerateUrl(String model) {
        return "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
    }

    /** Fast calls (500 requested) get 2048 so low thinking and the answer both fit. Catalog calls keep their budget. */
    static int geminiOutputBudget(int requested) {
        return requested <= FAST_MAX_COMPLETION_TOKENS ? GEMINI_FAST_OUTPUT_TOKENS : requested;
    }

    /** One larger budget after a thought-only MAX_TOKENS reply. Doubled, at least 4096, and at most 8192. */
    static int geminiRaisedBudget(int current) {
        long raised = Math.max((long) current * 2, 4096L);
        return (int) Math.min(raised, GEMINI_OUTPUT_TOKEN_CEILING);
    }

    static boolean isThinkingConfigRejected(String body) {
        return body != null && body.toLowerCase(Locale.ROOT).contains("thinking");
    }

    private static String safeGeminiToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "EMPTY";
        }
        String token = raw.trim();
        if (token.length() > 40 || !token.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '_')) {
            return "EMPTY";
        }
        return token;
    }

    private Map<String, Object> geminiRequest(List<Map<String, Object>> messages, int maxOutputTokens,
                                              boolean includeThinking) {
        String systemPrompt = null;
        Object userContent = null;
        for (Map<String, Object> message : messages) {
            String role = String.valueOf(message.get("role"));
            if ("system".equals(role)) {
                Object content = message.get("content");
                if (content != null) {
                    systemPrompt = content.toString();
                }
            } else if ("user".equals(role)) {
                userContent = message.get("content");
            }
        }

        Map<String, Object> request = new HashMap<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, Object> system = new HashMap<>();
            system.put("parts", List.of(Map.of("text", systemPrompt)));
            request.put("systemInstruction", system);
        }
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", geminiParts(userContent));
        request.put("contents", List.of(content));

        Map<String, Object> generation = new HashMap<>();
        generation.put("maxOutputTokens", maxOutputTokens);
        if (includeThinking) {
            generation.put("thinkingConfig", Map.of("thinkingLevel", "low"));
        }
        request.put("generationConfig", generation);
        return request;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> geminiParts(Object userContent) {
        if (userContent instanceof String text) {
            return List.of(Map.of("text", text));
        }
        if (!(userContent instanceof List<?> list)) {
            throw new LibraryException("Unsupported prompt for Gemini");
        }
        List<Map<String, Object>> parts = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> part = (Map<String, Object>) raw;
            String type = String.valueOf(part.get("type"));
            if ("text".equals(type)) {
                parts.add(Map.of("text", String.valueOf(part.get("text"))));
            } else if ("image_url".equals(type)) {
                Object imageUrl = part.get("image_url");
                if (imageUrl instanceof Map<?, ?> image) {
                    parts.add(inlineImage(String.valueOf(image.get("url"))));
                }
            }
        }
        if (parts.isEmpty()) {
            throw new LibraryException("Gemini prompt has no content");
        }
        return parts;
    }

    private static Map<String, Object> inlineImage(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:") || !dataUrl.contains(",")) {
            throw new LibraryException("Photo data is not a data URL");
        }
        int comma = dataUrl.indexOf(',');
        String meta = dataUrl.substring("data:".length(), comma);
        int semi = meta.indexOf(';');
        String mime = semi >= 0 ? meta.substring(0, semi) : meta;
        Map<String, Object> inline = new HashMap<>();
        inline.put("mimeType", mime);
        inline.put("data", dataUrl.substring(comma + 1));
        Map<String, Object> part = new HashMap<>();
        part.put("inlineData", inline);
        return part;
    }

    /**
     * Joins answer text and skips thinking parts. Returns null when the body has no answer.
     */
    @SuppressWarnings("unchecked")
    static String geminiText(Map<String, Object> body) {
        Object candidates = body.get("candidates");
        if (!(candidates instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> candidate)) {
            return null;
        }
        Object content = candidate.get("content");
        if (!(content instanceof Map<?, ?> contentMap)) {
            return null;
        }
        Object parts = contentMap.get("parts");
        if (!(parts instanceof List<?> partList)) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (Object partObj : partList) {
            if (!(partObj instanceof Map<?, ?> part)) {
                continue;
            }
            if (Boolean.TRUE.equals(part.get("thought"))) {
                continue;
            }
            Object value = part.get("text");
            if (value != null) {
                text.append(value);
            }
        }
        return text.isEmpty() ? null : text.toString();
    }

    static String geminiFinishReason(Map<String, Object> body) {
        Map<?, ?> candidate = firstGeminiCandidate(body);
        if (candidate == null) {
            return null;
        }
        Object reason = candidate.get("finishReason");
        return reason == null ? null : reason.toString();
    }

    static String geminiBlockReason(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object feedback = body.get("promptFeedback");
        if (!(feedback instanceof Map<?, ?> map)) {
            return null;
        }
        Object reason = map.get("blockReason");
        if (reason == null || reason.toString().isBlank()) {
            return null;
        }
        return reason.toString();
    }

    static int geminiPartCount(Map<String, Object> body) {
        Map<?, ?> candidate = firstGeminiCandidate(body);
        if (candidate == null) {
            return 0;
        }
        Object content = candidate.get("content");
        if (!(content instanceof Map<?, ?> contentMap)) {
            return 0;
        }
        Object parts = contentMap.get("parts");
        return parts instanceof List<?> partList ? partList.size() : 0;
    }

    private static Map<?, ?> firstGeminiCandidate(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object candidates = body.get("candidates");
        if (!(candidates instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> candidate)) {
            return null;
        }
        return candidate;
    }

    private enum GeminiFailure {
        BUSY, RATE_LIMIT, IO, EMPTY, THINKING_REJECTED, SKIP
    }

    private static final class GeminiOutcome {
        private final String text;
        private final LibraryException fatal;
        private final GeminiFailure failure;
        private final String finishReason;

        private GeminiOutcome(String text, LibraryException fatal, GeminiFailure failure, String finishReason) {
            this.text = text;
            this.fatal = fatal;
            this.failure = failure;
            this.finishReason = finishReason;
        }

        static GeminiOutcome text(String text) {
            return new GeminiOutcome(text, null, null, null);
        }

        static GeminiOutcome fatal(LibraryException fatal) {
            return new GeminiOutcome(null, fatal, null, null);
        }

        static GeminiOutcome failure(GeminiFailure failure, String finishReason) {
            return new GeminiOutcome(null, null, failure, finishReason);
        }
    }
}
