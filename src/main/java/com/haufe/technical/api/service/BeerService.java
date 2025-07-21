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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeerService {
    private final ManufacturerRepository manufacturerRepository;
    private final BeerRepository beerRepository;

    @Transactional
    public BeerUpsertResponseDto create(Long manufacturerId, BeerUpsertDto request) throws ApiException {
        if (!manufacturerRepository.existsById(manufacturerId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Manufacturer with ID " + manufacturerId + " not found.");
        }

        final Beer beer = Beer.builder()
                .name(request.name())
                .abv(request.avb())
                .style(request.style())
                .description(request.description())
                .manufacturer(manufacturerRepository.getReferenceById(manufacturerId))
                .build();

        Beer savedBeer = beerRepository.save(beer);
        log.atInfo().log(() -> "Created beer: " + savedBeer);

        return new BeerUpsertResponseDto(savedBeer.getId(), savedBeer.getName());
    }

    @Transactional
    public void update(Long id, BeerUpsertDto request) throws ApiException {
        Beer beer = beerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Beer with ID " + id + " not found."));

        beer.setName(request.name());
        beer.setAbv(request.avb());
        beer.setStyle(request.style());
        beer.setDescription(request.description());

        Beer savedBeer = beerRepository.save(beer);
        log.atInfo().log(() -> "Updated beer: " + savedBeer);
    }

    public BeerReadResponseDto read(Long id) throws ApiException {
        return beerRepository.findById(id)
                .map(beer -> new BeerReadResponseDto(
                        beer.getName(),
                        beer.getAbv(),
                        beer.getStyle(),
                        beer.getDescription()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Beer with ID " + id + " not found"));
    }

    public Page<BeerListResponseDto> list(Pageable pageable) {
        return beerRepository.findAll(pageable)
                .map(beer -> new BeerListResponseDto(
                        beer.getId(),
                        beer.getName(),
                        beer.getAbv(),
                        beer.getStyle(),
                        beer.getDescription()));
    }

    public void delete(Long id) throws ApiException {
        if (!beerRepository.existsById(id)) {
            log.warn("Attempted to delete non-existing beer with id: {}", id);
            throw new ApiException(HttpStatus.NOT_FOUND, "Beer with ID " + id + " not found");
        }

        beerRepository.deleteById(id);
        log.info("Deleted beer with id: {}", id);
    }
}
