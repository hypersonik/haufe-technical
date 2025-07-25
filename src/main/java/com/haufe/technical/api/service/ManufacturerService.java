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
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManufacturerService {
    private final ManufacturerRepository manufacturerRepository;
    private final BeerRepository beerRepository;

    @Transactional
    public ManufacturerUpsertResponseDto create(ManufacturerUpsertDto request) throws ApiException {
        // Validate request
        if (StringUtils.isBlank(request.name())) {
            log.warn("Attempted to create manufacturer with missing name");
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Manufacturer name must not be null");
        }

        // Check for duplicate manufacturer name
        if (manufacturerRepository.existsByName(request.name())) {
            log.warn("Attempted to create manufacturer with duplicate name: {}", request.name());
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Manufacturer with name " + request.name() + " already exists");
        }

        // Create a new Manufacturer entity
        final Manufacturer savedManufacturer = createNew(request);
        return new ManufacturerUpsertResponseDto(savedManufacturer.getId(), savedManufacturer.getName());
    }

    private Manufacturer createNew(ManufacturerUpsertDto request) {
        final Manufacturer manufacturer = Manufacturer.builder()
                .name(request.name())
                .country(request.country())
                .build();

        final Manufacturer savedManufacturer = manufacturerRepository.save(manufacturer);
        log.atInfo().log(() -> "Created manufacturer: " + savedManufacturer);

        return savedManufacturer;
    }

    @Transactional
    public void update(Long id, ManufacturerUpsertDto request) throws ApiException {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Manufacturer with id " + id + " not found"));

        // Don't allow clearing the name
        if (StringUtils.isNotBlank(request.name())) {
            manufacturer.setName(request.name());
        }
        manufacturer.setCountry(request.country());

        if (manufacturerRepository.existsByName(manufacturer.getName())) {
            log.warn("Attempted to update manufacturer with duplicate name: {}", manufacturer.getName());
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Manufacturer with name " + manufacturer.getName() + " already exists");
        }

        Manufacturer savedManufacturer = manufacturerRepository.save(manufacturer);
        log.atInfo().log(() -> "Updated manufacturer: " + savedManufacturer);
    }

    public ManufacturerReadResponseDto read(Long id) throws ApiException {
        return manufacturerRepository.findById(id)
                .map(manufacturer -> new ManufacturerReadResponseDto(manufacturer.getName(), manufacturer.getCountry()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Manufacturer with id " + id + " not found"));
    }

    public Page<ManufacturerListResponseDto> list(Pageable pageable) {
        return manufacturerRepository.findAll(pageable)
                .map(manufacturer ->
                        new ManufacturerListResponseDto(
                                manufacturer.getId(),
                                manufacturer.getName(),
                                manufacturer.getCountry()));
    }

    @Transactional
    public void delete(Long id) throws ApiException {
        if (!manufacturerRepository.existsById(id)) {
            log.warn("Attempted to delete non-existing manufacturer with id: {}", id);
            throw new ApiException(HttpStatus.NOT_FOUND, "Manufacturer with id " + id + " not found");
        }

        if (beerRepository.existsByManufacturerId(id)) {
            log.warn("Attempted to delete manufacturer with id: {} that has associated beers", id);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot delete manufacturer with id " + id + " because it has associated beers");
        }

        manufacturerRepository.deleteById(id);
        log.info("Deleted manufacturer with id: {}", id);
    }
}
