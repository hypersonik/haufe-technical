package com.haufe.technical.api.controller;

import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.exception.ApiException;
import com.haufe.technical.api.service.ManufacturerService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manufacturer")
@RequiredArgsConstructor
@Tag(name = "Manufacturer API")
public class ManufacturerController {

    public static final String PAGE_PARAMETER_EXAMPLE = """
            {
              "page": 0,
              "size": 10,
              "sort": "name"
            }""";

    private final ManufacturerService manufacturerService;

    /**
     * Adds a new manufacturer.
     *
     * @param request the {@link ManufacturerUpsertDto} request containing manufacturer details
     * @return the {@link ManufacturerUpsertResponseDto} response containing created manufacturer details
     */
    @PostMapping()
    public ManufacturerUpsertResponseDto create(@RequestBody ManufacturerUpsertDto request) {
        return manufacturerService.create(request);
    }

    /**
     * Updates an existing manufacturer.
     *
     * @param id      the ID of the manufacturer to update
     * @param request the {@link ManufacturerUpsertDto} request containing updated manufacturer details
     * @throws ApiException if the manufacturer with the given ID is not found
     */
    @PutMapping("{id}")
    public void update(@PathVariable Long id, @RequestBody ManufacturerUpsertDto request) throws ApiException {
        manufacturerService.update(id, request);
    }

    /**
     * Reads a manufacturer by ID.
     *
     * @param id the ID of the manufacturer to read
     * @return the {@link ManufacturerReadResponseDto} response containing manufacturer details
     * @throws ApiException if the manufacturer with the given ID is not found
     */
    @GetMapping("{id}")
    public ManufacturerReadResponseDto read(@PathVariable Long id) throws ApiException {
        return manufacturerService.read(id);
    }

    /**
     * Lists all manufacturers.
     *
     * @return a list of {@link ManufacturerListResponseDto} containing then id and name of all manufacturers
     */
    @GetMapping()
    public Page<ManufacturerListResponseDto> list(
            @Parameter(example = PAGE_PARAMETER_EXAMPLE)
            @PageableDefault(sort = "name") Pageable pageable) {
        return manufacturerService.list(pageable);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable Long id) throws ApiException {
        manufacturerService.delete(id);
    }
}
