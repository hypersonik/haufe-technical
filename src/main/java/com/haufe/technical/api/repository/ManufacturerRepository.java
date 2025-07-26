package com.haufe.technical.api.repository;

import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.domain.entity.Manufacturer;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ManufacturerRepository extends R2dbcRepository<Manufacturer, Long> {
    @Query("""
        SELECT U.NAME, M.COUNTRY
        FROM MANUFACTURER M LEFT JOIN "USER" U ON U.ID = M.USER_ID
        WHERE M.ID = :ID
    """)
    Mono<ManufacturerReadResponseDto> findByIdWithName(Long id);
}
