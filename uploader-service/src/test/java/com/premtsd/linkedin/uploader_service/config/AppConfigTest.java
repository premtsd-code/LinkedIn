package com.premtsd.linkedin.uploader_service.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    private final appConfig config = new appConfig();

    @Test
    void customOpenAPI_ShouldReturnConfiguredOpenAPI() {
        // When
        OpenAPI openAPI = config.customOpenAPI();

        // Then
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Uploader Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(openAPI.getServers()).isNotEmpty();
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
    }

    @Test
    void capability_ShouldReturnMicrometerCapability() {
        // Given
        MeterRegistry registry = new SimpleMeterRegistry();

        // When
        var capability = config.capability(registry);

        // Then
        assertThat(capability).isNotNull();
    }

    @Test
    void openAPI_ShouldHaveSecurityRequirement() {
        // When
        OpenAPI openAPI = config.customOpenAPI();

        // Then
        assertThat(openAPI.getSecurity()).isNotEmpty();
        assertThat(openAPI.getSecurity().get(0).get("bearerAuth")).isNotNull();
    }
}
