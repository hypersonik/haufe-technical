package com.haufe.technical.api;

import com.haufe.technical.api.auth.HaufeUserDetails;
import com.haufe.technical.api.domain.dto.beer.BeerReadResponseDto;
import com.haufe.technical.api.domain.dto.beer.BeerUpsertDto;
import com.haufe.technical.api.domain.dto.beer.BeerUpsertResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerListResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerReadResponseDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertDto;
import com.haufe.technical.api.domain.dto.manufacturer.ManufacturerUpsertResponseDto;
import com.haufe.technical.api.domain.entity.Manufacturer;
import com.haufe.technical.api.domain.entity.User;
import com.haufe.technical.api.repository.BeerRepository;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

/**
 * This class contains integration tests for the API application.
 * It uses WebTestClient to perform HTTP requests and verify responses.
 * The tests cover various scenarios including creation, updating, reading, and listing manufacturers.
 */

@SpringBootTest
@AutoConfigureWebTestClient
@EnableReactiveMethodSecurity
class ApiApplicationTests {
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
	@Autowired
    private BeerRepository beerRepository;
	@Test
	void contextLoads() {
		// This test is just to ensure the application context loads correctly
	}

	@Nested
	class ManufacturerTests {
		private static final ParameterizedTypeReference<RestResponsePage<ManufacturerListResponseDto>>
				BODY_TYPE = new ParameterizedTypeReference<>() {};

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
					"cm-ok", THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);

			StepVerifier.create(createManufacturer(request)
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
                        assertThat(user.getName()).isEqualTo("cm-ok");
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
					"um-modified", THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);

			StepVerifier.create(createManufacturer(request)
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
					"rm-ok", THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);
			StepVerifier.create(createManufacturer(request)
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
					.expectBody(BODY_TYPE)
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

		@Test
		@WithMockUser(roles = "ADMIN")
		void deleteManufacturer_shouldDeleteManufacturer() {
			ManufacturerUpsertDto request = new ManufacturerUpsertDto(
					"dm-ok", THE_PASSWORD, true, THE_MANUFACTURER, THE_COUNTRY);

			StepVerifier.create(createManufacturer(request)
							.flatMap(response -> {
								Long manufacturerId = response.id();
								return webTestClient
										.delete().uri("/api/manufacturer/" + manufacturerId)
										.exchange()
										.expectStatus().isNoContent()
										.returnResult(Void.class)
										.getResponseBody()
										.then(Mono.just(response));
							})
					)
					.assertNext(dto ->
							assertThat(manufacturerRepository.existsById(dto.id()).block()).isFalse())
					.verifyComplete();
		}

		private Mono<ManufacturerUpsertResponseDto> createManufacturer(ManufacturerUpsertDto request) {
			return webTestClient
					.post().uri("/api/manufacturer")
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(request)
					.exchange()
					.expectStatus().isCreated()
					.returnResult(ManufacturerUpsertResponseDto.class)
					.getResponseBody()
					.next();
		}
	}

	@Nested
	class BeerTests {
		public static final ParameterizedTypeReference<RestResponsePage<BeerReadResponseDto>>
				BODY_TYPE = new ParameterizedTypeReference<>() {};

		@Test
		@WithMockUser(roles = "ADMIN")
		void createBeer_shouldReturnCreated() {
			BeerUpsertDto request = BeerUpsertDto.builder()
					.name("Test Beer")
					.abv(5.0f)
					.style("IPA")
					.description("A test beer for integration testing")
					.build();

			StepVerifier.create(createBeer(request))
					.assertNext(dto -> {
						assertThat(dto.id()).isNotNull();
						assertThat(dto.name()).isEqualTo("Test Beer");

						beerRepository.findById(dto.id()).blockOptional().ifPresentOrElse(beer -> {
							assertThat(beer.getName()).isEqualTo(request.name());
							assertThat(beer.getAbv()).isEqualTo(request.abv());
							assertThat(beer.getStyle()).isEqualTo(request.style());
							assertThat(beer.getDescription()).isEqualTo(request.description());

							// Clean up created beer
							beerRepository.delete(beer).block();
						}, () -> Assertions.fail("Beer not found in the database after creation"));
					})
					.verifyComplete();
		}

