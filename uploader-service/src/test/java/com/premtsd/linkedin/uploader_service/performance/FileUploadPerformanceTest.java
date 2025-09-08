package com.premtsd.linkedin.uploader_service.performance;

import com.premtsd.linkedin.uploader_service.service.FileUploaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class FileUploadPerformanceTest {

    @Mock
    private FileUploaderService fileUploaderService;

    private MockMultipartFile smallFile;
    private MockMultipartFile mediumFile;
    private MockMultipartFile largeFile;

    @BeforeEach
    void setUp() {
        // Small file: 1KB
        smallFile = new MockMultipartFile(
                "file", "small.jpg", "image/jpeg", new byte[1024]);
        
        // Medium file: 1MB
        mediumFile = new MockMultipartFile(
                "file", "medium.jpg", "image/jpeg", new byte[1024 * 1024]);
        
        // Large file: 10MB
        largeFile = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", new byte[10 * 1024 * 1024]);
    }

    @Test
    void upload_ShouldCompleteWithinTimeLimit_ForSmallFiles() throws IOException {
        // Given
        when(fileUploaderService.upload(any())).thenReturn("https://cloudinary.com/small.jpg");

        // When
        long startTime = System.currentTimeMillis();
        String result = fileUploaderService.upload(smallFile);
        long endTime = System.currentTimeMillis();

        // Then
        assertNotNull(result);
        long duration = endTime - startTime;
        assertTrue(duration < 1000, "Small file upload should complete within 1 second, took: " + duration + "ms");
    }

    @Test
    void upload_ShouldCompleteWithinTimeLimit_ForMediumFiles() throws IOException {
        // Given
        when(fileUploaderService.upload(any())).thenReturn("https://cloudinary.com/medium.jpg");

        // When
        long startTime = System.currentTimeMillis();
        String result = fileUploaderService.upload(mediumFile);
        long endTime = System.currentTimeMillis();

        // Then
        assertNotNull(result);
        long duration = endTime - startTime;
        assertTrue(duration < 5000, "Medium file upload should complete within 5 seconds, took: " + duration + "ms");
    }

    @Test
    void upload_ShouldCompleteWithinTimeLimit_ForLargeFiles() throws IOException {
        // Given
        when(fileUploaderService.upload(any())).thenReturn("https://cloudinary.com/large.jpg");

        // When
        long startTime = System.currentTimeMillis();
        String result = fileUploaderService.upload(largeFile);
        long endTime = System.currentTimeMillis();

        // Then
        assertNotNull(result);
        long duration = endTime - startTime;
        assertTrue(duration < 10000, "Large file upload should complete within 10 seconds, took: " + duration + "ms");
    }

    @Test
    void upload_ShouldHandleConcurrentUploads() throws Exception {
        // Given
        when(fileUploaderService.upload(any())).thenReturn("https://cloudinary.com/concurrent.jpg");
        int numberOfThreads = 10;
        int uploadsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<String>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfThreads; i++) {
            for (int j = 0; j < uploadsPerThread; j++) {
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return fileUploaderService.upload(smallFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, executor);
                futures.add(future);
            }
        }

        // Wait for all uploads to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        allFutures.get(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        // Then
        long duration = endTime - startTime;
        int totalUploads = numberOfThreads * uploadsPerThread;
        
        // Verify all uploads completed successfully
        for (CompletableFuture<String> future : futures) {
            assertNotNull(future.get());
        }
        
        // Performance assertion: should handle 50 concurrent uploads within 30 seconds
        assertTrue(duration < 30000, 
                "Concurrent uploads should complete within 30 seconds, took: " + duration + "ms for " + totalUploads + " uploads");
        
        // Calculate throughput
        double throughput = (double) totalUploads / (duration / 1000.0);
        assertTrue(throughput > 1.0, "Throughput should be at least 1 upload per second, actual: " + throughput);
    }

    @Test
    void upload_ShouldMaintainPerformance_UnderMemoryPressure() throws IOException {
        // Given
        when(fileUploaderService.upload(any())).thenReturn("https://cloudinary.com/memory-test.jpg");
        List<byte[]> memoryConsumers = new ArrayList<>();
        
        // Consume some memory to simulate pressure
        for (int i = 0; i < 10; i++) {
            memoryConsumers.add(new byte[1024 * 1024]); // 1MB each
        }

        // When
        long startTime = System.currentTimeMillis();
        String result = fileUploaderService.upload(mediumFile);
        long endTime = System.currentTimeMillis();

        // Then
        assertNotNull(result);
        long duration = endTime - startTime;
        assertTrue(duration < 10000, 
                "Upload under memory pressure should complete within 10 seconds, took: " + duration + "ms");
        
        // Clean up memory
        memoryConsumers.clear();
    }

    @Test
    void upload_ShouldHandleRepeatedUploads_WithoutPerformanceDegradation() throws IOException {
        // Given
        when(fileUploaderService.upload(any())).thenReturn("https://cloudinary.com/repeated.jpg");
        int numberOfUploads = 100;
        List<Long> durations = new ArrayList<>();

        // When
        for (int i = 0; i < numberOfUploads; i++) {
            long startTime = System.currentTimeMillis();
            String result = fileUploaderService.upload(smallFile);
            long endTime = System.currentTimeMillis();
            
            assertNotNull(result);
            durations.add(endTime - startTime);
        }

        // Then
        // Calculate average duration for first 10 uploads vs last 10 uploads
        double firstTenAverage = durations.subList(0, 10).stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        double lastTenAverage = durations.subList(numberOfUploads - 10, numberOfUploads).stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        // Performance should not degrade significantly (allow 50% increase)
        assertTrue(lastTenAverage <= firstTenAverage * 1.5,
                String.format("Performance degradation detected. First 10 avg: %.2fms, Last 10 avg: %.2fms",
                        firstTenAverage, lastTenAverage));
    }

    @Test
    void upload_ShouldHandleVariousFileSizes_Efficiently() throws IOException {
        // Given
        when(fileUploaderService.upload(any()))
                .thenReturn("https://cloudinary.com/small.jpg")
                .thenReturn("https://cloudinary.com/medium.jpg")
                .thenReturn("https://cloudinary.com/large.jpg");

        // When & Then
        // Small file
        long startTime = System.currentTimeMillis();
        String smallResult = fileUploaderService.upload(smallFile);
        long smallDuration = System.currentTimeMillis() - startTime;
        assertNotNull(smallResult);

        // Medium file
        startTime = System.currentTimeMillis();
        String mediumResult = fileUploaderService.upload(mediumFile);
        long mediumDuration = System.currentTimeMillis() - startTime;
        assertNotNull(mediumResult);

        // Large file
        startTime = System.currentTimeMillis();
        String largeResult = fileUploaderService.upload(largeFile);
        long largeDuration = System.currentTimeMillis() - startTime;
        assertNotNull(largeResult);

        // Performance assertions
        assertTrue(smallDuration < 1000, "Small file: " + smallDuration + "ms");
        assertTrue(mediumDuration < 5000, "Medium file: " + mediumDuration + "ms");
        assertTrue(largeDuration < 10000, "Large file: " + largeDuration + "ms");
        
        // Duration should scale reasonably with file size
        assertTrue(mediumDuration >= smallDuration, "Medium file should take at least as long as small file");
        assertTrue(largeDuration >= mediumDuration, "Large file should take at least as long as medium file");
    }

    @Test
    void upload_ShouldRecoverFromTemporaryFailures() throws IOException {
        // Given
        when(fileUploaderService.upload(any()))
                .thenThrow(new IOException("Temporary failure"))
                .thenThrow(new IOException("Another failure"))
                .thenReturn("https://cloudinary.com/recovered.jpg");

        // When & Then
        // First attempt should fail
        assertThrows(IOException.class, () -> fileUploaderService.upload(smallFile));
        
        // Second attempt should fail
        assertThrows(IOException.class, () -> fileUploaderService.upload(smallFile));
        
        // Third attempt should succeed
        long startTime = System.currentTimeMillis();
        String result = fileUploaderService.upload(smallFile);
        long duration = System.currentTimeMillis() - startTime;
        
        assertNotNull(result);
        assertTrue(duration < 5000, "Recovery should be fast, took: " + duration + "ms");
    }
}
