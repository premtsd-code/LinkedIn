package com.premtsd.linkedin.postservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

//    @Bean
//    public Retry uploadImageRetry(RetryRegistry retryRegistry) {
//        return retryRegistry.retry("uploadImageRetry");
//    }
//
//    @Bean
//    public CircuitBreaker uploadImageCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
//        return circuitBreakerRegistry.circuitBreaker("uploadImageCircuitBreaker");
//    }
//
//    @Bean
//    public RateLimiter uploadImageRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
//        return rateLimiterRegistry.rateLimiter("uploadImageRateLimiter");
//    }
}
