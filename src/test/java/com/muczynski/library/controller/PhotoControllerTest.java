/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @AfterEach
    void tearDown() {
        loanRepository.deleteAll();
        photoRepository.deleteAll();
        bookRepository.deleteAll();
    }

    private byte[] createDummyImage(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    @Test
    @WithMockUser
    void getThumbnail() throws Exception {
        Book book = new Book();
        bookRepository.save(book);

        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(createDummyImage(200, 300));
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photoRepository.save(photo);

        MvcResult result = mockMvc.perform(get("/api/photos/" + photo.getId() + "/thumbnail?width=100"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] thumbnailBytes = result.getResponse().getContentAsByteArray();
        BufferedImage thumbnailImage = ImageIO.read(new ByteArrayInputStream(thumbnailBytes));
        assertEquals(100, thumbnailImage.getWidth());
        assertEquals(150, thumbnailImage.getHeight());
    }

    @Test
    @WithMockUser
    void getImage_returnsFullImage() throws Exception {
        Book book = new Book();
        bookRepository.save(book);

        byte[] originalImage = createDummyImage(400, 600);
        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(originalImage);
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photoRepository.save(photo);

        MvcResult result = mockMvc.perform(get("/api/photos/" + photo.getId() + "/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE))
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        assertTrue(imageBytes.length > 0);
    }

    @Test
    @WithMockUser
    void getImage_notFound() throws Exception {
        mockMvc.perform(get("/api/photos/99999/image"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void deletePhoto_asLibrarian() throws Exception {
        Book book = new Book();
        bookRepository.save(book);

        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(createDummyImage(100, 100));
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photoRepository.save(photo);

        Long photoId = photo.getId();

        mockMvc.perform(delete("/api/photos/" + photoId))
                .andExpect(status().isOk());

        // Verify soft delete (photo should still exist but marked as deleted)
        Photo deletedPhoto = photoRepository.findById(photoId).orElse(null);
        assertNotNull(deletedPhoto);
    }

    @Test
    @WithMockUser(authorities = "USER")
    void deletePhoto_asRegularUser_forbidden() throws Exception {
        Book book = new Book();
        bookRepository.save(book);

        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(createDummyImage(100, 100));
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photoRepository.save(photo);

        mockMvc.perform(delete("/api/photos/" + photo.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void restorePhoto_asLibrarian() throws Exception {
        Book book = new Book();
        bookRepository.save(book);

        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(createDummyImage(100, 100));
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photoRepository.save(photo);

        mockMvc.perform(post("/api/photos/" + photo.getId() + "/restore"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void restorePhoto_asRegularUser_forbidden() throws Exception {
        Book book = new Book();
        bookRepository.save(book);

        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(createDummyImage(100, 100));
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photoRepository.save(photo);

        mockMvc.perform(post("/api/photos/" + photo.getId() + "/restore"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void cropPhoto_asLibrarian() throws Exception {
        Book book = new Book();
        bookRepository.save(book);

        byte[] originalImage = createDummyImage(400, 600);
        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(originalImage);
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photo.setPhotoOrder(0);
        photoRepository.save(photo);

        Long originalPhotoId = photo.getId();

        // Create a new image to upload as the cropped version
        byte[] croppedImage = createDummyImage(200, 300);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cropped.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                croppedImage
        );

        mockMvc.perform(multipart("/api/photos/" + originalPhotoId + "/crop")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk());

        // Verify the original photo was shifted to the right (order +1)
        Photo originalPhoto = photoRepository.findById(originalPhotoId).orElse(null);
        assertNotNull(originalPhoto);
        assertEquals(1, originalPhoto.getPhotoOrder());

        // Verify a new photo was created at the original position with the cropped image
        var allPhotos = photoRepository.findAll();
        assertEquals(2, allPhotos.size()); // Should have 2 photos now

        Photo newPhoto = allPhotos.stream()
                .filter(p -> p.getPhotoOrder() == 0)
                .findFirst()
                .orElse(null);
        assertNotNull(newPhoto);
        assertArrayEquals(croppedImage, newPhoto.getImage());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void cropPhoto_asRegularUser_forbidden() throws Exception {
        Book book = new Book();
        bookRepository.save(book);

        Photo photo = new Photo();
        photo.setBook(book);
        photo.setImage(createDummyImage(100, 100));
        photo.setContentType(MediaType.IMAGE_JPEG_VALUE);
        photoRepository.save(photo);

        byte[] croppedImage = createDummyImage(50, 50);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cropped.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                croppedImage
        );

        mockMvc.perform(multipart("/api/photos/" + photo.getId() + "/crop")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isForbidden());
    }
}