package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LibraryRepository;
import com.muczynski.library.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LoanRepository loanRepository;

    public BookDto createBook(BookDto bookDto) {
        Book book = bookMapper.toEntity(bookDto);
        book.setAuthor(authorRepository.findById(bookDto.getAuthorId()).orElseThrow(() -> new RuntimeException("Author not found: " + bookDto.getAuthorId())));
        book.setLibrary(libraryRepository.findById(bookDto.getLibraryId()).orElseThrow(() -> new RuntimeException("Library not found: " + bookDto.getLibraryId())));
        Book savedBook = bookRepository.save(book);
        return bookMapper.toDto(savedBook);
    }

    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(bookMapper::toDto)
                .sorted(Comparator.comparing(bookDto -> {
                    String title = bookDto.getTitle().toLowerCase();
                    if (title.startsWith("the ")) {
                        return title.substring(4);
                    }
                    return title;
                }))
                .collect(Collectors.toList());
    }

    public BookDto getBookById(Long id) {
        return bookRepository.findById(id)
                .map(bookMapper::toDto)
                .orElse(null);
    }

    public BookDto updateBook(Long id, BookDto bookDto) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Book not found: " + id));
        book.setTitle(bookDto.getTitle());
        book.setPublicationYear(bookDto.getPublicationYear());
        book.setPublisher(bookDto.getPublisher());
        book.setPlotSummary(bookDto.getPlotSummary());
        book.setRelatedWorks(bookDto.getRelatedWorks());
        book.setDetailedDescription(bookDto.getDetailedDescription());
        book.setDateAddedToLibrary(bookDto.getDateAddedToLibrary());
        if (bookDto.getStatus() != null) {
            book.setStatus(bookDto.getStatus());
        }
        if (bookDto.getAuthorId() != null) {
            book.setAuthor(authorRepository.findById(bookDto.getAuthorId()).orElseThrow(() -> new RuntimeException("Author not found: " + bookDto.getAuthorId())));
        }
        if (bookDto.getLibraryId() != null) {
            book.setLibrary(libraryRepository.findById(bookDto.getLibraryId()).orElseThrow(() -> new RuntimeException("Library not found: " + bookDto.getLibraryId())));
        }
        Book savedBook = bookRepository.save(book);
        return bookMapper.toDto(savedBook);
    }

    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new RuntimeException("Book not found: " + id);
        }
        long loanCount = loanRepository.countByBookId(id);
        if (loanCount > 0) {
            throw new RuntimeException("Cannot delete book because it has " + loanCount + " associated loans.");
        }
        bookRepository.deleteById(id);
    }

}
