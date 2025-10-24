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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoService {
    private final PhotoRepository photoRepository;
    private final BookRepository bookRepository;
    private final PhotoMapper photoMapper;

    @Transactional
    public PhotoDto addPhoto(Long bookId, MultipartFile file) {
        try {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));
            Photo photo = new Photo();
            photo.setBook(book);
            photo.setImage(file.getBytes());
            photo.setContentType(file.getContentType());
            photo.setCaption("");
            photo.setRotation(0);
            return photoMapper.toDto(photoRepository.save(photo));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store photo data", e);
        }
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
        if (photoDto.getCaption() != null) {
            photo.setCaption(photoDto.getCaption());
        }
        return photoMapper.toDto(photoRepository.save(photo));
    }

    @Transactional
    public void deletePhoto(Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));
        photoRepository.delete(photo);
    }

    @Transactional
    public void rotatePhoto(Long photoId, boolean clockwise) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));
        int delta = clockwise ? 90 : -90;
        int currentRotation = photo.getRotation();
        int newRotation = ((currentRotation + delta) % 360 + 360) % 360;
        photo.setRotation(newRotation);
        photoRepository.save(photo);
    }

    @Transactional(readOnly = true)
    public byte[] getImage(Long photoId) {
        Photo photo = photoRepository.findById(photoId).orElse(null);
        return photo != null ? photo.getImage() : null;
    }

    @Transactional(readOnly = true)
    public Photo getPhotoById(Long id) {
        return photoRepository.findById(id).orElse(null);
    }
}
