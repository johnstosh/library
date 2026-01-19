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
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
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
    private final EntityManager entityManager;

    /**
     * Compute SHA-256 checksum of image bytes
     */
    private String computeChecksum(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(imageBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            return null;
        }
    }

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

            byte[] imageBytes = file.getBytes();
            Photo photo = new Photo();
            photo.setBook(book);
            photo.setImage(imageBytes);
            photo.setContentType(file.getContentType());
            photo.setCaption("");
            photo.setPhotoOrder(maxOrder + 1);
            photo.setImageChecksum(computeChecksum(imageBytes));
            return photoMapper.toDto(photoRepository.save(photo));
        } catch (IOException e) {
            logger.warn("Failed to add photo to book ID {} due to IO error with file {}: {}", bookId, file.getOriginalFilename(), e.getMessage(), e);
            throw new LibraryException("Failed to store photo data", e);
        } catch (Exception e) {
            logger.warn("Failed to add photo to book ID {} with file {}: {}", bookId, file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Add photo to book using raw image bytes and content type
     * Used by books-from-feed when downloading photos from Google Photos
     */
    @Transactional
    public PhotoDto addPhotoFromBytes(Long bookId, byte[] imageBytes, String contentType) {
        return addPhotoFromBytes(bookId, imageBytes, contentType, null);
    }

    /**
     * Add checkout card photo to a loan using raw image bytes
     * Used by loan-by-photo feature to store the checkout card image
     */
    @Transactional
    public PhotoDto addPhotoToLoan(Long loanId, byte[] imageBytes, String contentType) {
        try {
            Photo photo = new Photo();
            // Note: Loan will be set by the caller (LoanService) after loan creation
            photo.setImage(imageBytes);
            photo.setContentType(contentType != null ? contentType : "image/jpeg");
            photo.setCaption("Checkout card photo");
            photo.setPhotoOrder(0);
            photo.setImageChecksum(computeChecksum(imageBytes));

            Photo savedPhoto = photoRepository.save(photo);
            logger.info("Added checkout card photo with checksum: {}", savedPhoto.getImageChecksum());
            return photoMapper.toDto(savedPhoto);
        } catch (Exception e) {
            logger.error("Failed to add checkout card photo: {}", e.getMessage(), e);
            throw new LibraryException("Failed to store checkout card photo: " + e.getMessage(), e);
        }
    }

    /**
     * Associate an existing photo with a loan
     * Used after loan creation to link the checkout card photo to the loan
     */
    @Transactional
    public void associatePhotoWithLoan(Long photoId, com.muczynski.library.domain.Loan loan) {
        try {
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new LibraryException("Photo not found"));
            photo.setLoan(loan);
            photoRepository.save(photo);
            logger.info("Associated photo ID {} with loan ID {}", photoId, loan.getId());
        } catch (Exception e) {
            logger.error("Failed to associate photo with loan: {}", e.getMessage(), e);
            throw new LibraryException("Failed to associate photo with loan: " + e.getMessage(), e);
        }
    }

    /**
     * Get photo associated with a loan
     */
    @Transactional(readOnly = true)
    public PhotoDto getPhotoByLoanId(Long loanId) {
        return photoRepository.findByLoanId(loanId)
                .map(photoMapper::toDto)
                .orElse(null);
    }

    /**
     * Add photo to book using raw image bytes, content type, and optional date taken
     * Used by books-from-feed when downloading photos from Google Photos with metadata
     */
    @Transactional
    public PhotoDto addPhotoFromBytes(Long bookId, byte[] imageBytes, String contentType, LocalDateTime dateTaken) {
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
            photo.setImageChecksum(computeChecksum(imageBytes));
            if (dateTaken != null) {
                photo.setDateTaken(dateTaken);
            }

            Photo savedPhoto = photoRepository.save(photo);
            logger.debug("Added photo to book ID {} with order {} (dateTaken: {})",
                    bookId, savedPhoto.getPhotoOrder(), dateTaken);
            return photoMapper.toDto(savedPhoto);
        } catch (Exception e) {
            logger.error("Failed to add photo from bytes to book ID {}: {}", bookId, e.getMessage(), e);
            throw new LibraryException("Failed to store photo data: " + e.getMessage(), e);
        }
    }

    /**
     * Add photo to book from Google Photos with permanent ID
     * Used when importing photos directly from Google Photos Picker
     * The photo is marked as already exported since it comes from Google Photos
     */
    @Transactional
    public PhotoDto addPhotoFromGooglePhotos(Long bookId, byte[] imageBytes, String contentType, String permanentId) {
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
            photo.setImageChecksum(computeChecksum(imageBytes));

            // Set Google Photos permanent ID and mark as already exported
            photo.setPermanentId(permanentId);
            photo.setExportStatus(Photo.ExportStatus.COMPLETED);
            photo.setExportedAt(LocalDateTime.now());

            Photo savedPhoto = photoRepository.save(photo);
            logger.info("Added photo from Google Photos to book ID {} with permanent ID: {}", bookId, permanentId);
            return photoMapper.toDto(savedPhoto);
        } catch (Exception e) {
            logger.error("Failed to add photo from Google Photos to book ID {}: {}", bookId, e.getMessage(), e);
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
            logger.warn("Failed to delete photo ID {} for author ID {}: {}", photoId, authorId, e.getMessage(), e);
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
            logger.warn("Failed to rotate photo ID {} for author ID {} (clockwise: {}): {}", photoId, authorId, clockwise, e.getMessage(), e);
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
            logger.warn("Failed to move photo ID {} left for author ID {}: {}", photoId, authorId, e.getMessage(), e);
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
            logger.warn("Failed to move photo ID {} right for author ID {}: {}", photoId, authorId, e.getMessage(), e);
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
            byte[] rotatedBytes = baos.toByteArray();
            photo.setImage(rotatedBytes);
            // Recalculate checksum after rotation since image bytes changed
            photo.setImageChecksum(computeChecksum(rotatedBytes));
        } catch (IOException e) {
            logger.error("IO error rotating image: {}", e.getMessage(), e);
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

            byte[] imageBytes = file.getBytes();
            Photo photo = new Photo();
            photo.setAuthor(author);
            photo.setImage(imageBytes);
            photo.setContentType(file.getContentType());
            photo.setCaption("");
            photo.setPhotoOrder(maxOrder + 1);
            photo.setImageChecksum(computeChecksum(imageBytes));
            return photoMapper.toDto(photoRepository.save(photo));
        } catch (IOException e) {
            logger.warn("Failed to add photo to author ID {} due to IO error with file {}: {}", authorId, file.getOriginalFilename(), e.getMessage(), e);
            throw new LibraryException("Failed to store photo data", e);
        } catch (Exception e) {
            logger.warn("Failed to add photo to author ID {} with file {}: {}", authorId, file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Add photo to author from Google Photos with permanent ID
     * Used when importing photos directly from Google Photos Picker
     * The photo is marked as already exported since it comes from Google Photos
     */
    @Transactional
    public PhotoDto addAuthorPhotoFromGooglePhotos(Long authorId, byte[] imageBytes, String contentType, String permanentId) {
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
            photo.setImage(imageBytes);
            photo.setContentType(contentType != null ? contentType : "image/jpeg");
            photo.setCaption("");
            photo.setPhotoOrder(maxOrder + 1);
            photo.setImageChecksum(computeChecksum(imageBytes));

            // Set Google Photos permanent ID and mark as already exported
            photo.setPermanentId(permanentId);
            photo.setExportStatus(Photo.ExportStatus.COMPLETED);
            photo.setExportedAt(LocalDateTime.now());

            Photo savedPhoto = photoRepository.save(photo);
            logger.info("Added author photo from Google Photos to author ID {} with permanent ID: {}", authorId, permanentId);
            return photoMapper.toDto(savedPhoto);
        } catch (Exception e) {
            logger.error("Failed to add author photo from Google Photos to author ID {}: {}", authorId, e.getMessage(), e);
            throw new LibraryException("Failed to store photo data: " + e.getMessage(), e);
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
            logger.warn("Failed to retrieve photos for book ID {}: {}", bookId, e.getMessage(), e);
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
            logger.warn("Failed to move photo ID {} left for book ID {}: {}", photoId, bookId, e.getMessage(), e);
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
            logger.warn("Failed to move photo ID {} right for book ID {}: {}", photoId, bookId, e.getMessage(), e);
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
            logger.warn("Failed to retrieve photos for author ID {}: {}", authorId, e.getMessage(), e);
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
            logger.warn("Failed to update photo ID {} with DTO {}: {}", photoId, photoDto, e.getMessage(), e);
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
            logger.warn("Failed to delete photo ID {}: {}", photoId, e.getMessage(), e);
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
            logger.warn("Failed to reorder photos: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void reorderAuthorPhotos(Long authorId) {
        try {
            List<Photo> photos = photoRepository.findByAuthorIdOrderByPhotoOrder(authorId);
            reorderPhotos(photos);
        } catch (Exception e) {
            logger.warn("Failed to reorder author photos for author ID {}: {}", authorId, e.getMessage(), e);
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
            logger.warn("Failed to reorder author photos: {}", e.getMessage(), e);
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
            logger.warn("Failed to rotate photo ID {} (clockwise: {}): {}", photoId, clockwise, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public byte[] getImage(Long photoId) {
        try {
            Photo photo = photoRepository.findById(photoId).orElse(null);
            return photo != null ? photo.getImage() : null;
        } catch (Exception e) {
            logger.warn("Failed to retrieve image for photo ID {}: {}", photoId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Photo getPhotoById(Long id) {
        try {
            return photoRepository.findById(id).orElse(null);
        } catch (Exception e) {
            logger.warn("Failed to retrieve photo by ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Add edited photo as a new photo to the left of the original
     * Instead of replacing the original, this creates a new photo with the edited image
     * and places it before the original in the photo order
     * @param photoId The original photo ID
     * @param file The edited image file
     */
    @Transactional
    public void cropPhoto(Long photoId, MultipartFile file) {
        try {
            Photo originalPhoto = photoRepository.findById(photoId)
                    .orElseThrow(() -> new LibraryException("Photo not found"));

            // Get the book or author from the original photo
            Book book = originalPhoto.getBook();
            Author author = originalPhoto.getAuthor();

            if (book == null && author == null) {
                throw new LibraryException("Photo must be associated with a book or author");
            }

            int originalOrder = originalPhoto.getPhotoOrder();

            // Shift all photos at or after the original's position to the right
            List<Photo> photosToShift;
            if (book != null) {
                photosToShift = photoRepository.findByBookIdOrderByPhotoOrder(book.getId());
            } else {
                photosToShift = photoRepository.findByAuthorIdOrderByPhotoOrder(author.getId());
            }

            // Shift photos that are at or after the original's position
            for (Photo photo : photosToShift) {
                if (photo.getPhotoOrder() >= originalOrder) {
                    photo.setPhotoOrder(photo.getPhotoOrder() + 1);
                }
            }
            photoRepository.saveAll(photosToShift);

            // Create a new photo with the edited image at the original's position
            byte[] imageBytes = file.getBytes();
            Photo newPhoto = new Photo();
            newPhoto.setBook(book);
            newPhoto.setAuthor(author);
            newPhoto.setImage(imageBytes);
            newPhoto.setContentType(file.getContentType());
            newPhoto.setCaption(""); // New edited photo starts with empty caption
            newPhoto.setPhotoOrder(originalOrder); // Place at original's position (left of shifted original)
            newPhoto.setImageChecksum(computeChecksum(imageBytes));

            photoRepository.save(newPhoto);
            logger.info("Added edited photo to the left of original photo ID {}. New photo order: {}, Original shifted to: {}",
                    photoId, originalOrder, originalOrder + 1);
        } catch (IOException e) {
            logger.error("Failed to add edited photo for original ID {} due to IO error: {}", photoId, e.getMessage(), e);
            throw new LibraryException("Failed to store edited photo data", e);
        } catch (Exception e) {
            logger.error("Failed to add edited photo for original ID {}: {}", photoId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Pair<byte[], String> getThumbnail(Long photoId, Integer width) {
        try {
            logger.debug("Generating thumbnail for photo ID {} with width {}", photoId, width);

            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new LibraryException("Photo not found"));

            logger.debug("Photo found: ID {}, contentType {}, imageSize {} bytes",
                    photoId, photo.getContentType(), photo.getImage() != null ? photo.getImage().length : 0);

            // Check if image data exists
            if (photo.getImage() == null || photo.getImage().length == 0) {
                logger.error("Photo ID {} has no image data (null or empty)", photoId);
                throw new LibraryException("Photo has no image data");
            }

            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(photo.getImage()));
            if (originalImage == null) {
                logger.error("Failed to read image data for photo ID {}", photoId);
                throw new LibraryException("Failed to read image data");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            int newHeight = (int) Math.round((double) originalHeight / originalWidth * width);

            logger.debug("Scaling image from {}x{} to {}x{}", originalWidth, originalHeight, width, newHeight);

            Image scaledImage = originalImage.getScaledInstance(width, newHeight, Image.SCALE_SMOOTH);

            // Determine the appropriate BufferedImage type based on content type
            // JPEG doesn't support alpha channel, so use TYPE_INT_RGB for JPEG
            String contentType = photo.getContentType().toLowerCase();
            int imageType;
            if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                imageType = BufferedImage.TYPE_INT_RGB;
            } else {
                imageType = BufferedImage.TYPE_INT_ARGB;
            }

            BufferedImage bufferedScaledImage = new BufferedImage(width, newHeight, imageType);

            Graphics2D g2d = bufferedScaledImage.createGraphics();
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();

            logger.debug("Image scaled successfully, writing to output stream");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String formatName = photo.getContentType().substring(photo.getContentType().lastIndexOf("/") + 1);

            // Handle common format name variations
            if (formatName.equalsIgnoreCase("jpeg")) {
                formatName = "jpg";
            }

            logger.debug("Writing image as format: {}", formatName);

            boolean writeSuccess = ImageIO.write(bufferedScaledImage, formatName, baos);
            if (!writeSuccess) {
                logger.error("ImageIO.write returned false for format {} on photo ID {}", formatName, photoId);
                throw new LibraryException("Failed to write thumbnail image - unsupported format: " + formatName);
            }

            byte[] thumbnailBytes = baos.toByteArray();
            logger.debug("Thumbnail generated successfully: {} bytes", thumbnailBytes.length);

            return Pair.of(thumbnailBytes, photo.getContentType());

        } catch (IOException e) {
            logger.error("IO error generating thumbnail for photo ID {} with width {}: {}", photoId, width, e.getMessage(), e);
            throw new LibraryException("Failed to create thumbnail due to IO error", e);
        } catch (LibraryException e) {
            logger.error("Library error generating thumbnail for photo ID {} with width {}: {}", photoId, width, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error generating thumbnail for photo ID {} with width {}: {}", photoId, width, e.getMessage(), e);
            throw new LibraryException("Failed to generate thumbnail", e);
        }
    }

    /**
     * Soft delete a photo by setting its deletedAt timestamp
     */
    @Transactional
    public void softDeletePhoto(Long photoId) {
        try {
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new LibraryException("Photo not found"));
            photo.setDeletedAt(LocalDateTime.now());
            photoRepository.save(photo);
            logger.info("Soft deleted photo ID {}", photoId);
        } catch (Exception e) {
            logger.warn("Failed to soft delete photo ID {}: {}", photoId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Restore a soft-deleted photo by clearing its deletedAt timestamp
     */
    @Transactional
    public void restorePhoto(Long photoId) {
        try {
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new LibraryException("Photo not found"));
            photo.setDeletedAt(null);
            photoRepository.save(photo);
            logger.info("Restored photo ID {}", photoId);
        } catch (Exception e) {
            logger.warn("Failed to restore photo ID {}: {}", photoId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Migrate existing photos to compute SHA-256 checksums
     * Runs after application startup to backfill checksums for photos that don't have them
     * Processes photos one at a time to avoid OutOfMemoryError
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migratePhotosWithoutChecksum() {
        try {
            // Get only the IDs first - this doesn't load image bytes
            List<Long> photoIds = photoRepository.findIdsWithoutChecksum();

            if (photoIds.isEmpty()) {
                logger.info("Checksum migration: No photos without checksum found");
                return;
            }

            logger.info("Checksum migration: Found {} photos without checksum, computing...", photoIds.size());

            int processed = 0;
            int failed = 0;

            // Process photos one at a time to avoid loading all images into memory
            for (Long photoId : photoIds) {
                try {
                    Photo photo = photoRepository.findById(photoId).orElse(null);
                    if (photo == null) {
                        continue;
                    }

                    byte[] imageBytes = photo.getImage();
                    if (imageBytes != null && imageBytes.length > 0) {
                        String checksum = computeChecksum(imageBytes);
                        photo.setImageChecksum(checksum);
                        photoRepository.save(photo);
                        processed++;

                        if (processed % 100 == 0) {
                            logger.info("Checksum migration: Processed {} of {} photos", processed, photoIds.size());
                        }
                    } else {
                        logger.warn("Checksum migration: Photo ID {} has no image data", photo.getId());
                        failed++;
                    }

                    // Clear the persistence context to free memory after each photo
                    entityManager.flush();
                    entityManager.clear();

                } catch (Exception e) {
                    logger.error("Checksum migration: Failed to compute checksum for photo ID {}: {}", photoId, e.getMessage());
                    failed++;
                }
            }

            logger.info("Checksum migration complete: {} photos processed, {} failed", processed, failed);

        } catch (Exception e) {
            logger.error("Checksum migration failed: {}", e.getMessage(), e);
        }
    }
}
