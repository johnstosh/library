package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    public BookDto createBook(BookDto bookDto) {
        Book book = bookMapper.toEntity(bookDto);
        Book savedBook = bookRepository.save(book);
        return bookMapper.toDto(savedBook);
    }

    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    public BookDto getBookById(Long id) {
        return bookRepository.findById(id)
                .map(bookMapper::toDto)
                .orElse(null);
    }

    public void bulkImportBooks(List<BookDto> bookDtos) {
        List<Book> books = bookDtos.stream()
                .map(bookMapper::toEntity)
                .collect(Collectors.toList());
        bookRepository.saveAll(books);
    }
}