package com.haufe.technical.api.service;

import com.haufe.technical.api.domain.dto.beer.BeerListResponseDto;
import com.haufe.technical.api.domain.dto.beer.BeerReadResponseDto;
import com.haufe.technical.api.domain.dto.beer.BeerUpsertDto;
import com.haufe.technical.api.domain.dto.beer.BeerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Beer;
import com.haufe.technical.api.exception.ApiException;
import com.haufe.technical.api.repository.BeerRepository;
import com.haufe.technical.api.repository.ManufacturerRepository;
import com.haufe.technical.api.utils.ReactiveSecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeerService {
    private static final List<String> ALLOWED_SORT_COLUMNS = List.of("id", "name", "abv", "style", "description");

    private final ManufacturerRepository manufacturerRepository;
    private final BeerRepository beerRepository;

    private final DatabaseClient databaseClient;

    @Transactional
    public Mono<BeerUpsertResponseDto> create(Long manufacturerId, BeerUpsertDto request) throws ApiException {
        return manufacturerRepository.existsById(manufacturerId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Manufacturer with ID " + manufacturerId + " not found."));
                    }

                    final Beer beer = Beer.builder()
                            .name(request.name())
                            .abv(request.avb())
                            .style(request.style())
                            .description(request.description())
                            .manufacturerId(manufacturerId)
                            .build();

                    return beerRepository.save(beer);
                })
                .map(savedBeer -> {
                    log.atInfo().log(() -> "Created beer: " + savedBeer);
                    return new BeerUpsertResponseDto(savedBeer.getId(), savedBeer.getName());
                });
    }

    @Transactional
    public Mono<BeerUpsertResponseDto> update(Long id, BeerUpsertDto request) {
        return ReactiveSecurityUtils.getManufacturerId()
                .flatMap(manufacturerId -> update(id, request, manufacturerId.orElse(null)));
    }

    private Mono<BeerUpsertResponseDto> update(Long id, BeerUpsertDto request, Long manufacturerId) {
        return beerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Beer with ID " + id + " not found.")))
                .flatMap(beer -> {
                    // Check if the beer belongs to the manufacturer.
                    // If manufacturerId is null, it means the user is not a manufacturer.
                    // If the user is not a manufacturer and this code is reached,
                    // it means the user is an admin or has permission to update any beer.
                    if (manufacturerId != null && !beer.getManufacturerId().equals(manufacturerId)) {
                        return Mono.error(new ApiException(
                                HttpStatus.FORBIDDEN,
                                "You don't have permission to update this beer"));
                    }
                    return Mono.just(beer);
                })
                .flatMap(beer -> {
                    // Update fields only if they are not null or blank
                    if (StringUtils.isNotBlank(request.name()))         beer.setName(request.name());
                    if (request.avb() != null)                          beer.setAbv(request.avb());
                    if (StringUtils.isNotBlank(request.style()))        beer.setStyle(request.style());
                    if (StringUtils.isNotBlank(request.description()))  beer.setDescription(request.description());

                    return beerRepository.save(beer);
                })
                .map(savedBeer -> new BeerUpsertResponseDto(savedBeer.getId(), savedBeer.getName()))
                .doOnSuccess(savedBeer -> log.atInfo().log(() -> "Updated beer: " + savedBeer))
                .doOnError(exception -> log.error("Error updating beer with ID {}: {}", id, exception.getMessage()));
    }

    public Mono<BeerReadResponseDto> read(Long id) {
        return beerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND, "Beer with ID " + id + " not found.")))
                .map(beer -> new BeerReadResponseDto(
                        beer.getName(),
                        beer.getAbv(),
                        beer.getStyle(),
                        beer.getDescription()));
    }

    /**
     * Lists beers based on various filters and pagination.
     *
     * @param name            Optional name filter (case-insensitive).
     * @param minAbv          Optional minimum ABV filter.
     * @param maxAbv          Optional maximum ABV filter.
     * @param style           Optional style filter (case-insensitive).
     * @param manufacturerId  Optional manufacturer ID filter.
     * @param pageable        Pagination information.
     * @return Flux of BeerListResponseDto containing beer details.
     */
    public Flux<BeerListResponseDto> list(String name,
                                          Float minAbv,
                                          Float maxAbv,
                                          String style,
                                          Long manufacturerId,
                                          Pageable pageable) {
        return flexibleList(name, minAbv, maxAbv, style, manufacturerId, pageable)
                .map(beer -> new BeerListResponseDto(
                        beer.getId(),
                        beer.getName(),
                        beer.getAbv(),
                        beer.getStyle(),
                        beer.getDescription()));
    }

    private Flux<Beer> flexibleList(
            String name,
            Float minAbv,
            Float maxAbv,
            String style,
            Long manufacturerId,
            Pageable pageable
    ) {
        Sort sort = pageable.getSort();
        Sort.Order order = sort.get().findFirst().orElseGet(() -> Sort.Order.by("id"));

        int page = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        if (page < 0 || pageSize <= 0) {
            return Flux.error(new ApiException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters."));
        }

        String sql = buildListSql(name, minAbv, maxAbv, style, manufacturerId, order, page * pageSize, pageSize);
        log.debug("Generated SQL: {}", sql);

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql);
        if (StringUtils.isNotBlank(name))  spec = spec.bind("name", name);
        if (minAbv != null)                spec = spec.bind("minAbv", minAbv);
        if (maxAbv != null)                spec = spec.bind("maxAbv", maxAbv);
        if (StringUtils.isNotBlank(style)) spec = spec.bind("style", style);
        if (manufacturerId != null)        spec = spec.bind("manufacturerId", manufacturerId);

        return spec
                .map((row, metadata) -> Beer.builder()
                        .id(row.get("id", Long.class))
                        .name(row.get("name", String.class))
                        .abv(row.get("abv", Float.class))
                        .style(row.get("style", String.class))
                        .description(row.get("description", String.class))
                        .manufacturerId(row.get("manufacturer_id", Long.class))
                        .build())
                .all();
    }

    private String buildListSql(
            String name,
            Float minAbv,
            Float maxAbv,
            String style,
            Long manufacturerId,
            Sort.Order order,
            int offset,
            int pageSize) {

        String sortProperty = order.getProperty();
        String sortDirection = order.getDirection().name();

        // Allowlist of allowed columns to prevent SQL injection
        if (!ALLOWED_SORT_COLUMNS.contains(sortProperty.toLowerCase())) {
            sortProperty = "id"; // Safe fallback
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM BEER WHERE 1=1");

        if (StringUtils.isNotBlank(name))  sql.append(" AND name ILIKE CONCAT('%', :name, '%')");
        if (minAbv != null)                sql.append(" AND abv >= :minAbv");
        if (maxAbv != null)                sql.append(" AND abv <= :maxAbv");
        if (StringUtils.isNotBlank(style)) sql.append(" AND style ILIKE CONCAT('%', :style, '%')");
        if (manufacturerId != null)        sql.append(" AND manufacturer_id = :manufacturerId");

        sql.append(" ORDER BY ").append(sortProperty).append(" ").append(sortDirection);
        sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);

        return sql.toString();
    }

    public Mono<Void> delete(Long id) {
        return ReactiveSecurityUtils.getManufacturerId()
                        .flatMap(manufacturerId -> delete(id, manufacturerId.orElse(null)));
    }

    private Mono<Void> delete(Long id, Long manufacturerId) {
        return beerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Beer with ID " + id + " not found.")))
                .flatMap(beer -> {
                    // Check if the beer belongs to the manufacturer.
                    // If manufacturerId is null, it means the user is not a manufacturer.
                    // If the user is not a manufacturer and this code is reached,
                    // it means the user is an admin or has permission to delete any beer.
                    if (manufacturerId != null && !beer.getManufacturerId().equals(manufacturerId)) {
                        return Mono.error(new ApiException(
                                HttpStatus.FORBIDDEN,
                                "You don't have permission to delete this beer"));
                    }
                    return Mono.just(beer);
                })
                .flatMap(beerRepository::delete)
                .doOnSuccess(aVoid -> log.info("Deleted beer with id: {}", id))
                .doOnError(exception -> log.error("Error deleting beer with ID {}: {}", id, exception.getMessage()));
    }
}
