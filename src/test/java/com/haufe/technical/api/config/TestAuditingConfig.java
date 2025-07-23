package com.haufe.technical.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@TestConfiguration
@EnableR2dbcAuditing
public class TestAuditingConfig {

    @Bean
    public ReactiveAuditorAware<String> testAuditorProvider() {
        return () -> Mono.just("admin");
    }

    @Bean
    public DateTimeProvider testDateTimeProvider() {
        return () -> Optional.of(Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
