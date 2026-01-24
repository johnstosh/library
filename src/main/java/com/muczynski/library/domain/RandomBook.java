/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import com.muczynski.library.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
public class RandomBook {

    @Autowired
    private BranchRepository libraryRepository;

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

    private static final List<String> PUBLISHERS = List.of(
            "Penguin Random House", "HarperCollins", "Simon & Schuster",
            "Hachette", "Macmillan", "Scholastic", "Wiley", "Pearson"
    );

    private static final List<String> LOC_PREFIXES = List.of(
            "BX", "BT", "BV", "BR", "BS", "BL", "BM", "BP", "BQ", "BJ"
    );

    private static final Random RANDOM = new Random();

    public Book create(Author author) {
        Book book = new Book();
        book.setTitle(generateRandomTitle());
        book.setAuthor(author);
        book.setLibrary(libraryRepository.findAll().get(0));
        book.setPublisher("test-data - " + PUBLISHERS.get(RANDOM.nextInt(PUBLISHERS.size())));
        book.setPublicationYear(1900 + RANDOM.nextInt(125));
        book.setPlotSummary(generateRandomSummary());
        book.setRelatedWorks(generateRandomRelatedWorks());
        book.setDetailedDescription(generateRandomDescription());
        book.setGrokipediaUrl("https://grokipedia.example.com/book/" + RANDOM.nextInt(1000));
        book.setFreeTextUrl("https://freetext.example.com/" + RANDOM.nextInt(1000));
        book.setDateAddedToLibrary(LocalDateTime.now().minusDays(RANDOM.nextInt(365)));
        book.setStatus(BookStatus.ACTIVE);
        book.setLocNumber(generateRandomLocNumber());
        book.setStatusReason(null);
        return book;
    }

    private String generateRandomTitle() {
        String color = COLORS.get(RANDOM.nextInt(COLORS.size()));
        String noun = NOUNS.get(RANDOM.nextInt(NOUNS.size()));
        String verb = VERBS.get(RANDOM.nextInt(VERBS.size()));
        return "The " + color + " " + noun + " " + verb;
    }

    private String generateRandomSummary() {
        return "A captivating story about " + NOUNS.get(RANDOM.nextInt(NOUNS.size())).toLowerCase() +
               " and " + NOUNS.get(RANDOM.nextInt(NOUNS.size())).toLowerCase() +
               " that explores themes of love, loss, and redemption.";
    }

    private String generateRandomRelatedWorks() {
        return "See also: The " + COLORS.get(RANDOM.nextInt(COLORS.size())) + " " +
               NOUNS.get(RANDOM.nextInt(NOUNS.size())) + " Series";
    }

    private String generateRandomDescription() {
        return "This remarkable work by the author showcases their mastery of narrative. " +
               "Set in a world where " + NOUNS.get(RANDOM.nextInt(NOUNS.size())).toLowerCase() +
               " meet " + NOUNS.get(RANDOM.nextInt(NOUNS.size())).toLowerCase() +
               ", the story unfolds with unexpected twists and profound insights.";
    }

    private String generateRandomLocNumber() {
        String prefix = LOC_PREFIXES.get(RANDOM.nextInt(LOC_PREFIXES.size()));
        int number = 100 + RANDOM.nextInt(9900);
        String suffix = String.format(".%c%d", (char)('A' + RANDOM.nextInt(26)), RANDOM.nextInt(100));
        return prefix + number + suffix;
    }
}
