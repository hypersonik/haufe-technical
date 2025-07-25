package com.haufe.technical.api.service;

import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.controller.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Manufacturer;
import com.haufe.technical.api.domain.repository.BeerRepository;
import com.haufe.technical.api.domain.repository.ManufacturerRepository;
import com.haufe.technical.api.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManufacturerServiceTest {
    @Mock
    private ManufacturerRepository manufacturerRepository;

    @Mock
    private BeerRepository beerRepository;

    @InjectMocks
    private ManufacturerService manufacturerService;

    @Test
    void create_ValidManufacturer_ReturnsDto() throws ApiException {
        // Arrange
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Test Brewery", "Spain");
        Manufacturer manufacturer = Manufacturer.builder()
                .id(1L)
                .name("Test Brewery")
                .country("Spain")
                .build();
        when(manufacturerRepository.existsByName("Test Brewery")).thenReturn(false);
        when(manufacturerRepository.save(any(Manufacturer.class))).thenReturn(manufacturer);

        // Act
        ManufacturerUpsertResponseDto response = manufacturerService.create(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Test Brewery");

        verify(manufacturerRepository).save(any(Manufacturer.class));
    }

    @Test
    void create_BlankName_ThrowsApiException() {
        // Arrange
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("", "Spain");

        // Act & Assert
        assertThatThrownBy(() -> manufacturerService.create(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("Manufacturer name must not be null");

        verify(manufacturerRepository, never()).save(any());
    }

    @Test
    void create_DuplicateName_ThrowsApiException() {
        // Arrange
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Test Brewery", "Spain");
        when(manufacturerRepository.existsByName("Test Brewery")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> manufacturerService.create(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("Manufacturer with name Test Brewery already exists");

        verify(manufacturerRepository, never()).save(any());
    }

    @Test
    void update_ExistingManufacturer_UpdatesSuccessfully() throws ApiException {
        // Arrange
        Long id = 1L;
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Updated Name", "Spain");
        Manufacturer manufacturer = Manufacturer.builder()
                .id(id)
                .name("Old Name")
                .country("Portugal")
                .build();
        when(manufacturerRepository.findById(id)).thenReturn(Optional.of(manufacturer));
        when(manufacturerRepository.existsByName("Updated Name")).thenReturn(false);

        // Act
        manufacturerService.update(id, request);

        // Assert
        verify(manufacturerRepository).save(manufacturer);
        assertThat(manufacturer.getName()).isEqualTo("Updated Name");
        assertThat(manufacturer.getCountry()).isEqualTo("Spain");
    }

    @Test
    void update_NonexistentManufacturer_ThrowsApiException() {
        // Arrange
        Long id = 1L;
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Updated Name", "Spain");
        when(manufacturerRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> manufacturerService.update(id, request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", HttpStatus.NOT_FOUND)
                .hasMessageContaining("Manufacturer with id " + id + " not found");

        verify(manufacturerRepository, never()).save(any());
    }

    @Test
    void update_DuplicateName_ThrowsApiException() {
        // Arrange
        Long id = 1L;
        ManufacturerUpsertDto request = new ManufacturerUpsertDto("Updated Name", "Spain");
        Manufacturer manufacturer = Manufacturer.builder()
                .id(id)
                .name("Old Name")
                .country("Portugal")
                .build();
        when(manufacturerRepository.findById(id)).thenReturn(Optional.of(manufacturer));
        when(manufacturerRepository.existsByName("Updated Name")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> manufacturerService.update(id, request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("Manufacturer with name Updated Name already exists");

        verify(manufacturerRepository, never()).save(any());
    }

    @Test
    void delete_ManufacturerWithBeers_ThrowsApiException() {
        // Arrange
        Long id = 1L;
        when(manufacturerRepository.existsById(id)).thenReturn(true);
        when(beerRepository.existsByManufacturerId(id)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> manufacturerService.delete(id))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("Cannot delete manufacturer with id " + id + " because it has associated beers");

        verify(manufacturerRepository, never()).deleteById(any());
    }

    @Test
    void delete_NonexistentManufacturer_ThrowsApiException() {
        // Arrange
        Long id = 1L;
        when(manufacturerRepository.existsById(id)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> manufacturerService.delete(id))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", HttpStatus.NOT_FOUND)
                .hasMessageContaining("Manufacturer with id " + id + " not found");

        verify(manufacturerRepository, never()).deleteById(any());
    }

    @Test
    void read_ExistingManufacturer_ReturnsDto() throws ApiException {
        // Arrange
        Long id = 1L;
        Manufacturer manufacturer = Manufacturer.builder()
                .id(id)
                .name("Test Brewery")
                .country("Spain")
                .build();
        when(manufacturerRepository.findById(id)).thenReturn(Optional.of(manufacturer));

        // Act
        ManufacturerReadResponseDto response = manufacturerService.read(id);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Test Brewery");
        assertThat(response.country()).isEqualTo("Spain");
    }

    @Test
    void read_NonexistentManufacturer_ThrowsApiException() {
        // Arrange
        Long id = 1L;
        when(manufacturerRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> manufacturerService.read(id))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", HttpStatus.NOT_FOUND)
                .hasMessageContaining("Manufacturer with id " + id + " not found");
    }

    @Test
    void list_ReturnsPageOfManufacturers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Manufacturer> manufacturers = List.of(
                Manufacturer.builder().id(1L).name("Brewery 1").country("Spain").build(),
                Manufacturer.builder().id(2L).name("Brewery 2").country("Portugal").build()
        );
        Page<Manufacturer> page = new PageImpl<>(manufacturers);
        when(manufacturerRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<ManufacturerListResponseDto> result = manufacturerService.list(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .hasSize(2)
                .extracting(ManufacturerListResponseDto::name)
                .containsExactly("Brewery 1", "Brewery 2");
    }

    @Test
    void list_EmptyPage_ReturnsEmptyPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Manufacturer> emptyPage = new PageImpl<>(List.of());
        when(manufacturerRepository.findAll(pageable)).thenReturn(emptyPage);

        // Act
        Page<ManufacturerListResponseDto> result = manufacturerService.list(pageable);

        // Assert
        assertThat(result)
                .isNotNull()
                .hasSize(0);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(manufacturerRepository).findAll(pageable);
    }
}
