package com.haufe.technical.api.repository;

import com.haufe.technical.api.domain.entity.Beer;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface BeerRepository extends R2dbcRepository<Beer, Long> {
    Mono<Boolean> existsByManufacturerId(Long manufacturerId);
}
