package com.haufe.technical.api.domain.dto.beer;

import lombok.Builder;

@Builder
public record BeerReadResponseDto(String name, Float abv, String style, String description) {}
