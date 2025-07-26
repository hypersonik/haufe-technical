package com.haufe.technical.api.service;

import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Manufacturer;
import com.haufe.technical.api.domain.entity.User;
import com.haufe.technical.api.repository.BeerRepository;
import com.haufe.technical.api.repository.ManufacturerRepository;
import com.haufe.technical.api.exception.ApiException;
import com.haufe.technical.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManufacturerService {
    private static final List<String> ALLOWED_COLUMNS = List.of("id", "name", "country");

    private final ManufacturerRepository manufacturerRepository;
    private final BeerRepository beerRepository;
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    private final DatabaseClient databaseClient;

    @Transactional
    public Mono<ManufacturerUpsertResponseDto> create(ManufacturerUpsertDto request) {
        return userRepository.findByName(request.name())
                .flatMap(existingUser ->
                        Mono.<ManufacturerUpsertResponseDto>error(
                                new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "User with name " + request.name() + " already exists")))
                .switchIfEmpty(Mono.defer(() -> createUser(request)));
    }

    private Mono<ManufacturerUpsertResponseDto> createUser(ManufacturerUpsertDto request) {
        User user = User.builder()
                .name(request.name())
                .password(passwordEncoder.encode(request.password()))
                .roles("MANUFACTURER")
                .build();
        return userRepository.save(user)
                .flatMap(savedUser -> createNew(request, savedUser.getId()));
    }

    private Mono<ManufacturerUpsertResponseDto> createNew(ManufacturerUpsertDto request, Long userId) {
        Manufacturer manufacturer = Manufacturer.builder()
                .country(request.country())
                .userId(userId)
                .build();

        return manufacturerRepository.save(manufacturer)
                .map(savedManufacturer ->
                        new ManufacturerUpsertResponseDto(savedManufacturer.getId(), request.name()))
                .doOnSuccess(savedManufacturer ->
                        log.atInfo().log(() -> "Created manufacturer: " + savedManufacturer))
                .doOnError(exception ->
                        log.error("Error creating manufacturer: {}", exception.getMessage()));
    }

    @Transactional
    public Mono<ManufacturerUpsertResponseDto> update(Long id, ManufacturerUpsertDto request) {
        return manufacturerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Manufacturer with id " + id + " not found")))
                .flatMap(manufacturer -> doManufacturerUpdate(request, manufacturer))
                .doOnSuccess(savedManufacturer ->
                        log.atInfo().log(() -> "Updated manufacturer: " + savedManufacturer))
                .doOnError(exception ->
                        log.error("Error updating manufacturer with id {}: {}", id, exception.getMessage()));
    }

    private Mono<ManufacturerUpsertResponseDto> doManufacturerUpdate(ManufacturerUpsertDto request, Manufacturer manufacturer) {
        return findUserForManufacturer(manufacturer)
                .flatMap(user -> validateAndUpdateUser(request, user, manufacturer));
    }

    private Mono<User> findUserForManufacturer(Manufacturer manufacturer) {
        return userRepository.findById(manufacturer.getUserId())
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "User for manufacturer with id " + manufacturer.getId() + " not found")));
    }

    private Mono<ManufacturerUpsertResponseDto> validateAndUpdateUser(ManufacturerUpsertDto request, User user, Manufacturer manufacturer) {
        return checkUserNameUniqueness(request.name(), user.getId())
                .flatMap(unused ->  updateManufacturerAndUser(request, manufacturer, user));
    }

    private Mono<Boolean> checkUserNameUniqueness(String name, Long userId) {
        return userRepository.existsByNameAndIdNot(name, userId)
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.CONFLICT,
                        "A user with name '" + name + "' already exists")));
    }

    private Mono<ManufacturerUpsertResponseDto> updateManufacturerAndUser(ManufacturerUpsertDto request,
                                                                          Manufacturer manufacturer,
                                                                          User user) {
        updateFields(request, manufacturer, user);
        return userRepository.save(user)
                .zipWith(manufacturerRepository.save(manufacturer))
                .map(tuple -> new ManufacturerUpsertResponseDto(
                        tuple.getT2().getId(),
                        tuple.getT1().getName()));
    }

    // Update user details if they are provided
    private void updateFields(ManufacturerUpsertDto request, Manufacturer manufacturer, User user) {
        if (StringUtils.isNotBlank(request.country()))  manufacturer.setCountry(request.country());
        if (StringUtils.isNotBlank(request.name()))     user.setName(request.name());
        if (StringUtils.isNotBlank(request.password())) user.setPassword(passwordEncoder.encode(request.password()));
    }

    public Mono<ManufacturerReadResponseDto> read(Long id) {
        return manufacturerRepository.findByIdWithName(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Manufacturer with id " + id + " not found")))
                .map(dto ->
                        new ManufacturerReadResponseDto(dto.name(), dto.country()));
    }

    public Mono<Page<ManufacturerListResponseDto>> list(Pageable pageable) {
        return findAllWithName(pageable.getPageSize(), pageable.getOffset(), pageable.getSort())
                .collectList()
                .zipWith(manufacturerRepository.count())
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }

    private Flux<ManufacturerListResponseDto> findAllWithName(long limit, long offset, Sort sort) {
        // Validate sort parameters
        Sort.Order order = sort.get().findFirst().orElse(Sort.Order.by("id"));
        String sql = buildSql(limit, offset, order);
        log.debug("Executing SQL: {}", sql);

        return databaseClient.sql(sql)
                .map((row, metadata) -> new ManufacturerListResponseDto(
                        row.get("ID", Long.class),
                        row.get("NAME", String.class),
                        row.get("COUNTRY", String.class)
                ))
                .all();
    }

    private static String buildSql(long limit, long offset, Sort.Order order) {
        String sortProperty = order.getProperty();
        String sortDirection = order.getDirection().name();

        // Allowlist of allowed columns to prevent SQL injection
        if (!ALLOWED_COLUMNS.contains(sortProperty.toLowerCase())) {
            sortProperty = "id"; // Safe fallback
        }

        // Dynamic SQL query construction
        return """
            SELECT M.ID, U.NAME, M.COUNTRY
            FROM MANUFACTURER M LEFT JOIN "USER" U ON U.ID = M.USER_ID
            ORDER BY %s %s
            LIMIT %d OFFSET %d
            """.formatted(sortProperty, sortDirection, limit, offset);
    }

    @Transactional
    public Mono<Void> delete(Long id) {
        return manufacturerRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Manufacturer with id " + id + " not found"));
                    }
                    return beerRepository.existsByManufacturerId(id)
                            .flatMap(hasBeers -> {
                                if (hasBeers) {
                                    return Mono.error(new ApiException(
                                            HttpStatus.BAD_REQUEST,
                                            "Cannot delete manufacturer with id " + id + " because it has associated beers"));
                                }
                                return manufacturerRepository.deleteById(id);
                            });
                })
                .doOnSuccess(aVoid -> log.info("Deleted manufacturer with id: {}", id));
    }
}
