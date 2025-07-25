package com.haufe.technical.api.service;

import com.haufe.technical.api.auth.CustomUserDetails;
import com.haufe.technical.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements ReactiveUserDetailsService {
    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByNameAndEnabledIsTrue(username)
                .map(user -> CustomUserDetails.builder()
                        .id(user.getId())
                        .username(user.getName())
                        .password(user.getPassword())
                        .build()
                        .roles(StringUtils.split(user.getRoles(), ','))
                );
    }
}
