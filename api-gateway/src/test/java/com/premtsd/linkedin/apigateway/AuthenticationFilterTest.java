package com.premtsd.linkedin.apigateway;

import com.premtsd.linkedin.apigateway.filters.AuthenticationFilter;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@WebFluxTest
public class AuthenticationFilterTest {

    @MockBean
    private JwtService jwtService;

    private AuthenticationFilter authenticationFilter;

    private AuthenticationFilter.Config config;

    @BeforeEach
    void setUp() {
        authenticationFilter = new AuthenticationFilter(jwtService);
        config = new AuthenticationFilter.Config();
        config.setRequiredRole("USER");
    }

    @Test
    void shouldPassForWhitelistedPath() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/swagger-ui/index.html").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.apply(config).filter(exchange, chain);
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    void shouldFailForMissingAuthHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/secure-endpoint").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        Mono<Void> result = authenticationFilter.apply(config).filter(exchange, chain);
        StepVerifier.create(result).expectComplete().verify();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldFailForInvalidToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/secure-endpoint")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        doThrow(new JwtException("Invalid token")).when(jwtService).getUserIdFromToken(anyString());

        Mono<Void> result = authenticationFilter.apply(config).filter(exchange, chain);
        StepVerifier.create(result).expectComplete().verify();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldPassWithValidToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/secure-endpoint")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        when(jwtService.getUserIdFromToken("valid-token")).thenReturn("user-123");
        doNothing().when(jwtService).validateTokenWithRole("valid-token", "USER");

        Mono<Void> result = authenticationFilter.apply(config).filter(exchange, chain);
        StepVerifier.create(result).verifyComplete();
    }
}
