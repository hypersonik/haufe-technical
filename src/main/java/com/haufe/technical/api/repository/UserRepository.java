package com.haufe.technical.api.repository;

import com.haufe.technical.api.domain.dto.user.UserWithManufacturerDto;
import com.haufe.technical.api.domain.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {
    Mono<User> findByName(String name);
    Mono<Boolean> existsByNameAndIdNot(String name, Long id);

    @Query("""
        SELECT U.ID, M.ID AS MANUFACTURER_ID, U.NAME, U.PASSWORD, U.ROLES, U.ENABLED
        FROM "USER" U LEFT JOIN MANUFACTURER M ON M.USER_ID = U.ID
        WHERE U.NAME = :NAME AND U.ENABLED = TRUE
    """)
    Mono<UserWithManufacturerDto> findByNameAndEnabled(String name);
}
