/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue 390: every controller mapping has a heading in docs/api.
 * The heading line is the HTTP method, a space, and the full path.
 */
class ApiDocumentationCoverageTest {

    private static final Pattern MAPPING = Pattern.compile(
            "@(Get|Post|Put|Delete|Patch)Mapping(\\((?:[^()\"]|\"(?:[^\"\\\\]|\\\\.)*\"|\\((?:[^()\"]|\"(?:[^\"\\\\]|\\\\.)*\")*\\))*\\))?");

    @Test
    void everyControllerMappingHasAHeading() throws IOException {
        Path root = repoRoot();
        Path controllers = root.resolve("src/main/java/com/muczynski/library/controller");
        Path docs = root.resolve("docs/api");
        List<String> mappings = new ArrayList<>();
        try (Stream<Path> files = Files.list(controllers)) {
            files.filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            mappings.addAll(mappingsIn(Files.readString(path)));
                        } catch (IOException e) {
                            throw new IllegalStateException(path.toString(), e);
                        }
                    });
        }
        StringBuilder documented = new StringBuilder();
        try (Stream<Path> files = Files.list(docs)) {
            files.filter(path -> path.getFileName().toString().endsWith(".md"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            documented.append('\n').append(Files.readString(path));
                        } catch (IOException e) {
                            throw new IllegalStateException(path.toString(), e);
                        }
                    });
        }
        Set<String> headings = new LinkedHashSet<>();
        for (String line : documented.toString().split("\n")) {
            if (line.startsWith("## ")) {
                headings.add(line.substring(3).trim());
            }
        }
        List<String> missing = mappings.stream()
                .filter(mapping -> !headings.contains(mapping))
                .distinct()
                .sorted()
                .toList();
        assertTrue(missing.isEmpty(),
                "Add a docs/api heading for each mapping (## METHOD /path):\n" + String.join("\n", missing));
    }

    static List<String> mappingsIn(String source) {
        String stripped = stripComments(source);
        int classAt = stripped.indexOf(" class ");
        if (classAt < 0) {
            classAt = stripped.indexOf("\nclass ");
        }
        String header = classAt < 0 ? "" : stripped.substring(0, classAt);
        String base = classBasePath(header);
        String body = classAt < 0 ? stripped : stripped.substring(classAt);
        List<String> mappings = new ArrayList<>();
        Matcher matcher = MAPPING.matcher(body);
        while (matcher.find()) {
            String http = switch (matcher.group(1)) {
                case "Get" -> "GET";
                case "Post" -> "POST";
                case "Put" -> "PUT";
                case "Delete" -> "DELETE";
                case "Patch" -> "PATCH";
                default -> matcher.group(1).toUpperCase();
            };
            for (String path : annotationPaths(matcher.group(2))) {
                mappings.add(http + " " + joinPath(base, path));
            }
        }
        mappings.sort(Comparator.naturalOrder());
        return mappings;
    }

    private static String classBasePath(String header) {
        Matcher matcher = Pattern.compile("@RequestMapping(\\((?:[^()\"]|\"(?:[^\"\\\\]|\\\\.)*\"|\\((?:[^()\"]|\"(?:[^\"\\\\]|\\\\.)*\")*\\))*\\))?").matcher(header);
        String base = "";
        while (matcher.find()) {
            List<String> paths = annotationPaths(matcher.group(1));
            if (!paths.isEmpty()) {
                base = paths.get(0);
            }
        }
        return base;
    }

    private static List<String> annotationPaths(String args) {
        if (args == null || args.length() < 2) {
            return List.of("");
        }
        String body = args.substring(1, args.length() - 1).trim();
        if (body.isEmpty()) {
            return List.of("");
        }
        Matcher assigned = Pattern.compile("\\b(?:value|path)\\s*=\\s*(\\{.*?}|\"(?:[^\"\\\\]|\\\\.)*\")", Pattern.DOTALL).matcher(body);
        if (assigned.find()) {
            List<String> found = stringsIn(assigned.group(1));
            return found.isEmpty() ? List.of("") : found;
        }
        String stripped = body.stripLeading();
        if (stripped.startsWith("{")) {
            int end = body.indexOf('}');
            List<String> found = stringsIn(end < 0 ? body : body.substring(0, end + 1));
            return found.isEmpty() ? List.of("") : found;
        }
        if (stripped.startsWith("\"")) {
            int comma = indexOfTopLevelComma(body);
            List<String> found = stringsIn(comma < 0 ? body : body.substring(0, comma));
            return found.isEmpty() ? List.of("") : found;
        }
        return List.of("");
    }

    private static int indexOfTopLevelComma(String body) {
        boolean inString = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (c == ',' && !inString) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> stringsIn(String text) {
        List<String> found = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    private static String joinPath(String base, String sub) {
        if (sub == null || sub.isEmpty()) {
            return base == null || base.isEmpty() ? "/" : base;
        }
        if (base == null || base.isEmpty()) {
            return sub;
        }
        if ("/".equals(base)) {
            return sub.startsWith("/") ? sub : "/" + sub;
        }
        if (sub.startsWith("/")) {
            return base.endsWith("/") ? base.substring(0, base.length() - 1) + sub : base + sub;
        }
        return base.endsWith("/") ? base + sub : base + "/" + sub;
    }

    static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        String state = "code";
        while (i < n) {
            char c = source.charAt(i);
            char next = i + 1 < n ? source.charAt(i + 1) : '\0';
            switch (state) {
                case "code" -> {
                    if (c == '/' && next == '/') {
                        state = "line";
                        out.append("  ");
                        i += 2;
                    } else if (c == '/' && next == '*') {
                        state = "block";
                        out.append("  ");
                        i += 2;
                    } else if (c == '"') {
                        state = "str";
                        out.append(c);
                        i++;
                    } else if (c == '\'') {
                        state = "char";
                        out.append(c);
                        i++;
                    } else {
                        out.append(c);
                        i++;
                    }
                }
                case "line" -> {
                    if (c == '\n') {
                        state = "code";
                        out.append(c);
                    } else {
                        out.append(' ');
                    }
                    i++;
                }
                case "block" -> {
                    if (c == '*' && next == '/') {
                        state = "code";
                        out.append("  ");
                        i += 2;
                    } else {
                        out.append(c == '\n' ? '\n' : ' ');
                        i++;
                    }
                }
                case "str" -> {
                    out.append(c);
                    if (c == '\\' && i + 1 < n) {
                        out.append(source.charAt(i + 1));
                        i += 2;
                    } else {
                        if (c == '"') {
                            state = "code";
                        }
                        i++;
                    }
                }
                default -> {
                    out.append(c);
                    if (c == '\\' && i + 1 < n) {
                        out.append(source.charAt(i + 1));
                        i += 2;
                    } else {
                        if (c == '\'') {
                            state = "code";
                        }
                        i++;
                    }
                }
            }
        }
        return out.toString();
    }

    private static Path repoRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("Could not find settings.gradle from the working directory");
        }
        return dir;
    }
}
