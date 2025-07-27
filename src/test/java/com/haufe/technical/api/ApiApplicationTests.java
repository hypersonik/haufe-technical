package com.haufe.technical.api;

import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Manufacturer;
import com.haufe.technical.api.domain.entity.User;
import com.haufe.technical.api.repository.ManufacturerRepository;
import com.haufe.technical.api.repository.UserRepository;
import com.haufe.technical.api.utils.RestResponsePage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
@EnableReactiveMethodSecurity
class ApiApplicationTests {
	private static final ParameterizedTypeReference<RestResponsePage<ManufacturerListResponseDto>>
			REST_RESPONSE_PAGE_TYPE_REFERENCE = new ParameterizedTypeReference<>() {};

	private static final String THE_USERNAME = "manufacturer_user";
	private static final String THE_MANUFACTURER = "The Manufacturer";
	private static final String THE_COUNTRY = "The Country";
	public static final String THE_PASSWORD = "1234";

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ManufacturerRepository manufacturerRepository;

	@Test
	void contextLoads() {
		// This test is just to ensure the application context loads correctly
	}

	@Nested
	class ManufacturerTests {
		@Test
		void createManufacturer_shouldReturnUnauthorized() {
			ManufacturerUpsertDto request = new ManufacturerUpsertDto(
					THE_USERNAME, THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);

			webTestClient
					.post().uri("/api/manufacturer")
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(request)
					.exchange()
					.expectStatus().isUnauthorized();
		}

		@Test
		@WithMockUser(roles = "MANUFACTURER")
		void createManufacturer_shouldReturnForbidden() {
			ManufacturerUpsertDto request = new ManufacturerUpsertDto(
					THE_USERNAME, THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);

			webTestClient
					.post().uri("/api/manufacturer")
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(request)
					.exchange()
					.expectStatus().isForbidden();
		}

		@Test
		@WithMockUser(roles = "ADMIN")
		void createManufacturer_shouldReturnBadRequestForInvalidData() {
			ManufacturerUpsertDto request = new ManufacturerUpsertDto(
					"", THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);

			webTestClient
					.post().uri("/api/manufacturer")
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(request)
					.exchange()
					.expectStatus().isBadRequest();
		}

		@Test
		@WithMockUser(roles = "ADMIN")
		void createManufacturer_shouldReturnCreated() {
			ManufacturerUpsertDto request = new ManufacturerUpsertDto(
					THE_USERNAME, THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);

			StepVerifier.create(webTestClient
							.post().uri("/api/manufacturer")
							.contentType(MediaType.APPLICATION_JSON)
							.bodyValue(request)
							.exchange()
							.expectStatus().isOk()
							.returnResult(ManufacturerUpsertResponseDto.class)
							.getResponseBody()
							.next()
					)
					.assertNext(response -> {
						assertThat(response.id()).isNotNull();
						assertThat(response.name()).isEqualTo(THE_MANUFACTURER);

						Manufacturer manufacturer = manufacturerRepository.findById(response.id()).block();
						assertThat(manufacturer).isNotNull();
						assertThat(manufacturer.getName()).isEqualTo(THE_MANUFACTURER);
						assertThat(manufacturer.getCountry()).isEqualTo(THE_COUNTRY);

						User user = userRepository.findById(manufacturer.getUserId()).block();
                        Assertions.assertNotNull(user);
                        assertThat(user.getName()).isEqualTo(THE_USERNAME);
						assertThat(user.getPassword()).isNotEqualTo(THE_PASSWORD); // Password should be hashed
						assertThat(user.getRoles()).isEqualTo("MANUFACTURER");

						// Clean up created entities
						manufacturerRepository.delete(manufacturer).block();
						userRepository.delete(user).block();
					})
					.verifyComplete();
		}

		@Test
		@WithMockUser(roles = "ADMIN")
		void updateManufacturer_shouldReturnNotFound() {
			ManufacturerUpsertDto request = new ManufacturerUpsertDto(
					THE_USERNAME, THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);

			webTestClient
					.put().uri("/api/manufacturer/999")
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(request)
					.exchange()
					.expectStatus().isNotFound();
		}

