package com.haufe.technical.api.repository;

import com.haufe.technical.api.domain.entity.Manufacturer;
import com.haufe.technical.api.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@TestPropertySource(properties = {
        "spring.r2dbc.url=r2dbc:h2:mem:///userrepo_testdb;DB_CLOSE_DELAY=-1",
        "spring.r2dbc.username=test_user",
        "spring.r2dbc.password="
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    private final Instant testInstant = Instant.now();

    // Test data
    // These IDs will be set after the setup method runs
    private Long adminId;
    private Long manufacturerId;
    private Long disabledId;

    @BeforeEach
    void setup() {
        // Clear and setup test data
        manufacturerRepository.deleteAll()
                .then(userRepository.deleteAll())
                .block();

        // Insert test users
        adminId = userRepository.save(User.builder()
                .name("admin")
                .password("{noop}adminPass")
                .roles("ADMIN")
                .enabled(true)
                .createdAt(testInstant)
                .updatedAt(testInstant)
                .build())
                .map(User::getId)
                .block();
        assertThat(adminId).isNotNull();

        manufacturerId = userRepository.save(User.builder()
                .name("manufacturer1")
                .password("{noop}manuPass")
                .roles("MANUFACTURER")
                .enabled(true)
                .createdAt(testInstant)
                .updatedAt(testInstant)
                .build())
                .map(User::getId)
                .block();
        assertThat(manufacturerId).isNotNull();

        disabledId = userRepository.save(User.builder()
                .name("disabled")
                .password("{noop}disabledPass")
                .roles("MANUFACTURER")
                .enabled(false)
                .createdAt(testInstant)
                .updatedAt(testInstant)
                .build())
                .map(User::getId)
                .block();
        assertThat(disabledId).isNotNull();

        // Insert manufacturer for manufacturer user
        manufacturerRepository.save(Manufacturer.builder()
                .userId(manufacturerId)
                .name("Best Brewery")
                .country("Spain")
                .createdAt(testInstant)
                .updatedAt(testInstant)
                .build())
                .block();
    }

    @Test
    void findByName_shouldReturnAdminUserWithCorrectRole() {
        StepVerifier.create(userRepository.findByName("admin"))
                .expectNextMatches(user ->
                        user.getId() != null &&
                                user.getId().equals(adminId) &&
                                user.getName().equals("admin") &&
                                user.getPassword().equals("{noop}adminPass") &&
                                user.getRoles().equals("ADMIN") &&
                                user.isEnabled())
                .verifyComplete();
    }

    @Test
    void findByNameAndEnabled_shouldReturnManufacturerDto() {
        StepVerifier.create(userRepository.findByNameAndEnabled("manufacturer1"))
                .expectNextMatches(dto ->
                        dto.id() != null &&
                                dto.manufacturerId() != null &&
                                dto.id().equals(manufacturerId) &&
                                dto.name().equals("manufacturer1") &&
                                dto.password().equals("{noop}manuPass") &&
                                dto.roles().equals("MANUFACTURER") &&
                                dto.enabled())
                .verifyComplete();
    }

    @Test
    void existsByNameAndIdNot_shouldWorkCorrectly() {
        userRepository.existsByNameAndIdNot("admin", adminId + 1)
                .as(StepVerifier::create)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void findByNameAndEnabled_shouldNotReturnDisabledUsers() {
        StepVerifier.create(userRepository.findByNameAndEnabled("disabled"))
                .verifyComplete();
    }
}
