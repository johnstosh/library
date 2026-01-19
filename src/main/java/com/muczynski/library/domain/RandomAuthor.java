/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class RandomAuthor {

    private static final List<String> FIRST_NAMES = List.of(
            "James", "Mary", "John", "Patricia", "Robert",
            "Jennifer", "Michael", "Linda", "William", "Elizabeth"
    );

    private static final List<String> LAST_NAMES = List.of(
            "Smith", "Johnson", "Williams", "Brown", "Jones",
            "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"
    );

    private static final Random RANDOM = new Random();

    public Author create() {
        Author author = new Author();
        author.setName(generateRandomName());
        author.setReligiousAffiliation("test-data");
        author.setGrokipediaUrl("https://grokipedia.example.com/author/" + RANDOM.nextInt(1000));
        return author;
    }

    private String generateRandomName() {
        String firstName = FIRST_NAMES.get(RANDOM.nextInt(FIRST_NAMES.size()));
        String lastName = LAST_NAMES.get(RANDOM.nextInt(LAST_NAMES.size()));
        return firstName + " " + lastName;
    }
}