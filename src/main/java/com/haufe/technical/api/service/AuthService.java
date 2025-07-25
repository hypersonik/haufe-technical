package com.haufe.technical.api.service;

import com.haufe.technical.api.controller.dto.auth.LoginRequest;
import com.haufe.technical.api.domain.repository.ManufacturerRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String SECRET_KEY = "longSecretForDemonstrationPurposes";
    private static final String TOKEN_PREFIX = "Bearer";
    private static final Duration TOKEN_DURATION = Duration.ofHours(1);


    private final ManufacturerRepository manufacturerRepository;

    public Optional<Map<String, String>> authenticate(LoginRequest request) {
        if (request.username() == null || request.password() == null) {
            return Optional.empty();
        }
        return authenticateUser(request) ?
                Optional.of(Map.of(
                        "access_token", generateJwtToken(request),
                        "token_type", TOKEN_PREFIX,
                        "expires_in", TOKEN_DURATION.toSeconds() + ""
                )) : Optional.empty();
    }

    /**
     * Simplified authentication method based on the provided login request.
     *
     * @param request The login request containing username, password, and role.
     * @return true if authentication is successful, false otherwise.
     */
    private boolean authenticateUser(LoginRequest request) {
        if ("anonymous".equals(request.role()) || "admin".equals(request.username()) && "1234".equals(request.password())) {
            return true;
        }
        manufacturerRepository.findByName(request.username()).

        return manufacturerRepository.findByName(request.username()).blockOptional()
                .map(manufacturer -> "1234".equals(request.password()))
                .orElse(false);
    }

    private String generateJwtToken(LoginRequest request) {
        return Jwts.builder()
                .subject(request.username())
                .claim("role", request.role())
                .issuedAt(new Date())
                .expiration(new Date(Instant.now().plus(TOKEN_DURATION).toEpochMilli()))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
