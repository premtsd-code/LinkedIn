package com.premtsd.linkedin.connectionservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public KafkaTemplate<Long, Object> kafkaTemplate() {
        KafkaTemplate<Long, Object> mockTemplate = mock(KafkaTemplate.class);
        SendResult<Long, Object> mockResult = mock(SendResult.class);
        CompletableFuture<SendResult<Long, Object>> future = CompletableFuture.completedFuture(mockResult);
        
        when(mockTemplate.send(anyString(), any(), any())).thenReturn(future);
        
        return mockTemplate;
    }
}
