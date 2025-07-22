package com.haufe.technical.api.domain.repository;

import com.haufe.technical.api.domain.entity.Manufacturer;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManufacturerRepository extends R2dbcRepository<Manufacturer, Long> {
}
