// (c) Copyright 2025 by Muczynski
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.BookDto;
import org.springframework.stereotype.Service;

@Service
public class BookMapper {

    public BookDto toDto(Book book) {
        if (book == null) {
            return null;
        }

        BookDto bookDto = new BookDto();
        bookDto.setId(book.getId());
        bookDto.setTitle(book.getTitle());
        bookDto.setPublicationYear(book.getPublicationYear());
        bookDto.setPublisher(book.getPublisher());
        bookDto.setPlotSummary(book.getPlotSummary());
        bookDto.setRelatedWorks(book.getRelatedWorks());
        bookDto.setDetailedDescription(book.getDetailedDescription());
        bookDto.setDateAddedToLibrary(book.getDateAddedToLibrary());
        bookDto.setStatus(book.getStatus());
        if (book.getAuthor() != null) {
            bookDto.setAuthorId(book.getAuthor().getId());
        }
        if (book.getLibrary() != null) {
            bookDto.setLibraryId(book.getLibrary().getId());
        }
        if (book.getPhotos() != null && !book.getPhotos().isEmpty()) {
            bookDto.setFirstPhotoId(book.getPhotos().get(0).getId());
            bookDto.setFirstPhotoRotation(book.getPhotos().get(0).getRotation());
        }

        return bookDto;
    }

    public Book toEntity(BookDto bookDto) {
        if (bookDto == null) {
            return null;
        }

        Book book = new Book();
        book.setId(bookDto.getId());
        book.setTitle(bookDto.getTitle());
        book.setPublicationYear(bookDto.getPublicationYear());
        book.setPublisher(bookDto.getPublisher());
        book.setPlotSummary(bookDto.getPlotSummary());
        book.setRelatedWorks(bookDto.getRelatedWorks());
        book.setDetailedDescription(bookDto.getDetailedDescription());
        book.setDateAddedToLibrary(bookDto.getDateAddedToLibrary());
        book.setStatus(bookDto.getStatus());

        if (bookDto.getAuthorId() != null) {
            Author author = new Author();
            author.setId(bookDto.getAuthorId());
            book.setAuthor(author);
        }

        if (bookDto.getLibraryId() != null) {
            Library library = new Library();
            library.setId(bookDto.getLibraryId());
            book.setLibrary(library);
        }

        return book;
    }
}