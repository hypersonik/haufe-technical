package com.haufe.technical.api.controller;

import com.haufe.technical.api.auth.HaufeUserDetails;
import com.haufe.technical.api.config.TestSecurityConfig;
import com.haufe.technical.api.domain.dto.beer.BeerListResponseDto;
import com.haufe.technical.api.domain.dto.beer.BeerReadResponseDto;
import com.haufe.technical.api.domain.dto.beer.BeerUpsertDto;
import com.haufe.technical.api.domain.dto.beer.BeerUpsertResponseDto;
import com.haufe.technical.api.exception.ApiException;
import com.haufe.technical.api.service.BeerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(BeerController.class)
@Import(TestSecurityConfig.class)
@EnableReactiveMethodSecurity
class BeerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BeerService beerService;

    private BeerUpsertDto beerUpsertDto;
    private BeerUpsertResponseDto beerUpsertResponseDto;
    private BeerReadResponseDto beerReadResponseDto;
    private BeerListResponseDto beerListResponseDto;

    @BeforeEach
    void setUp() {
        beerUpsertDto = BeerUpsertDto.builder()
                .name("Test Beer")
                .avb(5.0F)
                .style("IPA")
                .description("Test Description")
                .build();

        beerUpsertResponseDto = BeerUpsertResponseDto.builder()
                .id(1L)
                .name("Test Beer")
                .build();

        beerReadResponseDto = BeerReadResponseDto.builder()
                .name("Test Beer")
                .build();

        beerListResponseDto = BeerListResponseDto.builder()
                .id(1L)
                .name("Test Beer")
                .build();
    }

    @Nested
    class CreateEndpointTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        void create_WithValidData_ShouldReturnCreatedBeer() throws ApiException {
            Long manufacturerId = 1L;
            when(beerService.create(eq(manufacturerId), any(BeerUpsertDto.class)))
                    .thenReturn(Mono.just(beerUpsertResponseDto));

            webTestClient.post()
                    .uri("/api/beer/{manufacturerId}", manufacturerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(beerUpsertDto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(BeerUpsertResponseDto.class)
                    .isEqualTo(beerUpsertResponseDto);
        }

        private static Stream<Arguments> create_WithoutProperRole_ShouldFail() {
            return Stream.of(
                    Arguments.of(HaufeUserDetails.builder()
                            .id(1L)
                            .manufacturerId(2L)
                            .build()
                            .roles("MANUFACTURER"), HttpStatus.FORBIDDEN), // Forbidden for a Manufacturer role without matching ID
                    Arguments.of(null, HttpStatus.UNAUTHORIZED) // Unauthorized for anonymous user
            );
        }

        @ParameterizedTest
        @MethodSource
        void create_WithoutProperRole_ShouldFail(UserDetails userDetails, HttpStatus expectedStatus) {
            Long manufacturerId = 1L;

            if (userDetails != null) {
                webTestClient = webTestClient.mutateWith(mockUser(userDetails));
            }

            webTestClient.post()
                    .uri("/api/beer/{manufacturerId}", manufacturerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(beerUpsertDto)
                    .exchange()
                    .expectStatus().isEqualTo(expectedStatus);
        }
    }

    @Nested
    class UpdateEndpointTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        void update_WithValidData_ShouldReturnUpdatedBeer() {
            Long beerId = 1L;
            when(beerService.update(eq(beerId), any(BeerUpsertDto.class)))
                    .thenReturn(Mono.just(beerUpsertResponseDto));

            webTestClient.put()
                    .uri("/api/beer/{id}", beerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(beerUpsertDto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(BeerUpsertResponseDto.class)
                    .isEqualTo(beerUpsertResponseDto);
        }

        @Test
        @WithMockUser(roles = "MANUFACTURER")
        void update_WithNonexistentId_ShouldReturnNotFound() {
            Long beerId = 999L;
            when(beerService.update(eq(beerId), any(BeerUpsertDto.class)))
                    .thenReturn(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Beer not found")));

            webTestClient.put()
                    .uri("/api/beer/{id}", beerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(beerUpsertDto)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        private static Stream<Arguments> update_WithoutProperRole_ShouldFail() {
            return Stream.of(
                    Arguments.of("USER", HttpStatus.FORBIDDEN), // Forbidden for a regular user
                    Arguments.of(null, HttpStatus.UNAUTHORIZED) // Unauthorized for anonymous user
            );
        }

        @ParameterizedTest
        @MethodSource
        void update_WithoutProperRole_ShouldFail(String roles, HttpStatus expectedStatus) {
            Long beerId = 1L;

            if (roles != null) {
                webTestClient = webTestClient.mutateWith(mockUser().roles(roles));
            }

            webTestClient.put()
                    .uri("/api/beer/{id}", beerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(beerUpsertDto)
                    .exchange()
                    .expectStatus().isEqualTo(expectedStatus);
        }
    }

    @Nested
    class ReadEndpointTests {
        @Test
        void read_ExistingBeer_ShouldReturnBeer() {
            Long beerId = 1L;
            when(beerService.read(beerId))
                    .thenReturn(Mono.just(beerReadResponseDto));

            webTestClient.get()
                    .uri("/api/beer/{id}", beerId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(BeerReadResponseDto.class)
                    .isEqualTo(beerReadResponseDto);
        }

        @Test
        void read_NonexistentBeer_ShouldReturnNotFound() {
            Long beerId = 999L;
            when(beerService.read(beerId))
                    .thenReturn(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Beer not found")));

            webTestClient.get()
                    .uri("/api/beer/{id}", beerId)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    class ListEndpointTests {
        @Test
        void list_WithData_ShouldReturnBeersList() {
            when(beerService.list(any(), any(), any(), any(),any(), any()))
                    .thenReturn(Flux.just(beerListResponseDto));

            webTestClient.get()
                    .uri("/api/beer")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(BeerListResponseDto.class)
                    .hasSize(1)
                    .contains(beerListResponseDto);
        }

        @Test
        void list_WithNoData_ShouldReturnEmptyList() {
            when(beerService.list(any(), any(), any(), any(),any(), any()))
                    .thenReturn(Flux.empty());

            webTestClient.get()
                    .uri("/api/beer")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(BeerListResponseDto.class)
                    .hasSize(0);
        }
    }

    @Nested
    class DeleteEndpointTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        void delete_ExistingBeer_ShouldReturnNoContent() {
            Long beerId = 1L;
            when(beerService.delete(beerId))
                    .thenReturn(Mono.empty());

            webTestClient.delete()
                    .uri("/api/beer/{id}", beerId)
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void delete_NonexistentBeer_ShouldReturnNotFound() {
            Long beerId = 999L;
            when(beerService.delete(beerId))
                    .thenReturn(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Beer not found")));

            webTestClient.delete()
                    .uri("/api/beer/{id}", beerId)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @WithMockUser(roles = "USER")
        void delete_WithoutProperRole_ShouldReturnForbidden() {
            Long beerId = 1L;

            webTestClient.delete()
                    .uri("/api/beer/{id}", beerId)
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }
}