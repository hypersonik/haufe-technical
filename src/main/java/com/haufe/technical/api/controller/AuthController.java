package com.haufe.technical.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {
    @GetMapping("/me")
    public Mono<ResponseEntity<String>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return Mono.just(ResponseEntity.ok()
                .body("Current user: " + (userDetails == null ? "anonymous" : userDetails.getUsername())));
    }
}
