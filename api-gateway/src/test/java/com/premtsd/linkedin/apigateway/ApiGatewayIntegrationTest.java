package com.premtsd.linkedin.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiGatewayIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturn401WhenMissingToken() {
        webTestClient.get()
                .uri("http://localhost:" + port + "/api/v1/posts")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401WhenInvalidToken() {
        webTestClient.get()
                .uri("http://localhost:" + port + "/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldPassWithValidToken() {
        String fakeJwt = generateFakeJwt();

        webTestClient.get()
                .uri("http://localhost:" + port + "/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fakeJwt)
                .exchange()
                .expectStatus().isOk(); // ✅ downstream must return 200 for full integration
    }

    private String generateFakeJwt() {
        // Replace this with a valid JWT if real validation is active
        return "eyJhbGciOiJIUzI1NiJ9." +
                "eyJzdWIiOiJ1c2VyLTEyMyIsInJvbGVzIjpbIlVTRVIiXSwiaWF0IjoxNjk5OTk5OTk5LCJleHAiOjI0OTk5OTk5OTl9." +
                "O0m9vU3ptnEDe_AgRLpqC-h7xvoZDyNBnpk02eZb7D8";
    }
}