		@Test
		@WithMockUser(roles = "ADMIN")
		void updateManufacturer_shouldReturnModified() {
			ManufacturerUpsertDto request = new ManufacturerUpsertDto(
					THE_USERNAME, THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);

			StepVerifier.create(webTestClient
							.post().uri("/api/manufacturer")
							.contentType(MediaType.APPLICATION_JSON)
							.bodyValue(request)
							.exchange()
							.expectStatus().isOk()
							.returnResult(ManufacturerUpsertResponseDto.class)
							.getResponseBody()
							.next()
							.flatMap(response -> {
								Long manufacturerId = response.id();
								// Now update the manufacturer
								return webTestClient
										.put().uri("/api/manufacturer/" + manufacturerId)
										.contentType(MediaType.APPLICATION_JSON)
										.bodyValue(request.toBuilder()
												.userName("updated_user")
												.password("updated_password")
												.userEnabled(false)
												.country("Updated Country")
												.name("Updated Manufacturer")
												.build())
										.exchange()
										.expectStatus().isOk()
										.returnResult(ManufacturerUpsertResponseDto.class)
										.getResponseBody()
										.next();
							})
					)
					.assertNext(response -> {
						assertThat(response.id()).isNotNull();
						assertThat(response.name()).isEqualTo("Updated Manufacturer");

						Manufacturer manufacturer = manufacturerRepository.findById(response.id()).block();
						assertThat(manufacturer).isNotNull();
						assertThat(manufacturer.getName()).isEqualTo("Updated Manufacturer");
						assertThat(manufacturer.getCountry()).isEqualTo("Updated Country");

						User user = userRepository.findById(manufacturer.getUserId()).block();
						Assertions.assertNotNull(user);
						assertThat(user.getName()).isEqualTo("updated_user");
						assertThat(user.getPassword()).isNotEqualTo("updated_password"); // Password should be hashed
						assertThat(user.getRoles()).isEqualTo("MANUFACTURER");

						// Clean up created entities
						manufacturerRepository.delete(manufacturer).block();
						userRepository.delete(user).block();
					})
					.verifyComplete();
		}

		@Test
		void readManufacturer_shouldReturnNotFound() {
			webTestClient
					.get().uri("/api/manufacturer/999")
					.exchange()
					.expectStatus().isNotFound();
		}

		@Test
		@WithMockUser(roles = "ADMIN")
		void readManufacturer_shouldReturnManufacturer() {
			ManufacturerUpsertDto request = new ManufacturerUpsertDto(
					THE_USERNAME, THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);
			StepVerifier.create(webTestClient
							.post().uri("/api/manufacturer")
							.contentType(MediaType.APPLICATION_JSON)
							.bodyValue(request)
							.exchange()
							.expectStatus().isOk()
							.returnResult(ManufacturerUpsertResponseDto.class)
							.getResponseBody()
							.next()
							.flatMap(response -> {
								Long manufacturerId = response.id();
								return webTestClient
										.get().uri("/api/manufacturer/" + manufacturerId)
										.exchange()
										.expectStatus().isOk()
										.returnResult(ManufacturerReadResponseDto.class)
										.getResponseBody()
										.next();
							})
					)
					.assertNext(response -> {
						assertThat(response.name()).isEqualTo(THE_MANUFACTURER);
						assertThat(response.country()).isEqualTo(THE_COUNTRY);
					})
					.verifyComplete();
		}

		@Test
		@WithMockUser(roles = "ADMIN")
		void listManufacturers_shouldReturnList() {
			webTestClient
					.get().uri(uriBuilder -> uriBuilder.path("/api/manufacturer")
							.queryParam("page", "0")
							.queryParam("size", "5")
							.queryParam("sort", "name")
							.build())
					.exchange()
					.expectStatus().isOk()
					.expectBody(REST_RESPONSE_PAGE_TYPE_REFERENCE)
					.consumeWith(responseList -> {
						RestResponsePage<ManufacturerListResponseDto> page = responseList.getResponseBody();
						assertThat(page).isNotNull();
						List<ManufacturerListResponseDto> content = page.getContent();
						assertThat(content)
								.isNotNull()
								.hasSize(5);
						content.forEach(manufacturer -> {
							assertThat(manufacturer.id()).isNotNull();
							assertThat(manufacturer.name()).isNotBlank();
							assertThat(manufacturer.country()).isNotBlank();
						});
					});
		}
	}
}
