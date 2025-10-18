package com.muczynski.library.domain;

import com.muczynski.library.repository.LibraryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class RandomBook {

    @Autowired
    private LibraryRepository libraryRepository;

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

    public Book create(Author author) {
        Book book = new Book();
        book.setTitle(generateRandomTitle());
        book.setAuthor(author);
        book.setLibrary(libraryRepository.findAll().get(0));
        book.setPublisher("test-data");
        return book;
    }

    private String generateRandomTitle() {
        String color = COLORS.get(RANDOM.nextInt(COLORS.size()));
        String noun = NOUNS.get(RANDOM.nextInt(NOUNS.size()));
        String verb = VERBS.get(RANDOM.nextInt(VERBS.size()));
        return "The " + color + " " + noun + " " + verb;
    }
}