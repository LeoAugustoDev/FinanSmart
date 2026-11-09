package com.finansmart_server;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategoryApiIntegrationTests {

	private static final String CATEGORIES = "/api/v1/categories";

	@Autowired
	private MockMvc mvc;

	private final RequestPostProcessor userA = user();

	private final RequestPostProcessor userB = user();

	@Test
	void defaultCategoriesAreVisibleToEveryUser() throws Exception {
		perform(get(CATEGORIES), this.userA).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name", hasItem("Alimentação")))
			.andExpect(jsonPath("$[*].name", hasItem("Salário")))
			.andExpect(jsonPath("$[?(@.name == 'Alimentação')].system", hasItem(true)));
		perform(get(CATEGORIES), this.userB).andExpect(jsonPath("$[*].name", hasItem("Alimentação")));
	}

	@Test
	void filtersByType() throws Exception {
		createCategory(this.userA, "Pets", "EXPENSE");

		perform(get(CATEGORIES).param("type", "INCOME"), this.userA).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].type", everyItem(is("INCOME"))))
			.andExpect(jsonPath("$[*].name", not(hasItem("Pets"))));
		perform(get(CATEGORIES).param("type", "EXPENSE"), this.userA)
			.andExpect(jsonPath("$[*].name", hasItem("Pets")));
	}

	@Test
	void fullLifecycle() throws Exception {
		String id = createCategory(this.userA, "Pets", "EXPENSE");

		perform(get(CATEGORIES + "/" + id), this.userA).andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Pets"))
			.andExpect(jsonPath("$.system").value(false));

		perform(json(put(CATEGORIES + "/" + id), "{\"name\":\"Animais\"}"), this.userA).andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Animais"));

		perform(delete(CATEGORIES + "/" + id), this.userA).andExpect(status().isNoContent());
		perform(get(CATEGORIES + "/" + id), this.userA).andExpect(status().isNotFound());
	}

	@Test
	void createReturnsLocationAndStripsTheName() throws Exception {
		perform(json(post(CATEGORIES), body("  Viagens  ", "EXPENSE")), this.userA).andExpect(status().isCreated())
			.andExpect(header().string("Location", containsString(CATEGORIES + "/")))
			.andExpect(jsonPath("$.name").value("Viagens"));
	}

	@Test
	void updateDoesNotChangeTheType() throws Exception {
		String id = createCategory(this.userA, "Pets", "EXPENSE");

		perform(json(put(CATEGORIES + "/" + id), "{\"name\":\"Pets\",\"type\":\"INCOME\"}"), this.userA)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.type").value("EXPENSE"));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "{\"name\":\"   \",\"type\":\"EXPENSE\"}|$.errors.name",
			"{\"type\":\"EXPENSE\"}|$.errors.name", "{\"name\":\"Pets\"}|$.errors.type" })
	void invalidFieldsReturn400WithTheField(String payload, String field) throws Exception {
		perform(json(post(CATEGORIES), payload), this.userA).andExpect(status().isBadRequest())
			.andExpect(jsonPath(field).exists());
	}

	@Test
	void nameLongerThan60CharactersIsRejected() throws Exception {
		perform(json(post(CATEGORIES), body("x".repeat(61), "EXPENSE")), this.userA)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void unknownTypeIsRejected() throws Exception {
		perform(json(post(CATEGORIES), body("Pets", "OTHER")), this.userA).andExpect(status().isBadRequest());
		perform(get(CATEGORIES).param("type", "OTHER"), this.userA).andExpect(status().isBadRequest());
	}

	@Test
	void duplicateNameIgnoresCaseWithinTheSameType() throws Exception {
		createCategory(this.userA, "Pets", "EXPENSE");

		perform(json(post(CATEGORIES), body("PETS", "EXPENSE")), this.userA).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void sameNameIsAllowedInTheOtherType() throws Exception {
		createCategory(this.userA, "Pets", "EXPENSE");
		createCategory(this.userA, "Pets", "INCOME");
	}

	@Test
	void nameOfDefaultCategoryCannotBeReused() throws Exception {
		perform(json(post(CATEGORIES), body("alimentação", "EXPENSE")), this.userA).andExpect(status().isConflict());
	}

	@Test
	void renamingToAnExistingNameIsConflict() throws Exception {
		createCategory(this.userA, "Pets", "EXPENSE");
		String other = createCategory(this.userA, "Viagens", "EXPENSE");

		perform(json(put(CATEGORIES + "/" + other), "{\"name\":\"pets\"}"), this.userA)
			.andExpect(status().isConflict());
	}

	@Test
	void differentUsersCanUseTheSameName() throws Exception {
		createCategory(this.userA, "Pets", "EXPENSE");
		createCategory(this.userB, "Pets", "EXPENSE");
	}

	@Test
	void defaultCategoriesCannotBeChanged() throws Exception {
		String response = perform(get(CATEGORIES).param("type", "EXPENSE"), this.userA).andReturn()
			.getResponse()
			.getContentAsString();
		List<String> ids = JsonPath.read(response, "$[?(@.name == 'Lazer')].id");
		String path = CATEGORIES + "/" + ids.get(0);

		perform(json(put(path), "{\"name\":\"Diversão\"}"), this.userA).andExpect(status().isForbidden());
		perform(delete(path), this.userA).andExpect(status().isForbidden());
		perform(get(path), this.userA).andExpect(jsonPath("$.name").value("Lazer"));
	}

	@Test
	void categoriesOfAnotherUserRespondNotFound() throws Exception {
		String id = createCategory(this.userA, "Pets", "EXPENSE");
		String path = CATEGORIES + "/" + id;

		perform(get(CATEGORIES), this.userB).andExpect(jsonPath("$[*].name", not(hasItem("Pets"))));
		perform(get(path), this.userB).andExpect(status().isNotFound());
		perform(json(put(path), "{\"name\":\"Invasão\"}"), this.userB).andExpect(status().isNotFound());
		perform(delete(path), this.userB).andExpect(status().isNotFound());

		perform(get(path), this.userA).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Pets"));
	}

	@Test
	void requestsWithoutTokenAreRejected() throws Exception {
		this.mvc.perform(get(CATEGORIES)).andExpect(status().isUnauthorized());
		this.mvc.perform(json(post(CATEGORIES), body("Pets", "EXPENSE"))).andExpect(status().isUnauthorized());
	}

	@Test
	void endpointsAreDocumented() throws Exception {
		this.mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/categories'].post").exists())
			.andExpect(jsonPath("$.paths['/api/v1/categories/{id}'].delete").exists());
	}

	private String createCategory(RequestPostProcessor user, String name, String type) throws Exception {
		String response = perform(json(post(CATEGORIES), body(name, type)), user).andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private ResultActions perform(MockHttpServletRequestBuilder request, RequestPostProcessor user) throws Exception {
		return this.mvc.perform(request.with(user));
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String payload) {
		return request.contentType(MediaType.APPLICATION_JSON).content(payload);
	}

	private static String body(String name, String type) {
		return "{\"name\":\"%s\",\"type\":\"%s\"}".formatted(name, type);
	}

	private static RequestPostProcessor user() {
		return jwt().jwt((token) -> token.subject(UUID.randomUUID().toString()));
	}

}
