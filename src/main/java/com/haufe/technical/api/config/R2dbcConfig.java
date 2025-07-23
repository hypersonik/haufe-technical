package com.haufe.technical.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.H2Dialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableR2dbcRepositories
@EnableR2dbcAuditing
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = Arrays.asList(
                new InstantToLocalDateTimeConverter(),
                new LocalDateTimeToInstantConverter()
        );
        return R2dbcCustomConversions.of(H2Dialect.INSTANCE, converters);
    }

    @WritingConverter
    public static class InstantToLocalDateTimeConverter implements Converter<Instant, LocalDateTime> {
        @Override
        public LocalDateTime convert(Instant source) {
            return LocalDateTime.ofInstant(source, ZoneOffset.UTC);
        }
    }

    @ReadingConverter
    public static class LocalDateTimeToInstantConverter implements Converter<LocalDateTime, Instant> {
        @Override
        public Instant convert(LocalDateTime source) {
            return source.toInstant(ZoneOffset.UTC);
        }
    }
}