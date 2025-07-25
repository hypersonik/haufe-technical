package com.haufe.technical.api.controller;

import com.haufe.technical.api.domain.dto.beer.BeerListResponseDto;
import com.haufe.technical.api.domain.dto.beer.BeerReadResponseDto;
import com.haufe.technical.api.domain.dto.beer.BeerUpsertDto;
import com.haufe.technical.api.domain.dto.beer.BeerUpsertResponseDto;
import com.haufe.technical.api.exception.ApiException;
import com.haufe.technical.api.service.BeerService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/beer")
@RequiredArgsConstructor
@Tag(name = "Beer API")
public class BeerController {

    public static final String PAGE_PARAMETER_EXAMPLE = """
            {
              "page": 0,
              "size": 10,
              "sort": "name"
            }""";

    private final BeerService beerService;


    /**
     * Adds a new beer.
     *
     * @param request the {@link BeerUpsertDto} request containing beer details
     * @return the {@link BeerUpsertResponseDto} response containing created beer details
     */
    @PostMapping("{manufacturerId}")
    public Mono<BeerUpsertResponseDto> create(@PathVariable Long manufacturerId,
                                              @RequestBody BeerUpsertDto request) throws ApiException {
        return beerService.create(manufacturerId, request);
    }

    /**
     * Updates an existing beer.
     *
     * @param id      the ID of the beer to update
     * @param request the {@link BeerUpsertDto} request containing updated beer details
     * @return the {@link BeerUpsertResponseDto} response containing updated beer details
     */
    @PutMapping("{id}")
    public Mono<BeerUpsertResponseDto> update(@PathVariable Long id, @RequestBody BeerUpsertDto request) {
        return beerService.update(id, request);
    }

    /**
     * Reads a beer by ID.
     *
     * @param id the ID of the beer to read
     * @return the {@link BeerReadResponseDto} response containing beer details
     */
    @GetMapping("{id}")
    public Mono<BeerReadResponseDto> read(@PathVariable Long id) {
        return beerService.read(id);
    }

    /**
     * Lists all beers.
     *
     * @return a list of {@link BeerListResponseDto} containing then id and name of all beers
     */
    @GetMapping()
    public Flux<BeerListResponseDto> list(
            @Parameter(example = PAGE_PARAMETER_EXAMPLE)
            @PageableDefault(sort = "name") Pageable pageable) {
        return beerService.list(pageable);
    }

    @DeleteMapping("{id}")
    public Mono<Void> delete(@PathVariable Long id) {
        return beerService.delete(id);
    }
}
