package com.premtsd.linkedin.apigateway.filters;

import com.premtsd.linkedin.apigateway.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtService jwtService;

//    @Retention(RetentionPolicy.RUNTIME)
//    @Target(ElementType.METHOD)
//    @PreAuthorize("@jwtAuthorizationService.hasPermission(authentication, #requiredRole)")
//    public @interface JwtPreAuthorize {
//        String requiredRole();
//    }

    private static final List<String> openApiWhitelist = List.of(
            "/v3/api-docs", "/swagger-ui", "/actuator"
    );



    public AuthenticationFilter(JwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService;
    }

    private boolean isWhitelisted(String path) {
        return openApiWhitelist.stream().anyMatch(path::contains);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            log.info("Login request: {}", exchange.getRequest().getURI());

            final String tokenHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            String path = exchange.getRequest().getURI().getPath();

            if (isWhitelisted(path)) {
                return chain.filter(exchange); // Skip auth
            }

            if(tokenHeader == null || !tokenHeader.startsWith("Bearer")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                log.error("Authorization token header not found");
                return exchange.getResponse().setComplete();
            }

            final String token = tokenHeader.split("Bearer ")[1];

            try {
                String userId = jwtService.getUserIdFromToken(token);
                ServerWebExchange modifiedExchange = exchange
                        .mutate()
                        .request(r -> r.header("X-User-Id", userId))
                        .build();

                jwtService.validateTokenWithRole(token,"USER");

                return chain.filter(modifiedExchange);
            } catch (JwtException e) {
                log.error("JWT Exception: {}", e.getLocalizedMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    public static class Config {
    }
}
