/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
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

    private static final List<String> COUNTRIES = List.of(
            "United States", "United Kingdom", "France", "Germany", "Italy",
            "Spain", "Ireland", "Poland", "Mexico", "Canada"
    );

    private static final List<String> NATIONALITIES = List.of(
            "American", "British", "French", "German", "Italian",
            "Spanish", "Irish", "Polish", "Mexican", "Canadian"
    );

    private static final List<String> RELIGIONS = List.of(
            "Catholic", "Protestant", "Orthodox", "Jewish", "Muslim",
            "Buddhist", "Hindu", "Agnostic", "Atheist", "Unknown"
    );

    private static final Random RANDOM = new Random();

    public Author create() {
        Author author = new Author();
        String firstName = FIRST_NAMES.get(RANDOM.nextInt(FIRST_NAMES.size()));
        String lastName = LAST_NAMES.get(RANDOM.nextInt(LAST_NAMES.size()));
        author.setName(firstName + " " + lastName);
        author.setReligiousAffiliation("test-data - " + RELIGIONS.get(RANDOM.nextInt(RELIGIONS.size())));
        author.setBirthCountry(COUNTRIES.get(RANDOM.nextInt(COUNTRIES.size())));
        author.setNationality(NATIONALITIES.get(RANDOM.nextInt(NATIONALITIES.size())));
        author.setDateOfBirth(generateRandomBirthDate());
        author.setDateOfDeath(RANDOM.nextInt(100) < 60 ? generateRandomDeathDate(author.getDateOfBirth()) : null);
        author.setBriefBiography(generateRandomBiography(firstName, lastName));
        author.setGrokipediaUrl("https://grokipedia.example.com/author/" + RANDOM.nextInt(1000));
        return author;
    }

    private LocalDate generateRandomBirthDate() {
        int year = 1800 + RANDOM.nextInt(150);
        int month = 1 + RANDOM.nextInt(12);
        int day = 1 + RANDOM.nextInt(28);
        return LocalDate.of(year, month, day);
    }

    private LocalDate generateRandomDeathDate(LocalDate birthDate) {
        int age = 30 + RANDOM.nextInt(70);  // Died between age 30 and 100
        return birthDate.plusYears(age);
    }

    private String generateRandomBiography(String firstName, String lastName) {
        String country = COUNTRIES.get(RANDOM.nextInt(COUNTRIES.size()));
        return firstName + " " + lastName + " was a prolific writer from " + country + ". " +
               "Known for their insightful works on human nature and society, " +
               "they left an enduring legacy in the literary world. " +
               "Their works continue to inspire readers across generations.";
    }
}
