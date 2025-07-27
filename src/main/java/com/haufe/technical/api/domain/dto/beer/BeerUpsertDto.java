package com.haufe.technical.api.domain.dto.beer;

import lombok.Builder;

@Builder
public record BeerUpsertDto(String name, Float avb, String style, String description) {}
