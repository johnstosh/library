/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

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
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
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
                    .orElseThrow(() -> new LibraryException("Book not found"));
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
            photo.setPhotoOrder(maxOrder + 1);
            return photoMapper.toDto(photoRepository.save(photo));
        } catch (IOException e) {
            logger.debug("Failed to add photo to book ID {} due to IO error with file {}: {}", bookId, file.getOriginalFilename(), e.getMessage(), e);
            throw new LibraryException("Failed to store photo data", e);
        } catch (Exception e) {
            logger.debug("Failed to add photo to book ID {} with file {}: {}", bookId, file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Add photo to book using raw image bytes and content type
     * Used by books-from-feed when downloading photos from Google Photos
     */
    @Transactional
    public PhotoDto addPhotoFromBytes(Long bookId, byte[] imageBytes, String contentType) {
        try {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new LibraryException("Book not found"));
            List<Photo> existingPhotos = photoRepository.findByBookIdOrderByPhotoOrder(bookId);
            int maxOrder = existingPhotos.stream()
                    .mapToInt(Photo::getPhotoOrder)
                    .max()
                    .orElse(-1);

            Photo photo = new Photo();
            photo.setBook(book);
            photo.setImage(imageBytes);
            photo.setContentType(contentType != null ? contentType : "image/jpeg");
            photo.setCaption("");
            photo.setPhotoOrder(maxOrder + 1);

            Photo savedPhoto = photoRepository.save(photo);
            logger.debug("Added photo to book ID {} with order {}", bookId, savedPhoto.getPhotoOrder());
            return photoMapper.toDto(savedPhoto);
        } catch (Exception e) {
            logger.error("Failed to add photo from bytes to book ID {}: {}", bookId, e.getMessage(), e);
            throw new LibraryException("Failed to store photo data: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteAuthorPhoto(Long authorId, Long photoId) {
        try {
            Photo photoToDelete = photoRepository.findById(photoId)
                    .orElseThrow(() -> new LibraryException("Photo not found"));

            if (photoToDelete.getAuthor() == null || !photoToDelete.getAuthor().getId().equals(authorId)) {
                throw new LibraryException("Photo does not belong to the specified author");
            }

            photoRepository.delete(photoToDelete);
            reorderAuthorPhotos(authorId);
        } catch (Exception e) {
            logger.debug("Failed to delete photo ID {} for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void rotateAuthorPhoto(Long authorId, Long photoId, boolean clockwise) {
        try {
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new LibraryException("Photo not found"));

            if (photo.getAuthor() == null || !photo.getAuthor().getId().equals(authorId)) {
                throw new LibraryException("Photo does not belong to the specified author");
            }

            rotateImage(photo, clockwise ? 90 : -90);
            photoRepository.save(photo);
        } catch (Exception e) {
            logger.debug("Failed to rotate photo ID {} for author ID {} (clockwise: {}): {}", photoId, authorId, clockwise, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void moveAuthorPhotoLeft(Long authorId, Long photoId) {
        try {
            List<Photo> photos = photoRepository.findByAuthorIdOrderByPhotoOrder(authorId);
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
                reorderAuthorPhotos(photos);
            }
        } catch (Exception e) {
            logger.debug("Failed to move photo ID {} left for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void moveAuthorPhotoRight(Long authorId, Long photoId) {
        try {
            List<Photo> photos = photoRepository.findByAuthorIdOrderByPhotoOrder(authorId);
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
                reorderAuthorPhotos(photos);
            }
        } catch (Exception e) {
            logger.debug("Failed to move photo ID {} right for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            throw e;
        }
    }

    private void rotateImage(Photo photo, int degrees) {
        try {
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(photo.getImage()));
            if (originalImage == null) {
                throw new LibraryException("Invalid image data");
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            double radians = Math.toRadians(degrees);
            double sin = Math.abs(Math.sin(radians));
            double cos = Math.abs(Math.cos(radians));
            int newWidth = (int) Math.floor(width * cos + height * sin);
            int newHeight = (int) Math.floor(height * cos + width * sin);

            BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
            Graphics2D g2d = rotatedImage.createGraphics();
            AffineTransform at = new AffineTransform();
            at.setToRotation(radians, newWidth / 2.0, newHeight / 2.0);
            at.translate((newWidth - width) / 2.0, (newHeight - height) / 2.0);
            g2d.setTransform(at);
            g2d.drawImage(originalImage, 0, 0, null);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String formatName = photo.getContentType().substring(photo.getContentType().lastIndexOf("/") + 1);
            ImageIO.write(rotatedImage, formatName, baos);
            photo.setImage(baos.toByteArray());
        } catch (IOException e) {
            logger.debug("IO error rotating image: {}", e.getMessage(), e);
            throw new LibraryException("Failed to rotate image", e);
        }
    }

    @Transactional
    public PhotoDto addPhotoToAuthor(Long authorId, MultipartFile file) {
        try {
            Author author = authorRepository.findById(authorId)
                    .orElseThrow(() -> new LibraryException("Author not found"));
            List<Photo> existingPhotos = photoRepository.findByAuthorIdOrderByPhotoOrder(authorId);
            int maxOrder = existingPhotos.stream()
                    .mapToInt(Photo::getPhotoOrder)
                    .max()
                    .orElse(-1);

            Photo photo = new Photo();
            photo.setAuthor(author);
            photo.setImage(file.getBytes());
            photo.setContentType(file.getContentType());
            photo.setCaption("");
            photo.setPhotoOrder(maxOrder + 1);
            return photoMapper.toDto(photoRepository.save(photo));
        } catch (IOException e) {
            logger.debug("Failed to add photo to author ID {} due to IO error with file {}: {}", authorId, file.getOriginalFilename(), e.getMessage(), e);
            throw new LibraryException("Failed to store photo data", e);
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
            List<Photo> photos = photoRepository.findByAuthorIdOrderByPhotoOrder(authorId);
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
                    .orElseThrow(() -> new LibraryException("Photo not found"));
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
                    .orElseThrow(() -> new LibraryException("Photo not found"));

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

    private void reorderAuthorPhotos(Long authorId) {
        try {
            List<Photo> photos = photoRepository.findByAuthorIdOrderByPhotoOrder(authorId);
            reorderPhotos(photos);
        } catch (Exception e) {
            logger.debug("Failed to reorder author photos for author ID {}: {}", authorId, e.getMessage(), e);
            throw e;
        }
    }

    private void reorderAuthorPhotos(List<Photo> photos) {
        try {
            for (int i = 0; i < photos.size(); i++) {
                photos.get(i).setPhotoOrder(i);
            }
            photoRepository.saveAll(photos);
        } catch (Exception e) {
            logger.debug("Failed to reorder author photos: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void rotatePhoto(Long photoId, boolean clockwise) {
        try {
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new LibraryException("Photo not found"));
            rotateImage(photo, clockwise ? 90 : -90);
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
                    .orElseThrow(() -> new LibraryException("Photo not found"));

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
            throw new LibraryException("Failed to create thumbnail", e);
        } catch (Exception e) {
            logger.debug("Failed to generate thumbnail for photo ID {} with width {}: {}", photoId, width, e.getMessage(), e);
            throw e;
        }
    }
}
