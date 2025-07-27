package com.haufe.technical.api.controller;

import com.haufe.technical.api.auth.HaufeUserDetails;
import com.haufe.technical.api.config.TestSecurityConfig;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.service.ManufacturerService;
import com.haufe.technical.api.utils.RestResponsePage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(controllers = ManufacturerController.class)
@Import(TestSecurityConfig.class)
@EnableReactiveMethodSecurity
class ManufacturerControllerTest {
    private static final ParameterizedTypeReference<RestResponsePage<ManufacturerListResponseDto>>
            REST_RESPONSE_PAGE_TYPE_REFERENCE = new ParameterizedTypeReference<>() {};

    private static final long THE_ID = 1L;
    private static final String THE_USERNAME = "manufacturer_user";
    private static final String THE_MANUFACTURER = "The Manufacturer";
    private static final String THE_COUNTRY = "The Country";
    public static final String THE_PASSWORD = "1234";

    @MockitoBean
    private ManufacturerService manufacturerService;

    @Autowired
    private WebTestClient webTestClient;

    @Nested
    class CreateTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        void testCreate() {
            ManufacturerUpsertDto request = new ManufacturerUpsertDto(
                    THE_USERNAME, THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);
            ManufacturerUpsertResponseDto response = new ManufacturerUpsertResponseDto(THE_ID, THE_MANUFACTURER);
            when(manufacturerService.create(any(ManufacturerUpsertDto.class))).thenReturn(Mono.just(response));

            webTestClient
                    .post().uri("/api/manufacturer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(THE_ID)
                    .jsonPath("$.name").isEqualTo(THE_MANUFACTURER);

            verify(manufacturerService).create(any());
        }

        private static Stream<Arguments> testCreateWithWrongRole() {
            return Stream.of(
                    Arguments.of("MANUFACTURER", HttpStatus.FORBIDDEN), // Manufacturer role
                    Arguments.of(null, HttpStatus.UNAUTHORIZED)         // Anonymous user
            );
        }

