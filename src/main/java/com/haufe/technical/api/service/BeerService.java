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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
                            .abv(request.abv())
                            .style(request.style())
                            .description(request.description())
                            .manufacturerId(manufacturerId)
                            .build();

                    log.info("creating beer: {}", beer);
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
                    if (request.abv() != null)                          beer.setAbv(request.abv());
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
    public Mono<Page<BeerListResponseDto>> list(String name,
                                                Float minAbv,
                                                Float maxAbv,
                                                String style,
                                                Long manufacturerId,
                                                Pageable pageable) {
        Sort sort = pageable.getSort();
        Sort.Order order = sort.get().findFirst().orElseGet(() -> Sort.Order.by("id"));

        int page = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        if (page < 0 || pageSize <= 0) {
            return Mono.error(new ApiException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters."));
        }

        SqlList sql = new SqlList(name, minAbv, maxAbv, style, manufacturerId, order, page * pageSize, pageSize);
        Mono<List<BeerListResponseDto>> beers = sql.getBeers();
        Mono<Long> count = sql.getCount();

        return Mono.zip(beers, count)
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }

    private class SqlList {
        private final String name;
        private final Float minAbv;
        private final Float maxAbv;
        private final String style;
        private final Long manufacturerId;

        private final String listSql;
        private final String countSql;

        public SqlList(String name, Float minAbv, Float maxAbv, String style, Long manufacturerId, Sort.Order order, int offset, int pageSize) {
            this.name = name;
            this.minAbv = minAbv;
            this.maxAbv = maxAbv;
            this.style = style;
            this.manufacturerId = manufacturerId;

            String sortProperty = order.getProperty();
            String sortDirection = order.getDirection().name();

            // Allowlist of allowed columns to prevent SQL injection
            if (!ALLOWED_SORT_COLUMNS.contains(sortProperty.toLowerCase())) {
                sortProperty = "id"; // Safe fallback
            }

            StringBuilder whereClause = new StringBuilder();

            if (StringUtils.isNotBlank(name))  whereClause.append(" AND name ILIKE CONCAT('%', :name, '%')");
            if (minAbv != null)                whereClause.append(" AND abv >= :minAbv");
            if (maxAbv != null)                whereClause.append(" AND abv <= :maxAbv");
            if (StringUtils.isNotBlank(style)) whereClause.append(" AND style ILIKE CONCAT('%', :style, '%')");
            if (manufacturerId != null)        whereClause.append(" AND manufacturer_id = :manufacturerId");

            String limits = " ORDER BY " + sortProperty + " " + sortDirection + " LIMIT " + pageSize + " OFFSET " + offset;

            listSql = "SELECT * FROM BEER WHERE 1=1" + whereClause + limits;
            countSql = "SELECT COUNT(*) AS count FROM BEER WHERE 1=1" + whereClause;
        }

        public Mono<List<BeerListResponseDto>> getBeers() {
            DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(listSql);
            return bindParams(spec)
                    .map(row -> BeerListResponseDto.builder()
                            .id(row.get("id", Long.class))
                            .name(row.get("name", String.class))
                            .abv(row.get("abv", Float.class))
                            .style(row.get("style", String.class))
                            .description(row.get("description", String.class))
                            .build())
                    .all()
                    .collectList();
        }

        public Mono<Long> getCount() {
            DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(countSql);
            return bindParams(spec)
                    .map(row -> row.get("count", Long.class))
                    .first();
        }

        private DatabaseClient.GenericExecuteSpec bindParams(DatabaseClient.GenericExecuteSpec spec) {
            if (StringUtils.isNotBlank(name))  spec = spec.bind("name", name);
            if (minAbv != null)                spec = spec.bind("minAbv", minAbv);
            if (maxAbv != null)                spec = spec.bind("maxAbv", maxAbv);
            if (StringUtils.isNotBlank(style)) spec = spec.bind("style", style);
            if (manufacturerId != null)        spec = spec.bind("manufacturerId", manufacturerId);
            return spec;
        }
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
