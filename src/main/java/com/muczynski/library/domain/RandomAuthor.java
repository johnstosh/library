package com.muczynski.library.domain;

import java.util.List;
import java.util.Random;

public class RandomAuthor {

    private static final List<String> FIRST_NAMES = List.of(
            "James", "John", "Robert", "Michael", "William",
            "David", "Richard", "Joseph", "Thomas", "Charles"
    );

    private static final List<String> MIDDLE_NAMES = List.of(
            "Lee", "Allen", "Ray", "Wayne", "Eugene",
            "Roy", "Dean", "Lynn", "Dale", "Paul"
    );

    private static final List<String> LAST_NAMES = List.of(
            "Smith", "Johnson", "Williams", "Brown", "Jones",
            "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"
    );

    private static final Random RANDOM = new Random();

    public Author create() {
        Author author = new Author();
        author.setName(generateRandomName());
        return author;
    }

    private String generateRandomName() {
        String firstName = FIRST_NAMES.get(RANDOM.nextInt(FIRST_NAMES.size()));
        String middleName = MIDDLE_NAMES.get(RANDOM.nextInt(MIDDLE_NAMES.size()));
        String lastName = LAST_NAMES.get(RANDOM.nextInt(LAST_NAMES.size()));
        return firstName + " " + middleName + " " + lastName;
    }
}