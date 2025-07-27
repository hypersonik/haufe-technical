package com.haufe.technical.api.domain.dto.manufacturer;

import lombok.Builder;

@Builder(toBuilder = true)
public record ManufacturerUpsertDto(String userName, String password, Boolean userEnabled, String name, String country) {}