        @ParameterizedTest
        @MethodSource
        void testCreateWithWrongRole(String roles, HttpStatus expectedStatus) {
            ManufacturerUpsertDto request = new ManufacturerUpsertDto(
                    THE_USERNAME, THE_PASSWORD, true, THE_MANUFACTURER, null);

            if (roles != null) {
                webTestClient = webTestClient.mutateWith(mockUser().roles(roles));
            }

            webTestClient
                    .post().uri("/api/manufacturer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isEqualTo(expectedStatus);

            verify(manufacturerService, never()).create(any());
        }

        @Test
        void testCreateWithAnonymousRole() {
            ManufacturerUpsertDto request = new ManufacturerUpsertDto(
                    THE_USERNAME, THE_PASSWORD, true, THE_MANUFACTURER, null);

            webTestClient
                    .post().uri("/api/manufacturer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isUnauthorized();

            verify(manufacturerService, never()).create(any());
        }
    }

    @Nested
    class UpdateTests {
        private static Stream<Arguments> testUpdate() {
            return Stream.of(
                    Arguments.of(HaufeUserDetails.builder()
                            .id(THE_ID)
                            .manufacturerId(THE_ID)
                            .username("manufacturer")
                            .build()
                            .roles("MANUFACTURER")), // Manufacturer role with matching ID
                    Arguments.of(HaufeUserDetails.builder().build().roles("ADMIN")) // Admin user
            );
        }

        @ParameterizedTest
        @MethodSource
        void testUpdate(UserDetails userDetails) {
            ManufacturerUpsertDto request = new ManufacturerUpsertDto(
                    THE_USERNAME, THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);
            when(manufacturerService.update(anyLong(), any(ManufacturerUpsertDto.class)))
                    .thenReturn(Mono.just(new ManufacturerUpsertResponseDto(THE_ID, THE_MANUFACTURER)));

            if (userDetails != null) {
                webTestClient = webTestClient.mutateWith(mockUser(userDetails));
            }

            webTestClient
                    .put().uri("/api/manufacturer/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(THE_ID)
                    .jsonPath("$.name").isEqualTo(THE_MANUFACTURER);

            verify(manufacturerService).update(eq(THE_ID), any(ManufacturerUpsertDto.class));
        }

        private static Stream<Arguments> testUpdate_WrongRole() {
            return Stream.of(
                    Arguments.of(HaufeUserDetails.builder()
                            .id(THE_ID)
                            .manufacturerId(THE_ID)
                            .build()
                            .roles("MANUFACTURER"), HttpStatus.FORBIDDEN), // Forbidden for a Manufacturer role without matching ID
                    Arguments.of(null, HttpStatus.UNAUTHORIZED) // Unauthorized for anonymous user
            );
        }

        @ParameterizedTest
        @MethodSource
        void testUpdate_WrongRole(UserDetails userDetails, HttpStatus expectedStatus) {
            ManufacturerUpsertDto request = new ManufacturerUpsertDto(
                    THE_USERNAME, THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);

            when(manufacturerService.update(anyLong(), any(ManufacturerUpsertDto.class)))
                    .thenReturn(Mono.just(new ManufacturerUpsertResponseDto(THE_ID, THE_MANUFACTURER)));

            if (userDetails != null) {
                webTestClient = webTestClient.mutateWith(mockUser(userDetails));
            }

            webTestClient
                    .put().uri("/api/manufacturer/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isEqualTo(expectedStatus);

            verify(manufacturerService, never()).update(anyLong(), any(ManufacturerUpsertDto.class));
        }
    }

    @Nested
    class ReadTests {
        @Test
        void testRead() {
            ManufacturerReadResponseDto response = new ManufacturerReadResponseDto(THE_MANUFACTURER, THE_COUNTRY);
            when(manufacturerService.read(anyLong())).thenReturn(Mono.just(response));

            webTestClient
                    .get().uri("/api/manufacturer/1")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo(THE_MANUFACTURER)
                    .jsonPath("$.country").isEqualTo(THE_COUNTRY);

            verify(manufacturerService).read(1L);
        }
    }

    @Nested
    class ListTests {
        @Test
        void testList() {
            List<ManufacturerListResponseDto> listResponseDtos = buildManufacturerList(10);
            doAnswer(invocation -> {
                Pageable pageable = invocation.getArgument(0, Pageable.class);
                return Mono.just(new PageImpl<>(listResponseDtos, pageable, listResponseDtos.size()));
            }).when(manufacturerService).list(any(Pageable.class));

            webTestClient
                    .get().uri(uriBuilder -> uriBuilder.path("/api/manufacturer")
                            .queryParam("page", "0")
                            .queryParam("size", "10")
                            .queryParam("sort", "name")
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(REST_RESPONSE_PAGE_TYPE_REFERENCE)
                    .consumeWith(response -> checkListResponse(response, listResponseDtos));

            verify(manufacturerService).list(any(Pageable.class));
        }

        private static List<ManufacturerListResponseDto> buildManufacturerList(int size) {
            return IntStream.rangeClosed(1, size)
                    .mapToObj(i -> new ManufacturerListResponseDto((long) i, "Manufacturer " + i, "Country " + i))
                    .toList();
        }

        private static void checkListResponse(EntityExchangeResult<RestResponsePage<ManufacturerListResponseDto>> response,
                                              List<ManufacturerListResponseDto> listResponseDtos) {
            RestResponsePage<ManufacturerListResponseDto> responseBody = response.getResponseBody();
            Assertions.assertNotNull(responseBody);
            List<ManufacturerListResponseDto> content = responseBody.getContent();
            Assertions.assertNotNull(content);
            assertThat(content).hasSize(listResponseDtos.size());
            for (int i = 0; i < listResponseDtos.size(); ++i) {
                ManufacturerListResponseDto expected = listResponseDtos.get(i);
                ManufacturerListResponseDto actual = content.get(i);
                assertThat(actual.id()).isEqualTo(expected.id());
                assertThat(actual.name()).isEqualTo(expected.name());
                assertThat(actual.country()).isEqualTo(expected.country());
            }
        }
    }

    @Nested
    class DeleteTests {
        private static Stream<Arguments> testDelete() {
            return Stream.of(
                    Arguments.of("ADMIN", HttpStatus.OK, times(1)), // Admin role
                    Arguments.of("MANUFACTURER", HttpStatus.FORBIDDEN, never()),           // Manufacturer role
                    Arguments.of(null, HttpStatus.UNAUTHORIZED, never())                   // Anonymous user
            );
        }

        @ParameterizedTest
        @MethodSource
        void testDelete(String roles, HttpStatus expectedStatus, VerificationMode times) {
            when(manufacturerService.delete(anyLong())).thenReturn(Mono.empty());

            if (roles != null) {
                webTestClient = webTestClient.mutateWith(mockUser().roles(roles));
            }

            webTestClient
                    .delete().uri("/api/manufacturer/1")
                    .exchange()
                    .expectStatus().isEqualTo(expectedStatus);

            verify(manufacturerService, times).delete(1L);
        }
    }
}
