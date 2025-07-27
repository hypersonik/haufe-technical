package com.haufe.technical.api.repository;

import com.haufe.technical.api.domain.entity.Beer;
import com.haufe.technical.api.domain.entity.Manufacturer;
import com.haufe.technical.api.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;

@DataR2dbcTest
@TestPropertySource(properties = {
        "spring.r2dbc.url=r2dbc:h2:mem:///beerrepo_testdb;DB_CLOSE_DELAY=-1",
        "spring.r2dbc.username=test_user",
        "spring.r2dbc.password="
})
public class BeerRepositoryTest {

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    private Long existingManufacturerId;

    @BeforeEach
    void setUp() {
        // Clear and setup test data
        beerRepository.deleteAll()
                .then(manufacturerRepository.deleteAll())
                .then(userRepository.deleteAll())
                .block();

        // Create required users and manufacturers
        User u = User.builder()
                .name("breewerya")
                .password("{noop}testPass1")
                .roles("MANUFACTURER")
                .enabled(true)
                .build();

        Long userId = Objects.requireNonNull(userRepository.save(u).block()).getId();

        Manufacturer m = Manufacturer.builder()
                .userId(userId)
                .name("Brewery A")
                .country("US")
                .build();

        existingManufacturerId = Objects.requireNonNull(manufacturerRepository.save(m).block()).getId();
    }

    @Test
    void existsByManufacturerId_shouldReturnTrueWhenBeersExist() {
        // Given a beer with existing manufacturer
        Beer testBeer = Beer.builder()
                .name("IPA")
                .abv(6.5f)
                .style("India Pale Ale")
                .manufacturerId(existingManufacturerId)
                .build();

        // When saving and checking existence
        Mono<Boolean> result = beerRepository.save(testBeer)
                .then(beerRepository.existsByManufacturerId(existingManufacturerId));

        // Then should return true
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void save_shouldFailWithNonexistentManufacturer() {
        // Given beer with invalid manufacturer
        Beer invalidBeer = Beer.builder()
                .name("Invalid")
                .manufacturerId(999L) // Doesn't exist
                .build();

        // When saving
        Mono<Beer> result = beerRepository.save(invalidBeer);

        // Then should fail due to FK constraint
        StepVerifier.create(result)
                .expectError()
                .verify();
    }
}
