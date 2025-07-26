package com.haufe.technical.api.service;

import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Manufacturer;
import com.haufe.technical.api.domain.entity.User;
import com.haufe.technical.api.exception.ApiException;
import com.haufe.technical.api.repository.BeerRepository;
import com.haufe.technical.api.repository.ManufacturerRepository;
import com.haufe.technical.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.BiFunction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManufacturerServiceTest {
    private static final Long THE_ID = 1L;
    public static final String THE_PASSWORD = "1234";

    @Mock
    private ManufacturerRepository manufacturerRepository;
    @Mock
    private BeerRepository beerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DatabaseClient databaseClient;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ManufacturerService manufacturerService;

    @Test
    void create_WhenManufacturerDoesNotExist_ShouldCreateSuccessfully() {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Test Brewery", "USA", THE_PASSWORD);
        User user = User.builder()
                .id(THE_ID)
                .name("Test Brewery")
                .password(THE_PASSWORD)
                .roles("MANUFACTURER")
                .build();
        Manufacturer manufacturer = Manufacturer.builder()
                .id(THE_ID)
                .country("USA")
                .build();

        when(userRepository.findByName("Test Brewery")).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
        when(manufacturerRepository.save(any(Manufacturer.class))).thenReturn(Mono.just(manufacturer));

        StepVerifier.create(manufacturerService.create(request))
                .expectNext(new ManufacturerUpsertResponseDto(THE_ID, "Test Brewery"))
                .verifyComplete();

        verify(userRepository).save(any(User.class));
        verify(manufacturerRepository).save(any(Manufacturer.class));
        verify(passwordEncoder).encode(THE_PASSWORD);
    }

    @Test
    void create_WhenManufacturerExists_ShouldReturnError() {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Test Brewery", "USA", THE_PASSWORD);
        User existingUser = User.builder()
                .id(THE_ID)
                .name("Test Brewery")
                .password(THE_PASSWORD)
                .roles("MANUFACTURER")
                .build();

        when(userRepository.findByName(existingUser.getName())).thenReturn(Mono.just(existingUser));

        StepVerifier.create(manufacturerService.create(request))
                .expectError(ApiException.class)
                .verify();

        verify(userRepository, never()).save(any(User.class));
        verify(manufacturerRepository, never()).save(any(Manufacturer.class));
        verify(passwordEncoder, never()).encode(THE_PASSWORD);
    }

    @Test
    void update_WhenManufacturerExists_ShouldUpdateSuccessfully() {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Updated Brewery", "ES", THE_PASSWORD);
        User existingUser = User.builder()
                .id(THE_ID + 1)
                .name("Test Brewery")
                .password(THE_PASSWORD)
                .roles("MANUFACTURER")
                .build();
        Manufacturer existingManufacturer = Manufacturer.builder()
                .id(THE_ID)
                .userId(THE_ID + 1) // Assuming userId is different
                .country("USA")
                .build();
        User updatedUser = existingUser.toBuilder()
                .name("Updated Brewery")
                .build();
        Manufacturer updatedManufacturer = existingManufacturer.toBuilder()
                .country("ES")
                .build();

        when(userRepository.findById(existingUser.getId())).thenReturn(Mono.just(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));
        when(manufacturerRepository.findById(existingManufacturer.getId())).thenReturn(Mono.just(existingManufacturer));
        when(manufacturerRepository.save(any(Manufacturer.class))).thenReturn(Mono.just(updatedManufacturer));
        when(userRepository.existsByNameAndIdNot(request.name(), existingUser.getId())).thenReturn(Mono.just(false));
        when(passwordEncoder.encode(THE_PASSWORD)).thenReturn(THE_PASSWORD);

        StepVerifier.create(manufacturerService.update(THE_ID, request))
                .expectNext(new ManufacturerUpsertResponseDto(updatedManufacturer.getId(), updatedUser.getName()))
                .verifyComplete();

        verify(userRepository).save(any(User.class));
        verify(manufacturerRepository).save(any(Manufacturer.class));
        verify(passwordEncoder).encode(THE_PASSWORD);
    }

    @Test
    void update_WhenManufacturerDoesNotExist_ShouldReturnError() {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Test Brewery", "USA", THE_PASSWORD);

        when(manufacturerRepository.findById(THE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(manufacturerService.update(THE_ID, request))
                .expectError(ApiException.class)
                .verify();

        verify(userRepository, never()).save(any(User.class));
        verify(manufacturerRepository, never()).save(any(Manufacturer.class));
        verify(passwordEncoder, never()).encode(THE_PASSWORD);
    }

    @Test
    void update_WhenManufacturerExistsButThereIsAnotherOneWithSameName_ShouldReturnError() {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Test Brewery", "USA", THE_PASSWORD);
        User existingUser = User.builder()
                .id(THE_ID + 1)
                .name("Test Brewery")
                .password(THE_PASSWORD)
                .roles("MANUFACTURER")
                .build();
        Manufacturer existingManufacturer = Manufacturer.builder()
                .id(THE_ID)
                .userId(THE_ID + 1) // Assuming userId is different
                .country("USA")
                .build();

        when(manufacturerRepository.findById(existingManufacturer.getId())).thenReturn(Mono.just(existingManufacturer));
        when(userRepository.findById(existingUser.getId())).thenReturn(Mono.just(existingUser));
        when(userRepository.existsByNameAndIdNot(request.name(), existingUser.getId())).thenReturn(Mono.just(true));

        StepVerifier.create(manufacturerService.update(THE_ID, request))
                .expectError(ApiException.class)
                .verify();

        verify(userRepository, never()).save(any(User.class));
        verify(manufacturerRepository, never()).save(any(Manufacturer.class));
        verify(passwordEncoder, never()).encode(THE_PASSWORD);
    }

    @Test
    void read_WhenManufacturerExists_ShouldReturnSuccessfully() {
        ManufacturerReadResponseDto expectedResponse =
                new ManufacturerReadResponseDto("Test Brewery", "USA");

        when(manufacturerRepository.findByIdWithName(THE_ID)).thenReturn(Mono.just(expectedResponse));

        StepVerifier.create(manufacturerService.read(THE_ID))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void read_WhenManufacturerDoesNotExist_ShouldReturnError() {
        when(manufacturerRepository.findByIdWithName(THE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(manufacturerService.read(THE_ID))
                .expectError(ApiException.class)
                .verify();
    }

    @Test
    void list_ShouldReturnPagedResults(@Mock DatabaseClient.GenericExecuteSpec executeSpec,
                                       @Mock RowsFetchSpec<ManufacturerListResponseDto> fetchSpec) {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("name"));
        ManufacturerListResponseDto mockManufacturer = new ManufacturerListResponseDto(1L, "Test Brewery", "USA");

        when(databaseClient.sql(anyString())).thenReturn(executeSpec);
        when(executeSpec.map(any(BiFunction.class))).thenReturn(fetchSpec);
        when(fetchSpec.all()).thenReturn(Flux.just(mockManufacturer));
        when(manufacturerRepository.count()).thenReturn(Mono.just(1L));

        StepVerifier.create(manufacturerService.list(pageable))
                .expectNextMatches(page -> page.getTotalElements() == 1L)
                .verifyComplete();
    }

    @Test
    void list_WhenNoManufacturers_ShouldReturnEmptyPage(@Mock DatabaseClient.GenericExecuteSpec executeSpec,
                                                        @Mock RowsFetchSpec<ManufacturerListResponseDto> fetchSpec) {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("name"));

        when(databaseClient.sql(anyString())).thenReturn(executeSpec);
        when(executeSpec.map(any(BiFunction.class))).thenReturn(fetchSpec);
        when(fetchSpec.all()).thenReturn(Flux.empty());
        when(manufacturerRepository.count()).thenReturn(Mono.just(0L));

        StepVerifier.create(manufacturerService.list(pageable))
                .expectNextMatches(page ->
                        page.getTotalElements() == 0 && page.getContent().isEmpty())
                .verifyComplete();
    }
    
    @Test
    void delete_WhenManufacturerHasNoBeers_ShouldDeleteSuccessfully() {
        when(manufacturerRepository.existsById(THE_ID)).thenReturn(Mono.just(true));
        when(beerRepository.existsByManufacturerId(THE_ID)).thenReturn(Mono.just(false));
        when(manufacturerRepository.deleteById(THE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(manufacturerService.delete(THE_ID))
                .verifyComplete();
    }

    @Test
    void delete_WhenManufacturerHasBeers_ShouldReturnError() {
        when(manufacturerRepository.existsById(THE_ID)).thenReturn(Mono.just(true));
        when(beerRepository.existsByManufacturerId(THE_ID)).thenReturn(Mono.just(true));

        StepVerifier.create(manufacturerService.delete(THE_ID))
                .expectError(ApiException.class)
                .verify();
    }

    @Test
    void delete_WhenManufacturerDoesNotExist_ShouldReturnError() {

        when(manufacturerRepository.existsById(THE_ID)).thenReturn(Mono.just(false));

        StepVerifier.create(manufacturerService.delete(THE_ID))
                .expectError(ApiException.class)
                .verify();
    }
}
