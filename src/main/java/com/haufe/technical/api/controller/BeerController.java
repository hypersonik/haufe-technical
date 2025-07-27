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
import org.springframework.security.access.prepost.PreAuthorize;
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
              "sort": "name,asc"
            }""";

    public static final String PAGE_PARAMETERS_DESCRIPTION = """
            Valid values for sort: "id", "name", "abv", "style", "description" with optional "asc" or "desc" suffix.""";

    private final BeerService beerService;


    /**
     * Adds a new beer.
     *
     * @param request the {@link BeerUpsertDto} request containing beer details
     * @return the {@link BeerUpsertResponseDto} response containing created beer details
     */
    @PostMapping("{manufacturerId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANUFACTURER') and #manufacturerId == authentication.principal.manufacturerId)")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANUFACTURER')")
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
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Float minAbv,
            @RequestParam(required = false) Float maxAbv,
            @RequestParam(required = false) String style,
            @RequestParam(required = false) Long manufacturerId,
            @Parameter(example = PAGE_PARAMETER_EXAMPLE, description = PAGE_PARAMETERS_DESCRIPTION)
            @PageableDefault(sort = "name") Pageable pageable) {
        return beerService.list(name, minAbv, maxAbv, style, manufacturerId, pageable);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANUFACTURER')")
    public Mono<Void> delete(@PathVariable Long id) {
        return beerService.delete(id);
    }
}
