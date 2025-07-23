package com.haufe.technical.api.controller;

import com.haufe.technical.api.config.TestSecurityConfig;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.service.ManufacturerService;
import com.haufe.technical.api.utils.RestResponsePage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(controllers = ManufacturerController.class)
@Import(TestSecurityConfig.class)
class ManufacturerControllerTest {
    private static final ParameterizedTypeReference<RestResponsePage<ManufacturerListResponseDto>>
            REST_RESPONSE_PAGE_TYPE_REFERENCE = new ParameterizedTypeReference<>() {};

    private static final long THE_ID = 1L;
    private static final String THE_MANUFACTURER = "The Manufacturer";
    private static final String THE_COUNTRY = "The Country";

    @MockitoBean
    private ManufacturerService manufacturerService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testCreate() {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto(THE_MANUFACTURER, THE_COUNTRY);
        ManufacturerUpsertResponseDto response = new ManufacturerUpsertResponseDto(THE_ID, THE_MANUFACTURER);
        when(manufacturerService.create(any(ManufacturerUpsertDto.class))).thenReturn(Mono.just(response));

        webTestClient
                .post().uri("/api/manufacturer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(THE_ID)
                .jsonPath("$.name").isEqualTo(THE_MANUFACTURER);

        verify(manufacturerService).create(any());
    }

    @Test
    void testUpdate() {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto(THE_MANUFACTURER, THE_COUNTRY);
        when(manufacturerService.update(anyLong(), any(ManufacturerUpsertDto.class)))
                .thenReturn(Mono.just(new ManufacturerUpsertResponseDto(THE_ID, THE_MANUFACTURER)));

        webTestClient
                .mutateWith(mockUser("admin").roles("ADMIN"))
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

    @Test
    //@WithMockUser(username = "admin", roles = "ADMIN")
    void testRead() {
        ManufacturerReadResponseDto response = new ManufacturerReadResponseDto(THE_MANUFACTURER, THE_COUNTRY);
        when(manufacturerService.read(anyLong())).thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(mockUser("admin").roles("ADMIN"))
                .get().uri("/api/manufacturer/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo(THE_MANUFACTURER)
                .jsonPath("$.country").isEqualTo(THE_COUNTRY);

        verify(manufacturerService).read(1L);
    }

    @Test
    void testList() {
        List<ManufacturerListResponseDto> listResponseDtos = buildManufacturerList(10);
        doAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0, Pageable.class);
            return Mono.just(new PageImpl<>(listResponseDtos, pageable, listResponseDtos.size()));
        }).when(manufacturerService).list(any(Pageable.class));

        webTestClient
                .mutateWith(mockUser("admin").roles("ADMIN"))
                .get().uri(uriBuilder -> uriBuilder.path("/api/manufacturer")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("sort", "name")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(REST_RESPONSE_PAGE_TYPE_REFERENCE)
                .consumeWith(response -> {
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
                });

        verify(manufacturerService).list(any(Pageable.class));
    }
    @Test
    void testDelete() {
        when(manufacturerService.delete(anyLong())).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockUser("admin").roles("ADMIN"))
                .delete().uri("/api/manufacturer/1")
                .exchange()
                .expectStatus().isOk();

        verify(manufacturerService).delete(1L);
    }

    private static List<ManufacturerListResponseDto> buildManufacturerList(int size) {
        return IntStream.rangeClosed(1, size)
                .mapToObj(i -> new ManufacturerListResponseDto((long) i, "Manufacturer " + i, "Country " + i))
                .toList();
    }
}
