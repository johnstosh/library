// (c) Copyright 2025 by Muczynski
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.mapper.PhotoMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.util.Pair;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoService {
    private static final Logger logger = LoggerFactory.getLogger(PhotoService.class);

    private final PhotoRepository photoRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final PhotoMapper photoMapper;

    @Transactional
    public PhotoDto addPhoto(Long bookId, MultipartFile file) {
        try {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));
            List<Photo> existingPhotos = photoRepository.findByBookIdOrderByPhotoOrder(bookId);
            int maxOrder = existingPhotos.stream()
                    .mapToInt(Photo::getPhotoOrder)
                    .max()
                    .orElse(-1);

            Photo photo = new Photo();
            photo.setBook(book);
            photo.setImage(file.getBytes());
            photo.setContentType(file.getContentType());
            photo.setCaption("");
            photo.setRotation(0);
            photo.setPhotoOrder(maxOrder + 1);
            return photoMapper.toDto(photoRepository.save(photo));
        } catch (IOException e) {
            logger.debug("Failed to add photo to book ID {} due to IO error with file {}: {}", bookId, file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Failed to store photo data", e);
        } catch (Exception e) {
            logger.debug("Failed to add photo to book ID {} with file {}: {}", bookId, file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public PhotoDto addPhotoToAuthor(Long authorId, MultipartFile file) {
        try {
            Author author = authorRepository.findById(authorId)
                    .orElseThrow(() -> new RuntimeException("Author not found"));
            Photo photo = new Photo();
            photo.setAuthor(author);
            photo.setImage(file.getBytes());
            photo.setContentType(file.getContentType());
            photo.setCaption("");
            photo.setRotation(0);
            return photoMapper.toDto(photoRepository.save(photo));
        } catch (IOException e) {
            logger.debug("Failed to add photo to author ID {} due to IO error with file {}: {}", authorId, file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Failed to store photo data", e);
        } catch (Exception e) {
            logger.debug("Failed to add photo to author ID {} with file {}: {}", authorId, file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<PhotoDto> getPhotosByBookId(Long bookId) {
        try {
            List<Photo> photos = photoRepository.findByBookIdOrderByPhotoOrder(bookId);
            return photos.stream()
                    .map(photoMapper::toDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.debug("Failed to retrieve photos for book ID {}: {}", bookId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void movePhotoLeft(Long bookId, Long photoId) {
        try {
            List<Photo> photos = photoRepository.findByBookIdOrderByPhotoOrder(bookId);
            int index = -1;
            for (int i = 0; i < photos.size(); i++) {
                if (photos.get(i).getId().equals(photoId)) {
                    index = i;
                    break;
                }
            }

            if (index > 0) {
                Photo photoToMove = photos.remove(index);
                photos.add(index - 1, photoToMove);
                reorderPhotos(photos);
            }
        } catch (Exception e) {
            logger.debug("Failed to move photo ID {} left for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void movePhotoRight(Long bookId, Long photoId) {
        try {
            List<Photo> photos = photoRepository.findByBookIdOrderByPhotoOrder(bookId);
            int index = -1;
            for (int i = 0; i < photos.size(); i++) {
                if (photos.get(i).getId().equals(photoId)) {
                    index = i;
                    break;
                }
            }

            if (index != -1 && index < photos.size() - 1) {
                Photo photoToMove = photos.remove(index);
                photos.add(index + 1, photoToMove);
                reorderPhotos(photos);
            }
        } catch (Exception e) {
            logger.debug("Failed to move photo ID {} right for book ID {}: {}", photoId, bookId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<PhotoDto> getPhotosByAuthorId(Long authorId) {
        try {
            List<Photo> photos = photoRepository.findByAuthorId(authorId);
            return photos.stream()
                    .map(photoMapper::toDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.debug("Failed to retrieve photos for author ID {}: {}", authorId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public PhotoDto updatePhoto(Long photoId, PhotoDto photoDto) {
        try {
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new RuntimeException("Photo not found"));
            if (photoDto.getCaption() != null) {
                photo.setCaption(photoDto.getCaption());
            }
            return photoMapper.toDto(photoRepository.save(photo));
        } catch (Exception e) {
            logger.debug("Failed to update photo ID {} with DTO {}: {}", photoId, photoDto, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void deletePhoto(Long photoId) {
        try {
            Photo photoToDelete = photoRepository.findById(photoId)
                    .orElseThrow(() -> new RuntimeException("Photo not found"));

            Book book = photoToDelete.getBook();
            if (book != null) {
                photoRepository.delete(photoToDelete);
                reorderPhotos(photoRepository.findByBookIdOrderByPhotoOrder(book.getId()));
            } else {
                photoRepository.delete(photoToDelete);
            }
        } catch (Exception e) {
            logger.debug("Failed to delete photo ID {}: {}", photoId, e.getMessage(), e);
            throw e;
        }
    }

    private void reorderPhotos(List<Photo> photos) {
        try {
            for (int i = 0; i < photos.size(); i++) {
                photos.get(i).setPhotoOrder(i);
            }
            photoRepository.saveAll(photos);
        } catch (Exception e) {
            logger.debug("Failed to reorder photos: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void rotatePhoto(Long photoId, boolean clockwise) {
        try {
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new RuntimeException("Photo not found"));
            int delta = clockwise ? 90 : -90;
            int currentRotation = photo.getRotation();
            int newRotation = ((currentRotation + delta) % 360 + 360) % 360;
            photo.setRotation(newRotation);
            photoRepository.save(photo);
        } catch (Exception e) {
            logger.debug("Failed to rotate photo ID {} (clockwise: {}): {}", photoId, clockwise, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public byte[] getImage(Long photoId) {
        try {
            Photo photo = photoRepository.findById(photoId).orElse(null);
            return photo != null ? photo.getImage() : null;
        } catch (Exception e) {
            logger.debug("Failed to retrieve image for photo ID {}: {}", photoId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Photo getPhotoById(Long id) {
        try {
            return photoRepository.findById(id).orElse(null);
        } catch (Exception e) {
            logger.debug("Failed to retrieve photo by ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Pair<byte[], String> getThumbnail(Long photoId, Integer width) {
        try {
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new RuntimeException("Photo not found"));

            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(photo.getImage()));
            if (originalImage == null) {
                return null;
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            int newHeight = (int) Math.round((double) originalHeight / originalWidth * width);

            Image scaledImage = originalImage.getScaledInstance(width, newHeight, Image.SCALE_SMOOTH);
            BufferedImage bufferedScaledImage = new BufferedImage(width, newHeight, originalImage.getType());

            Graphics2D g2d = bufferedScaledImage.createGraphics();
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String formatName = photo.getContentType().substring(photo.getContentType().lastIndexOf("/") + 1);
            ImageIO.write(bufferedScaledImage, formatName, baos);
            return Pair.of(baos.toByteArray(), photo.getContentType());

        } catch (IOException e) {
            logger.debug("IO error generating thumbnail for photo ID {} with width {}: {}", photoId, width, e.getMessage(), e);
            throw new RuntimeException("Failed to create thumbnail", e);
        } catch (Exception e) {
            logger.debug("Failed to generate thumbnail for photo ID {} with width {}: {}", photoId, width, e.getMessage(), e);
            throw e;
        }
    }
}
