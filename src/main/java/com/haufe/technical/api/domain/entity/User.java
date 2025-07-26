package com.haufe.technical.api.domain.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table(name = "\"USER\"")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private Long id;
    private String name;
    private String password;
    private String roles;
    private boolean enabled;

    @EqualsAndHashCode.Exclude
    private Long manufacturerId;

    @EqualsAndHashCode.Exclude
    @CreatedDate
    private Instant createdAt;
    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    private Instant updatedAt;
}
