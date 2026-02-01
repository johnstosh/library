/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Generates random Photo objects for testing purposes.
 * Creates simple test images with unique checksums.
 */
@Component
public class RandomPhoto {

    private static final List<String> CAPTIONS = List.of(
            "Front cover", "Back cover", "Title page", "Table of contents",
            "Author portrait", "Illustration", "Chapter heading", "Spine"
    );

    private static final Random RANDOM = new Random();

    /**
     * Creates a random photo associated with a book.
     */
    public Photo createForBook(Book book, int photoOrder) {
        Photo photo = new Photo();
        photo.setBook(book);
        photo.setAuthor(book.getAuthor());
        photo.setPhotoOrder(photoOrder);
        photo.setCaption(CAPTIONS.get(RANDOM.nextInt(CAPTIONS.size())));
        photo.setContentType("image/png");
        photo.setImage(generateTestImage(book.getTitle(), photoOrder));
        photo.setImageChecksum(computeChecksum(photo.getImage()));
        photo.setExportStatus(Photo.ExportStatus.PENDING);
        photo.setDateTaken(LocalDateTime.now().minusDays(RANDOM.nextInt(365)));
        return photo;
    }

    /**
     * Creates a random photo associated with an author (author portrait).
     */
    public Photo createForAuthor(Author author, int photoOrder) {
        Photo photo = new Photo();
        photo.setAuthor(author);
        photo.setBook(null);
        photo.setPhotoOrder(photoOrder);
        photo.setCaption("Author portrait of " + author.getName());
        photo.setContentType("image/png");
        photo.setImage(generateTestImage(author.getName(), photoOrder));
        photo.setImageChecksum(computeChecksum(photo.getImage()));
        photo.setExportStatus(Photo.ExportStatus.PENDING);
        photo.setDateTaken(LocalDateTime.now().minusDays(RANDOM.nextInt(365)));
        return photo;
    }

    /**
     * Generates a minimal valid PNG image for testing.
     * The image content varies based on the seed data to ensure unique checksums.
     */
    private byte[] generateTestImage(String seed, int order) {
        // PNG header
        byte[] header = new byte[] {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };

        // Simple IHDR chunk (1x1 pixel, 8-bit grayscale)
        byte[] ihdr = new byte[] {
            0x00, 0x00, 0x00, 0x0D, // length
            0x49, 0x48, 0x44, 0x52, // IHDR
            0x00, 0x00, 0x00, 0x01, // width
            0x00, 0x00, 0x00, 0x01, // height
            0x08, // bit depth
            0x00, // color type (grayscale)
            0x00, 0x00, 0x00, // compression, filter, interlace
            0x1D, (byte)0xF7, (byte)0xA8, (byte)0x97 // CRC
        };

        // IDAT chunk with single pixel data
        // Pixel value varies based on seed for unique images
        int pixelValue = Math.abs((seed + order).hashCode() % 256);
        byte[] idat = new byte[] {
            0x00, 0x00, 0x00, 0x0A, // length
            0x49, 0x44, 0x41, 0x54, // IDAT
            0x78, 0x01, // zlib header
            0x63, (byte)pixelValue, 0x01, 0x00, // compressed data
            0x00, 0x02, 0x00, 0x01, // CRC (simplified)
        };

        // IEND chunk
        byte[] iend = new byte[] {
            0x00, 0x00, 0x00, 0x00, // length
            0x49, 0x45, 0x4E, 0x44, // IEND
            (byte)0xAE, 0x42, 0x60, (byte)0x82 // CRC
        };

        // Combine all chunks
        byte[] result = new byte[header.length + ihdr.length + idat.length + iend.length];
        int pos = 0;
        System.arraycopy(header, 0, result, pos, header.length);
        pos += header.length;
        System.arraycopy(ihdr, 0, result, pos, ihdr.length);
        pos += ihdr.length;
        System.arraycopy(idat, 0, result, pos, idat.length);
        pos += idat.length;
        System.arraycopy(iend, 0, result, pos, iend.length);

        return result;
    }

    /**
     * Computes SHA-256 checksum of image bytes.
     */
    private String computeChecksum(byte[] imageBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(imageBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
