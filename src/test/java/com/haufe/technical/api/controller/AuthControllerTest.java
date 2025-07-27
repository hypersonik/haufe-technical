package com.haufe.technical.api.controller;

import com.haufe.technical.api.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(AuthController.class)
@Import(TestSecurityConfig.class)
@EnableReactiveMethodSecurity
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @WithMockUser(username = "testUser")
    void getCurrentUser_WithAuthenticatedUser_ReturnsUserDetails() {
        webTestClient.get()
                .uri("/auth/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Current user: testUser");
    }

    @Test
    void getCurrentUser_WithAnonymousUser_ReturnsAnonymous() {
        webTestClient.get()
                .uri("/auth/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Current user: anonymous");
    }
}
