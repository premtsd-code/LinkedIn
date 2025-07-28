package com.premtsd.linkedin.apigateway.filters;

import com.premtsd.linkedin.apigateway.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final List<String> OPEN_API_WHITELIST = List.of(
            "/v3/api-docs", "/swagger-ui", "/actuator"
    );

    private final JwtService jwtService;

    public AuthenticationFilter(JwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            log.debug("Processing authentication for path: {}", path);

            if (isWhitelisted(path)) {
                log.debug("Path {} is whitelisted, skipping authentication", path);
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Authorization header missing or invalid for path: {}", path);
                return onError(exchange, "Authorization header missing or invalid", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7).trim(); // safer than split
            log.debug("Validating JWT token for path: {}", path);

            try {
                String userId = jwtService.getUserIdFromToken(token);
                jwtService.validateTokenWithRole(token, config.requiredRole);
                log.info("Authentication successful for user: {} on path: {}", userId, path);

                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(builder -> builder.header("X-User-Id", userId))
                        .build();

                return chain.filter(modifiedExchange);
            } catch (JwtException e) {
                log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isWhitelisted(String path) {
        return OPEN_API_WHITELIST.stream().anyMatch(path::contains);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    public static class Config {
        private String requiredRole = "USER"; // default

        public String getRequiredRole() {
            return requiredRole;
        }

        public void setRequiredRole(String requiredRole) {
            this.requiredRole = requiredRole;
        }
    }
}
