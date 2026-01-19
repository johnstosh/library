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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        verify(photoRepository, times(1)).save(any(Photo.class));
    }

    @Test
    public void testGetPhotosByBookId() {
        // Given
        Long bookId = 1L;
        Photo photo1 = new Photo();
        photo1.setId(1L);
        Photo photo2 = new Photo();
        photo2.setId(2L);
        List<Photo> photos = Arrays.asList(photo1, photo2);

        PhotoDto photoDto1 = new PhotoDto();
        photoDto1.setId(1L);
        PhotoDto photoDto2 = new PhotoDto();
        photoDto2.setId(2L);

        when(photoRepository.findByBookIdOrderByPhotoOrder(bookId)).thenReturn(photos);
        when(photoMapper.toDto(photo1)).thenReturn(photoDto1);
        when(photoMapper.toDto(photo2)).thenReturn(photoDto2);

        // When
        List<PhotoDto> result = photoService.getPhotosByBookId(bookId);

        // Then
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    public void testSoftDeletePhoto() {
        // Given
        Long photoId = 1L;
        Photo photo = new Photo();
        photo.setId(photoId);
        photo.setDeletedAt(null);

        when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        // When
        photoService.softDeletePhoto(photoId);

        // Then
        verify(photoRepository, times(1)).save(photo);
        assertNotNull(photo.getDeletedAt());
    }

    @Test
    public void testRestorePhoto() {
        // Given
        Long photoId = 1L;
        Photo photo = new Photo();
        photo.setId(photoId);
        photo.setDeletedAt(java.time.LocalDateTime.now());

        when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        // When
        photoService.restorePhoto(photoId);

        // Then
        verify(photoRepository, times(1)).save(photo);
        assertNull(photo.getDeletedAt());
    }

    @Test
    public void testUpdatePhoto() {
        // Given
        Long photoId = 1L;
        PhotoDto photoDto = new PhotoDto();
        photoDto.setCaption("Updated caption");

        Photo photo = new Photo();
        photo.setId(photoId);
        photo.setCaption("Old caption");

        PhotoDto updatedPhotoDto = new PhotoDto();
        updatedPhotoDto.setId(photoId);
        updatedPhotoDto.setCaption("Updated caption");

        when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));
        when(photoRepository.save(any(Photo.class))).thenReturn(photo);
        when(photoMapper.toDto(photo)).thenReturn(updatedPhotoDto);

        // When
        PhotoDto result = photoService.updatePhoto(photoId, photoDto);

        // Then
        assertEquals("Updated caption", result.getCaption());
        verify(photoRepository, times(1)).save(photo);
    }

    @Test
    public void testDeletePhoto() {
        // Given
        Long photoId = 1L;
        Photo photo = new Photo();
        photo.setId(photoId);

        when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        // When
        photoService.deletePhoto(photoId);

        // Then
        verify(photoRepository, times(1)).delete(photo);
    }
}
