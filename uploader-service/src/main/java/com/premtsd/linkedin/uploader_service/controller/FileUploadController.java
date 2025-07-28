package com.premtsd.linkedin.uploader_service.controller;

import com.premtsd.linkedin.uploader_service.service.FileUploaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileUploaderService fileUploaderService;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file", description = "Uploads a file and returns its Cloudinary URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload successful"),
            @ApiResponse(responseCode = "500", description = "Upload failed")
    })
    public ResponseEntity<String> uploadImage(
            @Parameter(description = "The file to upload")
            @RequestPart("file") MultipartFile file) throws Exception {

        log.info("Received file upload request - filename: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());
        try {
            String url = fileUploaderService.upload(file);
            log.info("File uploaded successfully - URL: {}", url);
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            log.error("File upload failed for file: {}. Error: {}", file.getOriginalFilename(), e.getMessage());
            throw e;
        }
    }
}
