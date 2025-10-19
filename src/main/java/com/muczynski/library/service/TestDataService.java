package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.RandomAuthor;
import com.muczynski.library.domain.RandomBook;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LibraryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TestDataService {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private RandomAuthor randomAuthor;

    @Autowired
    private RandomBook randomBook;

    public void generateTestData(int count) {
        if (libraryRepository.findAll().isEmpty()) {
            Library library = new Library();
            library.setName("Test Library");
            library.setHostname("test.local");
            libraryRepository.save(library);
        }

        for (int i = 0; i < count; i++) {
            Author author = randomAuthor.create();
            authorRepository.save(author);

            Book book = randomBook.create(author);
            bookRepository.save(book);
        }
    }

    public void deleteTestData() {
        bookRepository.deleteByPublisher("test-data");
        authorRepository.deleteByReligiousAffiliation("test-data");
    }
}
