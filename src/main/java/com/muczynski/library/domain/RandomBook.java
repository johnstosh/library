package com.muczynski.library.domain;

import java.util.List;
import java.util.Random;

public class RandomBook {

    private static final List<String> COLORS = List.of(
            "Red", "Green", "Blue", "Yellow", "Black",
            "White", "Orange", "Purple", "Brown", "Gray"
    );

    private static final List<String> NOUNS = List.of(
            "Dog", "Cat", "House", "Car", "Tree",
            "Book", "River", "Mountain", "City", "Sun"
    );

    private static final List<String> VERBS = List.of(
            "Runs", "Jumps", "Sleeps", "Eats", "Flies",
            "Swims", "Walks", "Talks", "Sings", "Dances"
    );

    private static final Random RANDOM = new Random();

    private final String title;

    public RandomBook() {
        this.title = generateRandomTitle();
    }

    private String generateRandomTitle() {
        String color = COLORS.get(RANDOM.nextInt(COLORS.size()));
        String noun = NOUNS.get(RANDOM.nextInt(NOUNS.size()));
        String verb = VERBS.get(RANDOM.nextInt(VERBS.size()));
        return "The " + color + " " + noun + " " + verb;
    }

    public String getTitle() {
        return title;
    }
}