package com.haufe.technical.api.controller;

import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.service.ManufacturerService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/manufacturer")
@RequiredArgsConstructor
@Tag(name = "Manufacturer API")
public class ManufacturerController {

    public static final String PAGE_PARAMETER_EXAMPLE = """
            {
              "page": 0,
              "size": 10,
              "sort": "name,asc"
            }""";

    public static final String PAGE_PARAMETERS_DESCRIPTION = """
            Valid values for sort: "id", "name", "country" with optional "asc" or "desc" suffix.""";

    private final ManufacturerService manufacturerService;

    /**
     * Adds a new manufacturer.
     *
     * @param request the {@link ManufacturerUpsertDto} request containing manufacturer details
     * @return the {@link ManufacturerUpsertResponseDto} response containing created manufacturer details
     */
    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ManufacturerUpsertResponseDto> create(@RequestBody ManufacturerUpsertDto request) {
        return manufacturerService.create(request);
    }

    /**
     * Updates an existing manufacturer.
     *
     * @param id      the ID of the manufacturer to update
     * @param request the {@link ManufacturerUpsertDto} request containing updated manufacturer details
     * @return the {@link ManufacturerUpsertResponseDto} response containing updated manufacturer details
     */
    @PutMapping("{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANUFACTURER') and #id == authentication.principal.manufacturerId)")
    public Mono<ManufacturerUpsertResponseDto> update(@PathVariable Long id,
                                                      @RequestBody ManufacturerUpsertDto request) {
        return manufacturerService.update(id, request);
    }

    /**
     * Reads a manufacturer by ID.
     *
     * @param id the ID of the manufacturer to read
     * @return the {@link ManufacturerReadResponseDto} response containing manufacturer details
     */
    @GetMapping("{id}")
    public Mono<ManufacturerReadResponseDto> read(@PathVariable Long id) {
        return manufacturerService.read(id);
    }

    /**
     * Lists all manufacturers.
     *
     * @return a list of {@link ManufacturerListResponseDto} containing then id and name of all manufacturers
     */
    @GetMapping()
    public Mono<Page<ManufacturerListResponseDto>> list(
            @Parameter(example = PAGE_PARAMETER_EXAMPLE, description = PAGE_PARAMETERS_DESCRIPTION)
            @PageableDefault(sort = "name") Pageable pageable) {
        return manufacturerService.list(pageable);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> delete(@PathVariable Long id) {
        return manufacturerService.delete(id);
    }
}