		@Test
		@WithMockUser(roles = "ADMIN")
		void updateBeer_shouldReturnModified() {
			BeerUpsertDto request = BeerUpsertDto.builder()
					.name("Update Test Beer")
					.abv(6.0f)
					.style("Stout")
					.description("An updated test beer for integration testing")
					.build();

			BeerUpsertDto updatedBeerDto = BeerUpsertDto.builder()
					.name("Updated Beer")
					.abv(7.0f)
					.style("Pilsner")
					.description("An updated beer description")
					.build();

			webTestClient = webTestClient.mutateWith(mockUser(HaufeUserDetails.builder()
					.id(1L)
					.manufacturerId(1L)
					.build()
					.roles("MANUFACTURER")));

			StepVerifier.create(createBeer(request)
							.flatMap(dto -> {
								Long beerId = dto.id();
								// Now update the beer
								return webTestClient
										.put().uri("/api/beer/" + beerId)
										.contentType(MediaType.APPLICATION_JSON)
										.bodyValue(updatedBeerDto)
										.exchange()
										.expectStatus().isOk()
										.returnResult(BeerUpsertResponseDto.class)
										.getResponseBody()
										.next();
							})
					)
					.assertNext(dto -> {
						assertThat(dto.id()).isNotNull();
						assertThat(dto.name()).isEqualTo(updatedBeerDto.name());

						beerRepository.findById(dto.id()).blockOptional().ifPresentOrElse(beer -> {
							assertThat(beer.getName()).isEqualTo(updatedBeerDto.name());
							assertThat(beer.getAbv()).isEqualTo(updatedBeerDto.abv());
							assertThat(beer.getStyle()).isEqualTo(updatedBeerDto.style());
							assertThat(beer.getDescription()).isEqualTo(updatedBeerDto.description());

							// Clean up created beer
							beerRepository.delete(beer).block();
						}, () -> Assertions.fail("Beer not found in the database after update"));
					})
					.verifyComplete();
		}

		@Test
		@WithMockUser(roles = "ADMIN")
		void readBeer_shouldReturnValidBeer() {
			BeerUpsertDto request = BeerUpsertDto.builder()
					.name("Read Test Beer")
					.abv(4.5f)
					.style("Lager")
					.description("A test beer for reading")
					.build();

			StepVerifier.create(createBeer(request)
							.flatMap(dto -> webTestClient
									.get().uri("/api/beer/" + dto.id())
									.exchange()
									.expectStatus().isOk()
									.returnResult(BeerReadResponseDto.class)
									.getResponseBody()
									.next()
									.map(response -> Tuples.of(dto.id(), response)))
					)
					.assertNext(tuple -> {
						Long id = tuple.getT1();
						BeerReadResponseDto dto = tuple.getT2();

						assertThat(dto.name()).isEqualTo(request.name());
						assertThat(dto.abv()).isEqualTo(request.abv());
						assertThat(dto.style()).isEqualTo(request.style());
						assertThat(dto.description()).isEqualTo(request.description());

						// Clean up created beer
						beerRepository.deleteById(id).block();
					})
					.verifyComplete();
		}

		@Test
		void listBeers_shouldReturnList() {
			webTestClient
					.get().uri(uriBuilder -> uriBuilder.path("/api/beer")
							.queryParam("page", "0")
							.queryParam("size", "5")
							.queryParam("sort", "name")
							.build())
					.exchange()
					.expectStatus().isOk()
					.expectBody(BODY_TYPE)
					.consumeWith(responseList -> {
						RestResponsePage<BeerReadResponseDto> page = responseList.getResponseBody();
						assertThat(page).isNotNull();
						List<BeerReadResponseDto> content = page.getContent();
						assertThat(content)
								.isNotNull()
								.hasSize(5);
						content.forEach(beer -> {
							assertThat(beer.name()).isNotBlank();
							assertThat(beer.abv()).isNotNull();
							assertThat(beer.style()).isNotBlank();
							assertThat(beer.description()).isNotBlank();
						});
					});
		}

		@Test
		void deleteBeer_shouldDeleteBeer() {
			BeerUpsertDto request = BeerUpsertDto.builder()
					.name("Read Test Beer")
					.abv(4.5f)
					.style("Lager")
					.description("A test beer for reading")
					.build();

			webTestClient = webTestClient.mutateWith(mockUser(HaufeUserDetails.builder()
					.id(1L)
					.manufacturerId(1L)
					.build()
					.roles("MANUFACTURER")));

			StepVerifier.create(createBeer(request)
							.flatMap(response -> {
								Long beerId = response.id();
								return webTestClient
										.delete().uri("/api/beer/" + beerId)
										.exchange()
										.expectStatus().isNoContent()
										.returnResult(Void.class)
										.getResponseBody()
										.then(Mono.just(response));
							})
					)
					.assertNext(dto ->
							assertThat(beerRepository.existsById(dto.id()).block()).isFalse())
					.verifyComplete();
		}

		private Mono<BeerUpsertResponseDto> createBeer(BeerUpsertDto request) {
			return webTestClient
					.post().uri("/api/beer/1")    // Assuming manufacturer ID 1 exists
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(request)
					.exchange()
					.expectStatus().isCreated()
					.returnResult(BeerUpsertResponseDto.class)
					.getResponseBody()
					.next();
		}
	}
}
