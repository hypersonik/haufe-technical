package com.haufe.technical.api.service;

import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.domain.entity.Manufacturer;
import com.haufe.technical.api.domain.entity.User;
import com.haufe.technical.api.exception.ApiException;
import com.haufe.technical.api.repository.BeerRepository;
import com.haufe.technical.api.repository.ManufacturerRepository;
import com.haufe.technical.api.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManufacturerServiceTest {

    @Mock
    private ManufacturerRepository manufacturerRepository;

    @Mock
    private BeerRepository beerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ManufacturerService manufacturerService;

    @Nested
    class CreateManufacturer {
        @Test
        void shouldCreateManufacturerWithUser() {
            ManufacturerUpsertDto request = new ManufacturerUpsertDto(
                    "brewery_user", "password123", true, "Best Brewery", "Spain");

            User savedUser = User.builder()
                    .id(1L)
                    .name(request.userName())
                    .enabled(request.userEnabled())
                    .build();

            Manufacturer savedManufacturer = Manufacturer.builder()
                    .id(1L)
                    .userId(1L)
                    .name(request.name())
                    .country(request.country())
                    .build();

            when(userRepository.findByName(request.userName())).thenReturn(Mono.empty());
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
            when(manufacturerRepository.save(any(Manufacturer.class)))
                    .thenReturn(Mono.just(savedManufacturer));
            when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

            StepVerifier.create(manufacturerService.create(request))
                    .expectNextMatches(dto ->
                            dto.id().equals(1L) &&
                                    dto.name().equals(request.name()))
                    .verifyComplete();

            verify(userRepository).save(any(User.class));
            verify(manufacturerRepository).save(any(Manufacturer.class));
            verify(passwordEncoder).encode(request.password());
        }

        @Test
        void shouldFailWhenUsernameExists() {
            ManufacturerUpsertDto request = new ManufacturerUpsertDto(
                    "existing_user", "pass", true, "Brewery", "Germany");

            when(userRepository.findByName(request.userName())).thenReturn(Mono.just(User.builder().build()));

            StepVerifier.create(manufacturerService.create(request))
                    .verifyErrorSatisfies(throwable -> {
                        assertThat(throwable).isInstanceOf(ApiException.class);
                        ApiException apiException = (ApiException) throwable;
                        assertThat(apiException.getCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });

            verify(userRepository, never()).save(any(User.class));
            verify(manufacturerRepository, never()).save(any(Manufacturer.class));
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    @Nested
    class UpdateManufacturer {
        @Test
        void shouldUpdateAllFields() {
            ManufacturerUpsertDto request = new ManufacturerUpsertDto(
                    "new_user", "newpass", false, "Updated Brewery", "France");

            Manufacturer existingManufacturer = Manufacturer.builder()
                    .id(1L)
                    .userId(1L)
                    .name("Old Brewery")
                    .country("Spain")
                    .build();

            User existingUser = User.builder()
                    .id(1L)
                    .name("old_user")
                    .password("oldpass")
                    .enabled(true)
                    .build();

            when(manufacturerRepository.findById(1L)).thenReturn(Mono.just(existingManufacturer));
            when(userRepository.findById(1L)).thenReturn(Mono.just(existingUser));
            when(userRepository.existsByNameAndIdNot("new_user", 1L)).thenReturn(Mono.just(false));
            when(passwordEncoder.encode("newpass")).thenReturn("encodedNewPass");
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(existingUser));
            when(manufacturerRepository.save(any(Manufacturer.class)))
                    .thenReturn(Mono.just(existingManufacturer));

            StepVerifier.create(manufacturerService.update(1L, request))
                    .expectNextCount(1)
                    .verifyComplete();

            verify(userRepository).save(any(User.class));
            verify(manufacturerRepository).save(any(Manufacturer.class));
            verify(passwordEncoder).encode(request.password());
        }

        @Test
        void shouldFailWhenManufacturerNotFound() {
            when(manufacturerRepository.findById(1L)).thenReturn(Mono.empty());

            StepVerifier.create(manufacturerService.update(1L,
                            new ManufacturerUpsertDto("user", "pass", true, "name", "country")))
                    .verifyErrorSatisfies(throwable -> {
                        assertThat(throwable).isInstanceOf(ApiException.class);
                        ApiException apiException = (ApiException) throwable;
                        assertThat(apiException.getCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(userRepository, never()).save(any(User.class));
            verify(manufacturerRepository, never()).save(any(Manufacturer.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        void shouldFailWhenUsernameAlreadyExists() {
            ManufacturerUpsertDto request = new ManufacturerUpsertDto(
                    "existing_user",
                    "newpass",
                    true,
                    "Brewery",
                    "Germany"
            );

            Manufacturer existingManufacturer = Manufacturer.builder()
                    .id(1L)
                    .userId(1L)  // ID del usuario actual
                    .name("Original Brewery")
                    .country("Spain")
                    .build();

            User existingUser = User.builder()
                    .id(1L)
                    .name("original_user")
                    .build();

            // Mock setup
            when(manufacturerRepository.findById(existingManufacturer.getId()))
                    .thenReturn(Mono.just(existingManufacturer));
            when(userRepository.findById(existingManufacturer.getUserId()))
                    .thenReturn(Mono.just(existingUser));

            // Simulate that the user with the new name already exists and is not the current user
            when(userRepository.existsByNameAndIdNot(request.userName(), existingManufacturer.getUserId()))
                    .thenReturn(Mono.just(true));

            // Execution and verification
            StepVerifier.create(manufacturerService.update(1L, request))
                    .verifyErrorSatisfies(throwable -> {
                        assertThat(throwable).isInstanceOf(ApiException.class);
                        ApiException apiException = (ApiException) throwable;
                        assertThat(apiException.getCode()).isEqualTo(HttpStatus.CONFLICT);
                    });

            // Verify that no save operations were called
            verify(userRepository, never()).save(any());
            verify(manufacturerRepository, never()).save(any());
        }
    }

    @Nested
    class ReadManufacturer {
        @Test
        void shouldReturnManufacturerDetails() {
            Manufacturer manufacturer = Manufacturer.builder()
                    .id(1L)
                    .name("Best Brewery")
                    .country("Belgium")
                    .build();

            when(manufacturerRepository.findById(1L)).thenReturn(Mono.just(manufacturer));

            StepVerifier.create(manufacturerService.read(1L))
                    .expectNextMatches(dto ->
                            dto.name().equals(manufacturer.getName()) &&
                                    dto.country().equals(manufacturer.getCountry()))
                    .verifyComplete();

            verify(manufacturerRepository).findById(1L);
        }

        @Test
        void shouldFailWhenNotFound() {
            when(manufacturerRepository.findById(1L)).thenReturn(Mono.empty());

            StepVerifier.create(manufacturerService.read(1L))
                    .verifyErrorSatisfies(throwable -> {
                        assertThat(throwable).isInstanceOf(ApiException.class);
                        ApiException apiException = (ApiException) throwable;
                        assertThat(apiException.getCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(manufacturerRepository).findById(1L);
        }
    }

    @Nested
    class DeleteManufacturer {
        @Test
        void shouldDeleteWhenNoBeersExist() {
            when(manufacturerRepository.existsById(1L)).thenReturn(Mono.just(true));
            when(beerRepository.existsByManufacturerId(1L)).thenReturn(Mono.just(false));
            when(manufacturerRepository.deleteById(1L)).thenReturn(Mono.empty());

            StepVerifier.create(manufacturerService.delete(1L))
                    .verifyComplete();

            verify(manufacturerRepository).deleteById(1L);
        }

        @Test
        void shouldFailWhenBeersExist() {
            when(manufacturerRepository.existsById(1L)).thenReturn(Mono.just(true));
            when(beerRepository.existsByManufacturerId(1L)).thenReturn(Mono.just(true));

            StepVerifier.create(manufacturerService.delete(1L))
                    .expectError(ApiException.class)
                    .verify();

            verify(manufacturerRepository, never()).deleteById(1L);
        }
    }

    @Nested
    class ListManufacturers {
        @Test
        void shouldReturnPaginatedResults() {
            PageRequest pageable = PageRequest.of(0, 10, Sort.by("name"));

            Manufacturer manufacturer = Manufacturer.builder()
                    .id(1L)
                    .name("Test Brewery")
                    .country("USA")
                    .build();

            when(manufacturerRepository.findAll(pageable.getSort())).thenReturn(Flux.just(manufacturer));
            when(manufacturerRepository.count()).thenReturn(Mono.just(1L));

            StepVerifier.create(manufacturerService.list(pageable))
                    .expectNextMatches(page -> page.getTotalElements() == 1)
                    .verifyComplete();

            verify(manufacturerRepository).findAll(pageable.getSort());
            verify(manufacturerRepository).count();
        }
    }
}
