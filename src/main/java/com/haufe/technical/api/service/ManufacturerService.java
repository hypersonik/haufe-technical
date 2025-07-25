package com.haufe.technical.api.service;

import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Manufacturer;
import com.haufe.technical.api.domain.repository.BeerRepository;
import com.haufe.technical.api.domain.repository.ManufacturerRepository;
import com.haufe.technical.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManufacturerService {
    private final ManufacturerRepository manufacturerRepository;
    private final BeerRepository beerRepository;

    @Transactional
    public Mono<ManufacturerUpsertResponseDto> create(ManufacturerUpsertDto request) {
        return manufacturerRepository.findByName(request.name())
                .flatMap(existingManufacturer ->
                        Mono.<ManufacturerUpsertResponseDto>error(
                                new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "Manufacturer with name " + request.name() + " already exists")))
                .switchIfEmpty(Mono.defer(() -> createNew(request)));
    }

    private Mono<ManufacturerUpsertResponseDto> createNew(ManufacturerUpsertDto request) {
        Manufacturer manufacturer = Manufacturer.builder()
                .name(request.name())
                .country(request.country())
                .build();

        return manufacturerRepository.save(manufacturer)
                .map(savedManufacturer ->
                        new ManufacturerUpsertResponseDto(savedManufacturer.getId(), savedManufacturer.getName()))
                .doOnSuccess(savedManufacturer ->
                        log.atInfo().log(() -> "Created manufacturer: " + savedManufacturer))
                .doOnError(exception ->
                        log.error("Error creating manufacturer: {}", exception.getMessage()));
    }

    @Transactional
    public Mono<ManufacturerUpsertResponseDto> update(Long id, ManufacturerUpsertDto request) {
        return manufacturerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Manufacturer with id " + id + " not found")))
                .flatMap(manufacturer -> {
                    // Update fields only if they are not null
                    if (request.name() != null) {
                        manufacturer.setName(request.name());
                    }
                    if (request.country() != null) {
                        manufacturer.setCountry(request.country());
                    }
                    return manufacturerRepository.save(manufacturer);
                })
                .map(savedManufacturer ->
                        new ManufacturerUpsertResponseDto(savedManufacturer.getId(), savedManufacturer.getName()))
                .doOnSuccess(savedManufacturer ->
                        log.atInfo().log(() -> "Updated manufacturer: " + savedManufacturer))
                .doOnError(exception ->
                        log.error("Error updating manufacturer with id {}: {}", id, exception.getMessage()));
    }

    public Mono<ManufacturerReadResponseDto> read(Long id) {
        return manufacturerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Manufacturer with id " + id + " not found")))
                .map(manufacturer ->
                        new ManufacturerReadResponseDto(manufacturer.getName(), manufacturer.getCountry()));
    }

    public Mono<Page<ManufacturerListResponseDto>> list(Pageable pageable) {
        return manufacturerRepository.findAll(pageable.getSort())
                .skip(pageable.getOffset())
                .take(pageable.getPageSize())
                .map(manufacturer ->
                        new ManufacturerListResponseDto(
                                manufacturer.getId(),
                                manufacturer.getName(),
                                manufacturer.getCountry()))
                .collectList()
                .zipWith(manufacturerRepository.count())
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }

    @Transactional
    public Mono<Void> delete(Long id) {
        return manufacturerRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Manufacturer with id " + id + " not found"));
                    }
                    return beerRepository.existsByManufacturerId(id)
                            .flatMap(hasBeers -> {
                                if (hasBeers) {
                                    return Mono.error(new ApiException(
                                            HttpStatus.BAD_REQUEST,
                                            "Cannot delete manufacturer with id " + id + " because it has associated beers"));
                                }
                                return manufacturerRepository.deleteById(id);
                            });
                })
                .doOnSuccess(aVoid -> log.info("Deleted manufacturer with id: {}", id));
    }
}
