package com.premtsd.linkedin.postservice.service;

import com.premtsd.linkedin.postservice.clients.UploaderClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploaderServiceWrapper {

    private final UploaderClient uploaderClient;


//    @RateLimiter(name = "uploadImageRateLimiter", fallbackMethod = "fallbackRateLimit")
    @CircuitBreaker(name = "uploadImageCircuitBreaker", fallbackMethod = "fallbackUpload")
    @Retry(name = "uploadImageRetry", fallbackMethod = "fallbackRetry")
    public String uploadFile(MultipartFile file) {
        log.info("Calling uploader-service for image upload...");
        return uploaderClient.uploadFile(file);
    }

    public String fallbackUpload(MultipartFile file, Throwable t) {
        log.info("Uploader fallback CircuitBreaker triggered: {}", t.getMessage());
        return "https://fallback.url/CircuitBreaker.png";
    }

    public String fallbackRetry(MultipartFile file, Throwable t) {
        log.info("Uploader fallback Retry triggered: {}", t.getMessage());
        return "https://fallback.url/Retry.png";
    }

    public String fallbackRateLimit(MultipartFile file, Throwable t) {
        log.info("Uploader ratelimit triggered: {}", t.getMessage());
        return "https://fallback.url/ratelimit.png";
    }
}
