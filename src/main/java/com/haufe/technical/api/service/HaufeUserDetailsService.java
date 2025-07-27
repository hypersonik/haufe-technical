package com.haufe.technical.api.service;

import com.haufe.technical.api.auth.HaufeUserDetails;
import com.haufe.technical.api.domain.dto.user.UserWithManufacturerDto;
import com.haufe.technical.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class HaufeUserDetailsService implements ReactiveUserDetailsService {
    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByNameAndEnabled(username)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found or not enabled: " + username)))
                .map(user -> HaufeUserDetails.builder()
                        .id(user.id())
                        .manufacturerId(user.manufacturerId())
                        .username(user.name())
                        .password(user.password())
                        .build()
                        .roles(getRoles(user))
                );
    }

    private static String[] getRoles(UserWithManufacturerDto user) {
        return Arrays.stream(StringUtils.split(user.roles(), ','))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .toArray(String[]::new);
    }
}
