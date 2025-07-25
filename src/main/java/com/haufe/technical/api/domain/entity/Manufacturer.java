package com.haufe.technical.api.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Data
@ToString(exclude = {"beers"})
@NoArgsConstructor
@AllArgsConstructor
public class Manufacturer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String name;
    private String country;

    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "manufacturer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Beer> beers = new ArrayList<>();

    @EqualsAndHashCode.Exclude
    @CreationTimestamp
    private Instant createdAt;
    @EqualsAndHashCode.Exclude
    @UpdateTimestamp
    private Instant updatedAt;

    public void addBeer(Beer beer) {
        beers.add(beer);
        beer.setManufacturer(this);
    }

    public void removeBeer(Beer beer) {
        beers.remove(beer);
        beer.setManufacturer(null);
    }
}
