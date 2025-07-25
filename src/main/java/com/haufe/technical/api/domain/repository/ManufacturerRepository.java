package com.haufe.technical.api.domain.repository;

import com.haufe.technical.api.domain.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManufacturerRepository extends
        JpaRepository<Manufacturer, Long>, PagingAndSortingRepository<Manufacturer, Long> {
    boolean existsByName(String name);
}
