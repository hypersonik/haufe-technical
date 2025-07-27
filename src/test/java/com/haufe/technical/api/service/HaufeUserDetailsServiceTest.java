package com.haufe.technical.api.service;

import com.haufe.technical.api.auth.HaufeUserDetails;
import com.haufe.technical.api.domain.dto.user.UserWithManufacturerDto;
import com.haufe.technical.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HaufeUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HaufeUserDetailsService userDetailsService;

    @Test
    void findByUsername_shouldReturnUserDetailsWithCorrectAttributes() {
        // Given
        UserWithManufacturerDto userDto = new UserWithManufacturerDto(
                1L,
                100L,
                "testuser",
                "{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMy...",
                "ADMIN,MANUFACTURER",
                true
        );

        when(userRepository.findByNameAndEnabled("testuser"))
                .thenReturn(Mono.just(userDto));

        // When
        Mono<UserDetails> result = userDetailsService.findByUsername("testuser");

        // Then
        StepVerifier.create(result)
                .expectNextMatches(userDetails -> {
                    // Verify basic attributes
                    if (!userDetails.getUsername().equals("testuser")) return false;
                    if (!userDetails.getPassword().equals(userDto.password())) return false;

                    // Verify roles
                    Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
                    if (authorities.size() != 2) return false;

                    // Verify manufacturerId if it is HaufeUserDetails
                    if (userDetails instanceof HaufeUserDetails haufeDetails) {
                        if (!haufeDetails.getManufacturerId().equals(userDto.manufacturerId())) return false;
                        if (!haufeDetails.getId().equals(userDto.id())) return false;
                    }

                    return true;
                })
                .verifyComplete();
    }

    @Test
    void findByUsername_shouldThrowWhenUserNotFoundOrDisabled() {
        // Given
        when(userRepository.findByNameAndEnabled("unknown"))
                .thenReturn(Mono.empty());

        // When
        Mono<UserDetails> result = userDetailsService.findByUsername("unknown");

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("User not found or not enabled: unknown"))
                .verify();
    }

    @Test
    void findByUsername_shouldCorrectlyMapRoles() {
        // Given
        UserWithManufacturerDto userDto = new UserWithManufacturerDto(
                2L,
                null,
                "admin",
                "{noop}adminpass",
                "ADMIN,  USER",  // Roles with spaces
                true
        );

        when(userRepository.findByNameAndEnabled("admin"))
                .thenReturn(Mono.just(userDto));

        // When
        Mono<UserDetails> result = userDetailsService.findByUsername("admin");

        // Then
        StepVerifier.create(result)
                .expectNextMatches(userDetails -> userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet())
                        .containsAll(Set.of("ROLE_ADMIN", "ROLE_USER")))
                .verifyComplete();
    }

    @Test
    void findByUsername_shouldHandleUserWithoutManufacturer() {
        // Given
        UserWithManufacturerDto userDto = new UserWithManufacturerDto(
                3L,
                null,  // Sin manufacturer
                "user",
                "{noop}password",
                "USER",
                true
        );

        when(userRepository.findByNameAndEnabled("user"))
                .thenReturn(Mono.just(userDto));

        // When
        Mono<UserDetails> result = userDetailsService.findByUsername("user");

        // Then
        StepVerifier.create(result)
                .expectNextMatches(userDetails ->
                        userDetails instanceof HaufeUserDetails &&
                                ((HaufeUserDetails) userDetails).getManufacturerId() == null)
                .verifyComplete();
    }
}
