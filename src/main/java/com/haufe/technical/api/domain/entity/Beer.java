package com.haufe.technical.api.domain.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Beer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Float abv;
    private String style;
    private String description;

    @EqualsAndHashCode.Exclude
    private Long manufacturerId;

    @EqualsAndHashCode.Exclude
    @CreatedDate
    private Instant createdAt;
    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    private Instant updatedAt;
}
