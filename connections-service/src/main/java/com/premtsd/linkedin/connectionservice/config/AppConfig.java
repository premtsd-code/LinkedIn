package com.premtsd.linkedin.connectionservice.config;

import feign.Capability;
import feign.micrometer.MicrometerCapability;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Application-wide configuration for metrics and OpenAPI documentation.
 */
@Configuration
public class AppConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Enables Micrometer capability for Feign clients.
     */
    @Bean
    public Capability feignMicrometerCapability(MeterRegistry registry) {
        return new MicrometerCapability(registry);
    }

    /**
     * Configures the OpenAPI documentation with JWT-based authentication.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Connection Service API")
                        .version("1.0")
                        .description("API for managing LinkedIn-style user connections"))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api/v1/connections")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))); // Recommended for Swagger UI clarity
    }
}
