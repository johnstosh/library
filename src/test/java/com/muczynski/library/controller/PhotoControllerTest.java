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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}