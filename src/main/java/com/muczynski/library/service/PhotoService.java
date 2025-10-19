package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.mapper.PhotoMapper;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoService {
    private final PhotoRepository photoRepository;
    private final BookRepository bookRepository;
    private final PhotoMapper photoMapper;

    @Transactional
    public PhotoDto addPhoto(Long bookId, PhotoDto photoDto) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        Photo photo = photoMapper.toEntity(photoDto);
        photo.setBook(book);
        return photoMapper.toDto(photoRepository.save(photo));
    }

    @Transactional(readOnly = true)
    public List<PhotoDto> getPhotosByBookId(Long bookId) {
        List<Photo> photos = photoRepository.findByBookId(bookId);
        return photos.stream()
                .map(photoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PhotoDto updatePhoto(Long photoId, PhotoDto photoDto) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));
        photo.setBase64(photoDto.getBase64());
        return photoMapper.toDto(photoRepository.save(photo));
    }

    @Transactional
    public void deletePhoto(Long photoId) {
        photoRepository.deleteById(photoId);
    }
}