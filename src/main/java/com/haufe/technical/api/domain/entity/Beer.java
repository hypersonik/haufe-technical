package com.haufe.technical.api.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Builder
@Data
@ToString(exclude = {"manufacturer"})
@NoArgsConstructor
@AllArgsConstructor
public class Beer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String name;
    private Float abv;
    private String style;
    private String description;

    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "manufacturer_id")
    private Manufacturer manufacturer;

    @EqualsAndHashCode.Exclude
    @CreationTimestamp
    private Instant createdAt;
    @EqualsAndHashCode.Exclude
    @UpdateTimestamp
    private Instant updatedAt;
}
