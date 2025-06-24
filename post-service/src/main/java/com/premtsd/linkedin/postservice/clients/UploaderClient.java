package com.premtsd.linkedin.postservice.clients;

import com.premtsd.linkedin.postservice.dto.PersonDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "uploader-service", path = "/uploads")
public interface UploaderClient {

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String uploadFile(@RequestPart("file") MultipartFile file);
}