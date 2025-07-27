package com.haufe.technical.api.domain.dto.beer;

import lombok.Builder;

@Builder
public record BeerUpsertResponseDto(Long id, String name) {}
