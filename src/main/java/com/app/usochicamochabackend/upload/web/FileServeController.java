package com.app.usochicamochabackend.upload.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/uploads")
public class FileServeController {

    @GetMapping("/{category}/{subcategory}/{filename}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String category,
            @PathVariable String subcategory,
            @PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads", category, subcategory, filename);
            Resource resource = new FileSystemResource(filePath.toFile());

            if (!resource.exists()) {
                log.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            String mediaType = guessMediaType(filename);
            log.debug("Serving file: {}", filePath);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error serving file: {}/{}/{}", category, subcategory, filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String guessMediaType(String filename) {
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else if (filename.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (filename.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        } else if (filename.endsWith(".webp")) {
            return "image/webp";
        } else if (filename.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF_VALUE;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
