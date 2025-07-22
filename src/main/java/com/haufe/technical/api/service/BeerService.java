package com.haufe.technical.api.service;

import com.haufe.technical.api.controller.dto.beer.BeerListResponseDto;
import com.haufe.technical.api.controller.dto.beer.BeerReadResponseDto;
import com.haufe.technical.api.controller.dto.beer.BeerUpsertDto;
import com.haufe.technical.api.controller.dto.beer.BeerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Beer;
import com.haufe.technical.api.domain.repository.BeerRepository;
import com.haufe.technical.api.domain.repository.ManufacturerRepository;
import com.haufe.technical.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                })
                .doOnSuccess(savedBeer -> log.atInfo().log(() -> "Created beer: " + savedBeer));
    }

    @Transactional
    public Mono<BeerUpsertResponseDto> update(Long id, BeerUpsertDto request) {
        return beerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Beer with ID " + id + " not found.")))
                .flatMap(beer -> {
                    // Update fields only if they are not null
                    if (request.name() != null) {
                        beer.setName(request.name());
                    }
                    if (request.avb() != null) {
                        beer.setAbv(request.avb());
                    }
                    if (request.style() != null) {
                        beer.setStyle(request.style());
                    }
                    if (request.description() != null) {
                        beer.setDescription(request.description());
                    }
                    return beerRepository.save(beer);
                })
                .map(savedBeer -> {
                    log.atInfo().log(() -> "Updated beer: " + savedBeer);
                    return new BeerUpsertResponseDto(savedBeer.getId(), savedBeer.getName());
                })
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
        return beerRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Beer with ID " + id + " not found"));
                    }
                    return beerRepository.deleteById(id);
                })
                .doOnSuccess(aVoid -> log.info("Deleted beer with id: {}", id));
    }
}
