package com.haufe.technical.api.service;

import com.haufe.technical.api.domain.dto.beer.BeerListResponseDto;
import com.haufe.technical.api.domain.dto.beer.BeerReadResponseDto;
import com.haufe.technical.api.domain.dto.beer.BeerUpsertDto;
import com.haufe.technical.api.domain.dto.beer.BeerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Beer;
import com.haufe.technical.api.exception.ApiException;
import com.haufe.technical.api.repository.BeerRepository;
import com.haufe.technical.api.repository.ManufacturerRepository;
import com.haufe.technical.api.utils.ReactiveSecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeerService {
    private final ManufacturerRepository manufacturerRepository;
    private final BeerRepository beerRepository;

    @Transactional
    public Mono<BeerUpsertResponseDto> create(Long manufacturerId, BeerUpsertDto request) throws ApiException {
        return manufacturerRepository.existsById(manufacturerId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Manufacturer with ID " + manufacturerId + " not found."));
                    }

                    final Beer beer = Beer.builder()
                            .name(request.name())
                            .abv(request.avb())
                            .style(request.style())
                            .description(request.description())
                            .manufacturerId(manufacturerId)
                            .build();

                    return beerRepository.save(beer);
                })
                .map(savedBeer -> {
                    log.atInfo().log(() -> "Created beer: " + savedBeer);
                    return new BeerUpsertResponseDto(savedBeer.getId(), savedBeer.getName());
                });
    }

    @Transactional
    public Mono<BeerUpsertResponseDto> update(Long id, BeerUpsertDto request) {
        return ReactiveSecurityUtils.getManufacturerId()
                .flatMap(manufacturerId -> update(id, request, manufacturerId.orElse(null)));
    }

    private Mono<BeerUpsertResponseDto> update(Long id, BeerUpsertDto request, Long manufacturerId) {
        return beerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Beer with ID " + id + " not found.")))
                .flatMap(beer -> {
                    // Check if the beer belongs to the manufacturer.
                    // If manufacturerId is null, it means the user is not a manufacturer.
                    // If the user is not a manufacturer and this code is reached,
                    // it means the user is an admin or has permission to update any beer.
                    if (manufacturerId != null && !beer.getManufacturerId().equals(manufacturerId)) {
                        return Mono.error(new ApiException(
                                HttpStatus.FORBIDDEN,
                                "You don't have permission to update this beer"));
                    }
                    return Mono.just(beer);
                })
                .flatMap(beer -> {
                    // Update fields only if they are not null or blank
                    if (StringUtils.isNotBlank(request.name()))         beer.setName(request.name());
                    if (request.avb() != null)                          beer.setAbv(request.avb());
                    if (StringUtils.isNotBlank(request.style()))        beer.setStyle(request.style());
                    if (StringUtils.isNotBlank(request.description()))  beer.setDescription(request.description());

                    return beerRepository.save(beer);
                })
                .map(savedBeer -> new BeerUpsertResponseDto(savedBeer.getId(), savedBeer.getName()))
                .doOnSuccess(savedBeer -> log.atInfo().log(() -> "Updated beer: " + savedBeer))
                .doOnError(exception -> log.error("Error updating beer with ID {}: {}", id, exception.getMessage()));
    }

    public Mono<BeerReadResponseDto> read(Long id) {
        return beerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND, "Beer with ID " + id + " not found.")))
                .map(beer -> new BeerReadResponseDto(
                        beer.getName(),
                        beer.getAbv(),
                        beer.getStyle(),
                        beer.getDescription()));
    }

    public Flux<BeerListResponseDto> list(Pageable pageable) {
        return beerRepository.findAllBy(pageable)
                .map(beer -> new BeerListResponseDto(
                        beer.getId(),
                        beer.getName(),
                        beer.getAbv(),
                        beer.getStyle(),
                        beer.getDescription()));
    }

    public Mono<Void> delete(Long id) {
        return ReactiveSecurityUtils.getManufacturerId()
                        .flatMap(manufacturerId -> delete(id, manufacturerId.orElse(null)));
    }

    private Mono<Void> delete(Long id, Long manufacturerId) {
        return beerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Beer with ID " + id + " not found.")))
                .flatMap(beer -> {
                    // Check if the beer belongs to the manufacturer.
                    // If manufacturerId is null, it means the user is not a manufacturer.
                    // If the user is not a manufacturer and this code is reached,
                    // it means the user is an admin or has permission to delete any beer.
                    if (manufacturerId != null && !beer.getManufacturerId().equals(manufacturerId)) {
                        return Mono.error(new ApiException(
                                HttpStatus.FORBIDDEN,
                                "You don't have permission to delete this beer"));
                    }
                    return Mono.just(beer);
                })
                .flatMap(beerRepository::delete)
                .doOnSuccess(aVoid -> log.info("Deleted beer with id: {}", id))
                .doOnError(exception -> log.error("Error deleting beer with ID {}: {}", id, exception.getMessage()));
    }
}
