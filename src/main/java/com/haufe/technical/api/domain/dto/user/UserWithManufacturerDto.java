package com.haufe.technical.api.domain.dto.user;

public record UserWithManufacturerDto(Long id,
                                      Long manufacturerId,
                                      String name,
                                      String password,
                                      String roles,
                                      boolean enabled) {}
