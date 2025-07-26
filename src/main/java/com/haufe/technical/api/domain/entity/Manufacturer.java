package com.haufe.technical.api.domain.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table
@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Manufacturer {
    @Id
    private Long id;
    private Long userId;
    private String country;

    @EqualsAndHashCode.Exclude
    @CreatedDate
    private Instant createdAt;
    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    private Instant updatedAt;
}
