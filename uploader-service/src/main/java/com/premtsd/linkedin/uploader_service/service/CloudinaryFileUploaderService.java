package com.premtsd.linkedin.uploader_service.service;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryFileUploaderService implements FileUploaderService {

    private final Cloudinary cloudinary;

    @Override
    public String upload(MultipartFile file) {
        log.info("Starting Cloudinary upload for file: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());
        try {
            log.debug("Converting file to bytes and uploading to Cloudinary");
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), Map.of());
            String url = uploadResult.get("secure_url").toString();
            log.info("Cloudinary upload successful - URL: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Cloudinary upload failed for file: {}. Error: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }
}
