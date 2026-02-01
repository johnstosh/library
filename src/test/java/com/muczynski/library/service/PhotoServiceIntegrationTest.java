/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PhotoService thumbnail generation
 */
@SpringBootTest
@ActiveProfiles("test")
class PhotoServiceIntegrationTest {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    // Unique suffix to avoid conflicts with other tests
    private String uniqueSuffix;

    @BeforeEach
    void setUp() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        // Clean up any leftover data
        loanRepository.deleteAll();
        photoRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // Delete in correct order: loans -> photos -> books
        loanRepository.deleteAll();
        photoRepository.deleteAll();
        bookRepository.deleteAll();
    }

    private byte[] createColoredImage(int width, int height, Color color, String format) throws Exception {
        int imageType = format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg")
                ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;

        BufferedImage image = new BufferedImage(width, height, imageType);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    void getThumbnail_withJpegImage_shouldGenerateCorrectSizeAndColor() throws Exception {
        // Given: A book with a red JPEG photo
        Book book = new Book();
        book.setTitle("Test Book JPEG " + uniqueSuffix);
        bookRepository.save(book);

        byte[] jpegBytes = createColoredImage(800, 1200, Color.RED, "jpg");

        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(jpegBytes);
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photoRepository.save(photo);

        // When: Generating a thumbnail with width 200
        Pair<byte[], String> thumbnail = photoService.getThumbnail(photo.getId(), 200);

        // Then: Thumbnail should be correct size and maintain red color
        assertNotNull(thumbnail);
        assertEquals(MediaType.IMAGE_JPEG_VALUE, thumbnail.getSecond());

        byte[] thumbnailBytes = thumbnail.getFirst();
        assertTrue(thumbnailBytes.length > 0);

        BufferedImage thumbnailImage = ImageIO.read(new ByteArrayInputStream(thumbnailBytes));
        assertNotNull(thumbnailImage);
        assertEquals(200, thumbnailImage.getWidth());
        assertEquals(300, thumbnailImage.getHeight()); // Maintains aspect ratio

        // Verify the thumbnail is red (not black)
        int centerPixelRGB = thumbnailImage.getRGB(thumbnailImage.getWidth() / 2, thumbnailImage.getHeight() / 2);
        int red = (centerPixelRGB >> 16) & 0xFF;
        int green = (centerPixelRGB >> 8) & 0xFF;
        int blue = centerPixelRGB & 0xFF;

        assertTrue(red > 200, "Red channel should be dominant, got: " + red);
        assertTrue(green < 50, "Green channel should be low, got: " + green);
        assertTrue(blue < 50, "Blue channel should be low, got: " + blue);
    }

    @Test
    void getThumbnail_withPngImage_shouldGenerateCorrectSizeAndColor() throws Exception {
        // Given: A book with a blue PNG photo
        Book book = new Book();
        book.setTitle("Test Book PNG " + uniqueSuffix);
        bookRepository.save(book);

        byte[] pngBytes = createColoredImage(600, 800, Color.BLUE, "png");

        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(pngBytes);
        photo.setContentType(MediaType.IMAGE_PNG_VALUE);
        photoRepository.save(photo);

        // When: Generating a thumbnail with width 150
        Pair<byte[], String> thumbnail = photoService.getThumbnail(photo.getId(), 150);

        // Then: Thumbnail should be correct size and maintain blue color
        assertNotNull(thumbnail);
        assertEquals(MediaType.IMAGE_PNG_VALUE, thumbnail.getSecond());

        byte[] thumbnailBytes = thumbnail.getFirst();
        assertTrue(thumbnailBytes.length > 0);

        BufferedImage thumbnailImage = ImageIO.read(new ByteArrayInputStream(thumbnailBytes));
        assertNotNull(thumbnailImage);
        assertEquals(150, thumbnailImage.getWidth());
        assertEquals(200, thumbnailImage.getHeight()); // Maintains aspect ratio

        // Verify the thumbnail is blue (not black)
        int centerPixelRGB = thumbnailImage.getRGB(thumbnailImage.getWidth() / 2, thumbnailImage.getHeight() / 2);
        int red = (centerPixelRGB >> 16) & 0xFF;
        int green = (centerPixelRGB >> 8) & 0xFF;
        int blue = centerPixelRGB & 0xFF;

        assertTrue(blue > 200, "Blue channel should be dominant, got: " + blue);
        assertTrue(red < 50, "Red channel should be low, got: " + red);
        assertTrue(green < 50, "Green channel should be low, got: " + green);
    }

    @Test
    void getThumbnail_withGreenImage_shouldMaintainColor() throws Exception {
        // Given: A book with a green JPEG photo
        Book book = new Book();
        book.setTitle("Test Book Green " + uniqueSuffix);
        bookRepository.save(book);

        byte[] jpegBytes = createColoredImage(400, 600, Color.GREEN, "jpg");

        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(jpegBytes);
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photoRepository.save(photo);

        // When: Generating a thumbnail with width 100
        Pair<byte[], String> thumbnail = photoService.getThumbnail(photo.getId(), 100);

        // Then: Thumbnail should maintain green color
        assertNotNull(thumbnail);

        BufferedImage thumbnailImage = ImageIO.read(new ByteArrayInputStream(thumbnail.getFirst()));
        assertNotNull(thumbnailImage);

        // Verify the thumbnail is green (not black)
        int centerPixelRGB = thumbnailImage.getRGB(thumbnailImage.getWidth() / 2, thumbnailImage.getHeight() / 2);
        int red = (centerPixelRGB >> 16) & 0xFF;
        int green = (centerPixelRGB >> 8) & 0xFF;
        int blue = centerPixelRGB & 0xFF;

        assertTrue(green > 200, "Green channel should be dominant, got: " + green);
        assertTrue(red < 50, "Red channel should be low, got: " + red);
        assertTrue(blue < 50, "Blue channel should be low, got: " + blue);
    }

    @Test
    void getThumbnail_withDifferentWidths_shouldScaleCorrectly() throws Exception {
        // Given: A book with a photo
        Book book = new Book();
        book.setTitle("Test Book Widths " + uniqueSuffix);
        bookRepository.save(book);

        byte[] jpegBytes = createColoredImage(1000, 500, Color.YELLOW, "jpg");

        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(jpegBytes);
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photoRepository.save(photo);

        // When: Generating thumbnails with different widths
        Pair<byte[], String> thumbnail100 = photoService.getThumbnail(photo.getId(), 100);
        Pair<byte[], String> thumbnail200 = photoService.getThumbnail(photo.getId(), 200);
        Pair<byte[], String> thumbnail400 = photoService.getThumbnail(photo.getId(), 400);

        // Then: All thumbnails should have correct dimensions
        BufferedImage img100 = ImageIO.read(new ByteArrayInputStream(thumbnail100.getFirst()));
        assertEquals(100, img100.getWidth());
        assertEquals(50, img100.getHeight()); // Maintains 2:1 aspect ratio

        BufferedImage img200 = ImageIO.read(new ByteArrayInputStream(thumbnail200.getFirst()));
        assertEquals(200, img200.getWidth());
        assertEquals(100, img200.getHeight());

        BufferedImage img400 = ImageIO.read(new ByteArrayInputStream(thumbnail400.getFirst()));
        assertEquals(400, img400.getWidth());
        assertEquals(200, img400.getHeight());
    }
}
