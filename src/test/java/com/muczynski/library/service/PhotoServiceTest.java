/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.mapper.PhotoMapper;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PhotoServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private PhotoMapper photoMapper;

    @InjectMocks
    private PhotoService photoService;

    @Test
    public void testAddPhoto() throws Exception {
        // Given
        Long bookId = 1L;
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());

        Book book = new Book();
        book.setId(bookId);

        Photo photo = new Photo();
        photo.setBook(book);
        photo.setContentType("image/jpeg");
        photo.setImage(file.getBytes());

        PhotoDto expectedPhotoDto = new PhotoDto();
        expectedPhotoDto.setId(1L);
        expectedPhotoDto.setContentType("image/jpeg");

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(photoRepository.save(any(Photo.class))).thenReturn(photo);
        when(photoMapper.toDto(any(Photo.class))).thenReturn(expectedPhotoDto);

        // When
        PhotoDto result = photoService.addPhoto(bookId, file);

        // Then
        assertEquals(expectedPhotoDto.getContentType(), result.getContentType());
    }
}
