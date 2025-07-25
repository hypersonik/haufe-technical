package com.haufe.technical.api.service;

import com.haufe.technical.api.domain.repository.ManufacturerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final ManufacturerRepository manufacturerRepository;

    private static final Map<String, UserDetails> STATIC_USERS = Map.of(
            "admin1", User.withUsername("admin1")
                    .password("{noop}1234")
                    .roles("ADMIN")
                    .build(),
            "admin2", User.withUsername("admin2")
                    .password("{noop}1234")
                    .roles("ADMIN")
                    .build()
    );

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return Optional.ofNullable(STATIC_USERS.get(username))
                .map(Mono::just)
                .orElseGet(() -> findByManufacturerName(username));
    }

    private Mono<UserDetails> findByManufacturerName(String name) {
        return manufacturerRepository.findByName(name)
                .map(manufacturer -> User.withUsername(manufacturer.getName())
                        .password("{noop}1234")
                        .roles("MANUFACTURER")
                        .build())
                .switchIfEmpty(Mono.empty());
    }
}
