package com.muczynski.library.controller;

import com.muczynski.library.domain.Photo;
import com.muczynski.library.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id, @RequestParam Integer width) {
        Pair<byte[], String> thumbnailData = photoService.getThumbnail(id, width);
        if (thumbnailData == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(thumbnailData.getSecond()))
                .body(thumbnailData.getFirst());
    }
}
