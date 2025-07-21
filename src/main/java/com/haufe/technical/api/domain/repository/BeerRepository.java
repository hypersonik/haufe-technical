package com.haufe.technical.api.domain.repository;

import com.haufe.technical.api.domain.entity.Beer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeerRepository extends CrudRepository<Beer, Long>, PagingAndSortingRepository<Beer, Long> {
    boolean existsByManufacturerId(Long manufacturerId);
}
