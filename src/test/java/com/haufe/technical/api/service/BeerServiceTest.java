package com.haufe.technical.api.service;

import com.haufe.technical.api.domain.dto.beer.BeerUpsertDto;
import com.haufe.technical.api.domain.dto.beer.BeerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Beer;
import com.haufe.technical.api.exception.ApiException;
import com.haufe.technical.api.repository.BeerRepository;
import com.haufe.technical.api.repository.ManufacturerRepository;
import com.haufe.technical.api.utils.ReactiveSecurityUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeerServiceTest {
    @Mock
    private ManufacturerRepository manufacturerRepository;

    @Mock
    private BeerRepository beerRepository;

    @InjectMocks
    private BeerService beerService;

    @Nested
    class CreateTests {
        @Test
        void create_Success() throws ApiException {
            Long manufacturerId = 1L;
            BeerUpsertDto request = new BeerUpsertDto("Test Beer", 5.0F, "IPA", "Description");
            Beer beer = Beer.builder()
                    .id(1L)
                    .name(request.name())
                    .abv(request.abv())
                    .style(request.style())
                    .description(request.description())
                    .manufacturerId(manufacturerId)
                    .build();

            when(manufacturerRepository.existsById(manufacturerId)).thenReturn(Mono.just(true));
            when(beerRepository.save(any(Beer.class))).thenReturn(Mono.just(beer));

            StepVerifier.create(beerService.create(manufacturerId, request))
                    .expectNext(new BeerUpsertResponseDto(1L, "Test Beer"))
                    .verifyComplete();
        }

        @Test
        void create_ManufacturerNotFound() throws ApiException {
            Long manufacturerId = 1L;
            BeerUpsertDto request = new BeerUpsertDto("Test Beer", 5.0F, "IPA", "Description");

            when(manufacturerRepository.existsById(manufacturerId)).thenReturn(Mono.just(false));

            StepVerifier.create(beerService.create(manufacturerId, request))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    ((ApiException) throwable).getCode() == HttpStatus.NOT_FOUND)
                    .verify();
        }
    }

    @Nested
    class UpdateTests {
        @Test
        void update_Success() {
            Long beerId = 1L;
            Long manufacturerId = 1L;
            BeerUpsertDto request = new BeerUpsertDto("Updated Beer", 6.0F, "Stout", "New Description");
            Beer existingBeer = Beer.builder()
                    .id(beerId)
                    .manufacturerId(manufacturerId)
                    .build();
            Beer updatedBeer = Beer.builder()
                    .id(beerId)
                    .name(request.name())
                    .manufacturerId(manufacturerId)
                    .build();

            when(beerRepository.findById(beerId)).thenReturn(Mono.just(existingBeer));
            when(beerRepository.save(any(Beer.class))).thenReturn(Mono.just(updatedBeer));

            try (MockedStatic<ReactiveSecurityUtils> mockedStatic = Mockito.mockStatic(ReactiveSecurityUtils.class)) {
                mockedStatic.when(ReactiveSecurityUtils::getManufacturerId)
                        .thenReturn(Mono.just(Optional.of(manufacturerId)));

                StepVerifier.create(beerService.update(beerId, request))
                        .expectNext(new BeerUpsertResponseDto(beerId, "Updated Beer"))
                        .verifyComplete();
            }
        }

        @Test
        void update_Forbidden() {
            Long beerId = 1L;
            Long manufacturerId = 1L;
            Long wrongManufacturerId = 2L;
            BeerUpsertDto request = new BeerUpsertDto("Updated Beer", 6.0F, "Stout", "New Description");
            Beer existingBeer = Beer.builder()
                    .id(beerId)
                    .manufacturerId(manufacturerId)
                    .build();

            when(beerRepository.findById(beerId)).thenReturn(Mono.just(existingBeer));

            try (MockedStatic<ReactiveSecurityUtils> mockedStatic = Mockito.mockStatic(ReactiveSecurityUtils.class)) {
                mockedStatic.when(ReactiveSecurityUtils::getManufacturerId)
                        .thenReturn(Mono.just(Optional.of(wrongManufacturerId)));

                StepVerifier.create(beerService.update(beerId, request))
                        .expectErrorMatches(throwable ->
                                throwable instanceof ApiException &&
                                        ((ApiException) throwable).getCode() == HttpStatus.FORBIDDEN)
                        .verify();
            }
        }
    }

    @Nested
    class DeleteTests {
        @Test
        void delete_Success() {
            Long beerId = 1L;
            Long manufacturerId = 1L;
            Beer existingBeer = Beer.builder()
                    .id(beerId)
                    .manufacturerId(manufacturerId)
                    .build();

            when(beerRepository.findById(beerId)).thenReturn(Mono.just(existingBeer));
            when(beerRepository.delete(existingBeer)).thenReturn(Mono.empty());

            try (MockedStatic<ReactiveSecurityUtils> mockedStatic = Mockito.mockStatic(ReactiveSecurityUtils.class)) {
                mockedStatic.when(ReactiveSecurityUtils::getManufacturerId)
                        .thenReturn(Mono.just(Optional.of(manufacturerId)));

                StepVerifier.create(beerService.delete(beerId))
                        .verifyComplete();
            }
        }
    }

    @Nested
    class ReadTests {
        @Test
        void read_NotFound() {
            Long beerId = 1L;
            when(beerRepository.findById(beerId)).thenReturn(Mono.empty());

            StepVerifier.create(beerService.read(beerId))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    ((ApiException) throwable).getCode() == HttpStatus.NOT_FOUND)
                    .verify();
        }
    }
}
