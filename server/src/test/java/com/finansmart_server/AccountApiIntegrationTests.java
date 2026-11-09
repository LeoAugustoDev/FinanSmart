package com.finansmart_server;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AccountApiIntegrationTests {

	private static final String ACCOUNTS = "/api/v1/accounts";

	@Autowired
	private MockMvc mvc;

	private final RequestPostProcessor userA = user();

	private final RequestPostProcessor userB = user();

	@Test
	void fullLifecycle() throws Exception {
		String id = createAccount(this.userA, "Nubank");

		perform(get(ACCOUNTS), this.userA).andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].name").value("Nubank"))
			.andExpect(jsonPath("$[0].currency").value("BRL"))
			.andExpect(jsonPath("$[0].archived").value(false));

		perform(json(put(ACCOUNTS + "/" + id), body("Nu Conta", "SAVINGS", "-10.50")), this.userA)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Nu Conta"))
			.andExpect(jsonPath("$.type").value("SAVINGS"))
			.andExpect(jsonPath("$.initialBalance").value(-10.50));

		perform(patch(ACCOUNTS + "/" + id + "/archive"), this.userA).andExpect(status().isNoContent());
		perform(get(ACCOUNTS), this.userA).andExpect(jsonPath("$", hasSize(0)));
		perform(get(ACCOUNTS).param("includeArchived", "true"), this.userA)
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].archived").value(true));

		perform(patch(ACCOUNTS + "/" + id + "/unarchive"), this.userA).andExpect(status().isNoContent());
		perform(get(ACCOUNTS + "/" + id), this.userA).andExpect(jsonPath("$.archived").value(false));

		perform(delete(ACCOUNTS + "/" + id), this.userA).andExpect(status().isNoContent());
		perform(get(ACCOUNTS + "/" + id), this.userA).andExpect(status().isNotFound());
	}

	@Test
	void createReturnsLocationAndStripsTheName() throws Exception {
		perform(json(post(ACCOUNTS), body("  Carteira  ", "WALLET", "0")), this.userA)
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", containsString(ACCOUNTS + "/")))
			.andExpect(jsonPath("$.name").value("Carteira"));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"{\"name\":\"   \",\"type\":\"CHECKING\",\"initialBalance\":1}|$.errors.name",
			"{\"type\":\"CHECKING\",\"initialBalance\":1}|$.errors.name",
			"{\"name\":\"Conta\",\"initialBalance\":1}|$.errors.type",
			"{\"name\":\"Conta\",\"type\":\"CHECKING\"}|$.errors.initialBalance",
			"{\"name\":\"Conta\",\"type\":\"CHECKING\",\"initialBalance\":1.999}|$.errors.initialBalance" })
	void invalidFieldsReturn400WithTheField(String payload, String field) throws Exception {
		perform(json(post(ACCOUNTS), payload), this.userA).andExpect(status().isBadRequest())
			.andExpect(jsonPath(field).exists());
	}

	@Test
	void nameLongerThan60CharactersIsRejected() throws Exception {
		perform(json(post(ACCOUNTS), body("x".repeat(61), "CHECKING", "1")), this.userA)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void unknownTypeIsRejected() throws Exception {
		perform(json(post(ACCOUNTS), body("Conta", "CREDIT_CARD", "1")), this.userA).andExpect(status().isBadRequest());
	}

	@Test
	void duplicateNameIgnoresCase() throws Exception {
		createAccount(this.userA, "Nubank");

		perform(json(post(ACCOUNTS), body("NUBANK", "SAVINGS", "1")), this.userA).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void updateToAnExistingNameIsConflict() throws Exception {
		createAccount(this.userA, "Nubank");
		String other = createAccount(this.userA, "Itaú");

		perform(json(put(ACCOUNTS + "/" + other), body("nubank", "CHECKING", "1")), this.userA)
			.andExpect(status().isConflict());
	}

	@Test
	void differentUsersCanUseTheSameName() throws Exception {
		createAccount(this.userA, "Nubank");
		createAccount(this.userB, "Nubank");
	}

	@Test
	void archivedNameCanBeReusedAndBlocksUnarchiving() throws Exception {
		String archived = createAccount(this.userA, "Nubank");
		perform(patch(ACCOUNTS + "/" + archived + "/archive"), this.userA).andExpect(status().isNoContent());

		createAccount(this.userA, "Nubank");

		perform(patch(ACCOUNTS + "/" + archived + "/unarchive"), this.userA).andExpect(status().isConflict());
	}

	@Test
	void accountsOfAnotherUserRespondNotFound() throws Exception {
		String id = createAccount(this.userA, "Nubank");
		String path = ACCOUNTS + "/" + id;

		perform(get(ACCOUNTS), this.userB).andExpect(jsonPath("$", hasSize(0)));
		perform(get(path), this.userB).andExpect(status().isNotFound());
		perform(json(put(path), body("Invasão", "CHECKING", "1")), this.userB).andExpect(status().isNotFound());
		perform(patch(path + "/archive"), this.userB).andExpect(status().isNotFound());
		perform(patch(path + "/unarchive"), this.userB).andExpect(status().isNotFound());
		perform(delete(path), this.userB).andExpect(status().isNotFound());

		perform(get(path), this.userA).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nubank"));
	}

	@Test
	void requestsWithoutTokenAreRejected() throws Exception {
		this.mvc.perform(get(ACCOUNTS)).andExpect(status().isUnauthorized());
		this.mvc.perform(json(post(ACCOUNTS), body("Conta", "CHECKING", "1"))).andExpect(status().isUnauthorized());
	}

	@Test
	void endpointsAreDocumented() throws Exception {
		this.mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/accounts'].post").exists())
			.andExpect(jsonPath("$.paths['/api/v1/accounts/{id}/unarchive'].patch").exists());
	}

	private String createAccount(RequestPostProcessor user, String name) throws Exception {
		String response = perform(json(post(ACCOUNTS), body(name, "CHECKING", "1500.00")), user)
			.andExpect(status().isCreated())
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

	private static String body(String name, String type, String initialBalance) {
		return "{\"name\":\"%s\",\"type\":\"%s\",\"initialBalance\":%s}".formatted(name, type, initialBalance);
	}

	private static RequestPostProcessor user() {
		return jwt().jwt((token) -> token.subject(UUID.randomUUID().toString()));
	}

}
