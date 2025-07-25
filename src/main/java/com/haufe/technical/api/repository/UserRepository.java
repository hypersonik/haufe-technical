package com.haufe.technical.api.repository;

import com.haufe.technical.api.domain.entity.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {
    Mono<User> findByNameAndEnabledIsTrue(String name);
}
