package com.haufe.technical.api.controller;

import com.haufe.technical.api.controller.dto.auth.LoginRequest;
import com.haufe.technical.api.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, String>>> login(
            @RequestBody LoginRequest request) {
        return authService.authenticate(request)
                .map(jwt -> Mono.just(ResponseEntity.ok(jwt)))
                .orElseGet(() -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @GetMapping("/current-user")
    public Mono<ResponseEntity<String>> currentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        return Mono.just(userDetails)
                .map(ud -> ResponseEntity.ok("Current user: " + ud.getUsername()))
                .defaultIfEmpty(ResponseEntity.ok("Current user: anonymous"));
    }
}
