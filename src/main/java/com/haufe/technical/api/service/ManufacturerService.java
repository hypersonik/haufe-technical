package com.haufe.technical.api.service;

import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Manufacturer;
import com.haufe.technical.api.domain.entity.User;
import com.haufe.technical.api.exception.ApiException;
import com.haufe.technical.api.repository.BeerRepository;
import com.haufe.technical.api.repository.ManufacturerRepository;
import com.haufe.technical.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManufacturerService {
    private final ManufacturerRepository manufacturerRepository;
    private final BeerRepository beerRepository;
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Mono<ManufacturerUpsertResponseDto> create(ManufacturerUpsertDto request) {
        if (!checkManufacturer(request)) {
            return Mono.error(new ApiException(HttpStatus.BAD_REQUEST, "User name and name must not be empty"));
        }
        return userRepository.findByName(request.userName())
                .flatMap(existingUser ->
                        Mono.<ManufacturerUpsertResponseDto>error(
                                new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "User with name " + request.userName() + " already exists")))
                .switchIfEmpty(Mono.defer(() -> createUser(request)));
    }

    private static boolean checkManufacturer(ManufacturerUpsertDto request) {
        return StringUtils.isNotBlank(request.userName()) && StringUtils.isNotBlank(request.name());
    }

    private Mono<ManufacturerUpsertResponseDto> createUser(ManufacturerUpsertDto request) {
        User user = User.builder()
                .name(request.userName())
                .password(passwordEncoder.encode(request.password()))
                .enabled(request.userEnabled())
                .roles("MANUFACTURER")
                .build();
        return userRepository.save(user)
                .flatMap(savedUser -> createManufacturer(request, savedUser.getId()));
    }

    private Mono<ManufacturerUpsertResponseDto> createManufacturer(ManufacturerUpsertDto request, Long userId) {
        Manufacturer manufacturer = Manufacturer.builder()
                .userId(userId)
                .name(request.name())
                .country(request.country())
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
        return checkUserNameUniqueness(request.userName(), user.getId())
                .flatMap(unused ->  updateManufacturerAndUser(request, manufacturer, user));
    }

    private Mono<Boolean> checkUserNameUniqueness(String name, Long userId) {
        return StringUtils.isBlank(name) ?
                Mono.just(true) :
                userRepository.existsByNameAndIdNot(name, userId)
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
                .flatMap(unused -> manufacturerRepository.save(manufacturer))
                .map(updatedManufacturer -> new ManufacturerUpsertResponseDto(
                        updatedManufacturer.getId(),
                        updatedManufacturer.getName()));
    }

    // Update user details if they are provided
    private void updateFields(ManufacturerUpsertDto request, Manufacturer manufacturer, User user) {
        if (StringUtils.isNotBlank(request.name()))     manufacturer.setName(request.name());
        if (StringUtils.isNotBlank(request.country()))  manufacturer.setCountry(request.country());
        if (StringUtils.isNotBlank(request.name()))     user.setName(request.userName());
        if (StringUtils.isNotBlank(request.password())) user.setPassword(passwordEncoder.encode(request.password()));
    }

    public Mono<ManufacturerReadResponseDto> read(Long id) {
        return manufacturerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Manufacturer with id " + id + " not found")))
                .map(manufacturer ->
                        new ManufacturerReadResponseDto(manufacturer.getName(), manufacturer.getCountry()));
    }

    public Mono<Page<ManufacturerListResponseDto>> list(Pageable pageable) {
        return manufacturerRepository.findAll(pageable.getSort())
                .skip(pageable.getOffset())
                .take(pageable.getPageSize())
                .map(manufacturer -> new ManufacturerListResponseDto(
                        manufacturer.getId(),
                        manufacturer.getName(),
                        manufacturer.getCountry()))
                .collectList()
                .zipWith(manufacturerRepository.count())
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
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
