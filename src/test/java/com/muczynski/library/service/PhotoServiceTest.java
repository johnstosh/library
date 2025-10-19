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
    public void testAddPhoto() {
        // Given
        Long bookId = 1L;
        PhotoDto photoDto = new PhotoDto();
        photoDto.setBase64("dGVzdFBob3Rv");

        Book book = new Book();
        book.setId(bookId);

        Photo photo = new Photo();
        photo.setBase64(photoDto.getBase64());
        photo.setBook(book);

        PhotoDto expectedPhotoDto = new PhotoDto();
        expectedPhotoDto.setId(1L);
        expectedPhotoDto.setBase64(photoDto.getBase64());

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(photoMapper.toEntity(any(PhotoDto.class))).thenReturn(photo);
        when(photoRepository.save(any(Photo.class))).thenReturn(photo);
        when(photoMapper.toDto(any(Photo.class))).thenReturn(expectedPhotoDto);

        // When
        PhotoDto result = photoService.addPhoto(bookId, photoDto);

        // Then
        assertEquals(expectedPhotoDto.getBase64(), result.getBase64());
    }
}