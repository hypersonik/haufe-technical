package com.haufe.technical.api.controller;

import com.haufe.technical.api.controller.dto.beer.BeerListResponseDto;
import com.haufe.technical.api.controller.dto.beer.BeerReadResponseDto;
import com.haufe.technical.api.controller.dto.beer.BeerUpsertDto;
import com.haufe.technical.api.controller.dto.beer.BeerUpsertResponseDto;
import com.haufe.technical.api.exception.ApiException;
import com.haufe.technical.api.service.BeerService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

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
    public BeerUpsertResponseDto create(@PathVariable Long manufacturerId,
                                        @RequestBody BeerUpsertDto request) throws ApiException {
        return beerService.create(manufacturerId, request);
    }

    /**
     * Updates an existing beer.
     *
     * @param id      the ID of the beer to update
     * @param request the {@link BeerUpsertDto} request containing updated beer details
     * @throws ApiException if the beer with the given ID is not found
     */
    @PutMapping("{id}")
    public void update(@PathVariable Long id, @RequestBody BeerUpsertDto request) throws ApiException {
        beerService.update(id, request);
    }

    /**
     * Reads a beer by ID.
     *
     * @param id the ID of the beer to read
     * @return the {@link BeerReadResponseDto} response containing beer details
     * @throws ApiException if the beer with the given ID is not found
     */
    @GetMapping("{id}")
    public BeerReadResponseDto read(@PathVariable Long id) throws ApiException {
        return beerService.read(id);
    }

    /**
     * Lists all beers.
     *
     * @return a list of {@link BeerListResponseDto} containing then id and name of all beers
     */
    @GetMapping()
    public Page<BeerListResponseDto> list(
            @Parameter(example = PAGE_PARAMETER_EXAMPLE)
            @PageableDefault(sort = "name") Pageable pageable) {
        return beerService.list(pageable);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable Long id) throws ApiException {
        beerService.delete(id);
    }
}
