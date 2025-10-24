package com.muczynski.library.controller;

import com.muczynski.library.domain.Photo;
import com.muczynski.library.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        byte[] image = photoService.getImage(id);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        Photo photo = photoService.getPhotoById(id);
        String contentType = photo != null ? photo.getContentType() : MediaType.IMAGE_JPEG_VALUE;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(image);
    }
}
