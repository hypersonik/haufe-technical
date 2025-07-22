package com.haufe.technical.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haufe.technical.api.config.WebSecurityConfig;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.service.ManufacturerService;
import com.haufe.technical.api.utils.RestResponsePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebFluxTest(ManufacturerController.class)
//@Import({WebSecurityConfig.class})
class ManufacturerController2Test {
    private static final long THE_ID = 1L;
    private static final String THE_MANUFACTURER = "The Manufacturer";
    private static final String THE_COUNTRY = "The Country";

    @MockitoBean
    private ManufacturerService manufacturerService;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(
                new SpringDataJacksonConfiguration.PageModule(
                        new SpringDataWebSettings(EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)));
    }

    @Test
    //@WithMockUser(username = "admin", roles = "ADMIN")
    void testCreate() throws Exception {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto(THE_MANUFACTURER, THE_COUNTRY);
        ManufacturerUpsertResponseDto response = new ManufacturerUpsertResponseDto(THE_ID, THE_MANUFACTURER);
        when(manufacturerService.create(any(ManufacturerUpsertDto.class))).thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(mockUser("admin").roles("ADMIN"))
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
/*

    @Test
    void testUpdate() throws Exception {
        ManufacturerUpsertDto request = new ManufacturerUpsertDto(THE_MANUFACTURER, THE_COUNTRY);
        when(manufacturerService.update(anyLong(), any(ManufacturerUpsertDto.class)))
                .thenReturn(Mono.just(new ManufacturerUpsertResponseDto(THE_ID, THE_MANUFACTURER)));

        mockMvc.perform(put("/api/manufacturer/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(manufacturerService).update(eq(THE_ID), any(ManufacturerUpsertDto.class));
    }

    @Test
    void testRead() throws Exception {
        ManufacturerReadResponseDto response = new ManufacturerReadResponseDto(THE_MANUFACTURER, THE_COUNTRY);
        when(manufacturerService.read(anyLong())).thenReturn(Mono.just(response));

        mockMvc.perform(get("/api/manufacturer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(THE_MANUFACTURER))
                .andExpect(jsonPath("$.country").value(THE_COUNTRY));

        verify(manufacturerService).read(1L);
    }

    @Test
    void testList() throws Exception {
        List<ManufacturerListResponseDto> listResponseDtos = buildManufacturerList(10);
        when(manufacturerService.list(any(Pageable.class))).thenReturn(Flux.fromIterable(listResponseDtos));

        String json = mockMvc.perform(get("/api/manufacturer")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(manufacturerService).list(any());

        TypeReference<RestResponsePage<ManufacturerListResponseDto>> typeRef = new TypeReference<>() {};
        Page<ManufacturerListResponseDto> response = objectMapper.readValue(json, typeRef);
        List<ManufacturerListResponseDto> content = response.getContent();
        for (int i = 0; i < listResponseDtos.size(); ++i) {
            ManufacturerListResponseDto expected = listResponseDtos.get(i);
            ManufacturerListResponseDto actual = content.get(i);
            assertThat(actual.id()).isEqualTo(expected.id());
            assertThat(actual.name()).isEqualTo(expected.name());
            assertThat(actual.country()).isEqualTo(expected.country());
        }
    }

    @Test
    void testDelete() throws Exception {
        when(manufacturerService.delete(anyLong())).thenReturn(Mono.empty());

        mockMvc.perform(delete("/api/manufacturer/1")).andExpect(status().isOk());
        verify(manufacturerService).delete(1L);
    }

    private static List<ManufacturerListResponseDto> buildManufacturerList(int size) {
        return IntStream.rangeClosed(1, size)
                .mapToObj(i -> new ManufacturerListResponseDto((long) i, "Manufacturer " + i, "Country " + i))
                .toList();
    }
*/
}
