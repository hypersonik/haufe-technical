package com.haufe.technical.api.service;

import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Manufacturer;
import com.haufe.technical.api.domain.repository.BeerRepository;
import com.haufe.technical.api.domain.repository.ManufacturerRepository;
import com.haufe.technical.api.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManufacturerServiceTest {
    private static final Long THE_ID = 1L;

    @Mock
    private ManufacturerRepository manufacturerRepository;

    @Mock
    private BeerRepository beerRepository;

    @InjectMocks
    private ManufacturerService manufacturerService;

    @Test
    void create_WhenManufacturerDoesNotExist_ShouldCreateSuccessfully() {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Test Brewery", "USA");
        Manufacturer manufacturer = Manufacturer.builder()
                .id(THE_ID)
                .name("Test Brewery")
                .country("USA")
                .build();

        when(manufacturerRepository.findByName("Test Brewery")).thenReturn(Mono.empty());
        when(manufacturerRepository.save(any(Manufacturer.class))).thenReturn(Mono.just(manufacturer));

        StepVerifier.create(manufacturerService.create(request))
                .expectNext(new ManufacturerUpsertResponseDto(THE_ID, "Test Brewery"))
                .verifyComplete();
    }

    @Test
    void create_WhenManufacturerExists_ShouldReturnError() {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Test Brewery", "USA");
        Manufacturer existingManufacturer = Manufacturer.builder()
                .id(THE_ID)
                .name("Test Brewery")
                .country("USA")
                .build();

        when(manufacturerRepository.findByName("Test Brewery")).thenReturn(Mono.just(existingManufacturer));

        StepVerifier.create(manufacturerService.create(request))
                .expectError(ApiException.class)
                .verify();
    }

    @Test
    void update_WhenManufacturerExists_ShouldUpdateSuccessfully() {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Updated Brewery", "Spain");
        Manufacturer existingManufacturer = Manufacturer.builder()
                .id(THE_ID)
                .name("Test Brewery")
                .country("USA")
                .build();
        Manufacturer updatedManufacturer = Manufacturer.builder()
                .id(THE_ID)
                .name("Updated Brewery")
                .country("Spain")
                .build();

        when(manufacturerRepository.findById(THE_ID)).thenReturn(Mono.just(existingManufacturer));
        when(manufacturerRepository.save(any(Manufacturer.class))).thenReturn(Mono.just(updatedManufacturer));

        StepVerifier.create(manufacturerService.update(THE_ID, request))
                .expectNext(new ManufacturerUpsertResponseDto(THE_ID, "Updated Brewery"))
                .verifyComplete();
    }

    @Test
    void update_WhenManufacturerDoesNotExist_ShouldReturnError() {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Test Brewery", "USA");

        when(manufacturerRepository.findById(THE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(manufacturerService.update(THE_ID, request))
                .expectError(ApiException.class)
                .verify();
    }

    @Test
    void read_WhenManufacturerDoesNotExist_ShouldReturnError() {
        when(manufacturerRepository.findById(THE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(manufacturerService.read(THE_ID))
                .expectError(ApiException.class)
                .verify();
    }

    @Test
    void list_ShouldReturnPagedResults() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("name"));
        Manufacturer manufacturer = Manufacturer.builder()
                .id(THE_ID)
                .name("Test Brewery")
                .country("USA")
                .build();

        when(manufacturerRepository.findAll(pageable.getSort())).thenReturn(Flux.just(manufacturer));
        when(manufacturerRepository.count()).thenReturn(Mono.just(THE_ID));

        StepVerifier.create(manufacturerService.list(pageable))
                .expectNextMatches(page -> page.getTotalElements() == 1)
                .verifyComplete();
    }

    @Test
    void list_WhenNoManufacturers_ShouldReturnEmptyPage() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("name"));

        when(manufacturerRepository.findAll(pageable.getSort())).thenReturn(Flux.empty());
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
