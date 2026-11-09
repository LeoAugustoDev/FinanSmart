package com.finansmart_server;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class LeakApiIntegrationTests {

	private static final String LEAKS = "/api/v1/leaks";

	private static final String ACCOUNTS = "/api/v1/accounts";

	private static final String CATEGORIES = "/api/v1/categories";

	private static final String TRANSACTIONS = "/api/v1/transactions";

	@Autowired
	private MockMvc mvc;

	private final RequestPostProcessor user = user();

	private String account;

	private String subscriptions;

	private String fees;

	private String leisure;

	private String food;

	private String salary;

	@BeforeEach
	void setUp() throws Exception {
		this.account = createAccount("Nubank");
		this.subscriptions = defaultCategory("Assinaturas");
		this.fees = defaultCategory("Tarifas e juros");
		this.leisure = defaultCategory("Lazer");
		this.food = defaultCategory("Alimentação");
		this.salary = defaultCategory("Salário");
	}

	@Test
	void sumsTheThreeDetectorsOfTheMonth() throws Exception {
		createTransaction(this.subscriptions, "39.90", "2026-11-05", "Netflix");
		createTransaction(this.fees, "12.00", "2026-11-02", "Tarifa de conta");
		createTransaction(this.fees, "30.50", "2026-11-15", "Juros do cartão");
		createTransaction(this.leisure, "50.00", "2026-09-10", "Clube do Vinho");
		createTransaction(this.leisure, "50.00", "2026-10-10", "Clube do Vinho");
		createTransaction(this.leisure, "50.00", "2026-11-10", "clube do vinho");
		createTransaction(this.food, "300.00", "2026-08-10", null);
		createTransaction(this.food, "200.00", "2026-09-10", null);
		createTransaction(this.food, "100.00", "2026-10-10", null);
		createTransaction(this.food, "350.00", "2026-11-10", null);
		// Receita com descrição repetida não é vazamento.
		createTransaction(this.salary, "5000.00", "2026-09-01", "Empresa");
		createTransaction(this.salary, "5000.00", "2026-10-01", "Empresa");
		createTransaction(this.salary, "5000.00", "2026-11-01", "Empresa");

		perform(get(LEAKS).param("month", "2026-11")).andExpect(status().isOk())
			.andExpect(jsonPath("$.month").value("2026-11"))
			.andExpect(jsonPath("$.subscriptions.total").value(89.90))
			.andExpect(jsonPath("$.subscriptions.items[*].description", contains("clube do vinho", "Netflix")))
			.andExpect(jsonPath("$.subscriptions.items[0].categoryName").value("Lazer"))
			.andExpect(jsonPath("$.subscriptions.items[0].recurring").value(true))
			.andExpect(jsonPath("$.subscriptions.items[1].recurring").value(false))
			.andExpect(jsonPath("$.fees.total").value(42.50))
			.andExpect(jsonPath("$.fees.items[*].description", contains("Juros do cartão", "Tarifa de conta")))
			.andExpect(jsonPath("$.aboveAverage.total").value(150.00))
			.andExpect(jsonPath("$.aboveAverage.items", hasSize(1)))
			.andExpect(jsonPath("$.aboveAverage.items[0].categoryId").value(this.food))
			.andExpect(jsonPath("$.aboveAverage.items[0].categoryName").value("Alimentação"))
			.andExpect(jsonPath("$.aboveAverage.items[0].spent").value(350.00))
			.andExpect(jsonPath("$.aboveAverage.items[0].average").value(200.00))
			.andExpect(jsonPath("$.aboveAverage.items[0].excess").value(150.00))
			.andExpect(jsonPath("$.total").value(282.40));
	}

	@Test
	void onlyTheRequestedMonthLeaks() throws Exception {
		createTransaction(this.fees, "12.00", "2026-10-31", "Tarifa");
		createTransaction(this.fees, "15.00", "2026-11-01", "Tarifa");
		createTransaction(this.fees, "18.00", "2026-12-01", "Tarifa");

		perform(get(LEAKS).param("month", "2026-11")).andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(15.00));
	}

	@Test
	void historyCrossesTheYearBoundary() throws Exception {
		createTransaction(this.leisure, "30.00", "2025-11-10", "Revista");
		createTransaction(this.leisure, "30.00", "2025-12-10", "Revista");
		createTransaction(this.leisure, "30.00", "2026-01-10", "Revista");

		perform(get(LEAKS).param("month", "2026-01")).andExpect(status().isOk())
			.andExpect(jsonPath("$.subscriptions.items[0].recurring").value(true))
			.andExpect(jsonPath("$.total").value(30.00));
	}

	@Test
	void monthWithoutLeaksReturnsZeros() throws Exception {
		createTransaction(this.food, "100.00", "2026-11-10", "Mercado");

		perform(get(LEAKS).param("month", "2026-11")).andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(0))
			.andExpect(jsonPath("$.subscriptions.total").value(0))
			.andExpect(jsonPath("$.subscriptions.items", hasSize(0)))
			.andExpect(jsonPath("$.fees.items", hasSize(0)))
			.andExpect(jsonPath("$.aboveAverage.items", hasSize(0)));
	}

	@Test
	void monthDefaultsToTheCurrentOne() throws Exception {
		perform(get(LEAKS)).andExpect(status().isOk()).andExpect(jsonPath("$.month").isNotEmpty());
	}

	@Test
	void leaksAreIsolatedBetweenUsers() throws Exception {
		createTransaction(this.subscriptions, "39.90", "2026-11-05", "Netflix");
		createTransaction(this.fees, "12.00", "2026-11-02", "Tarifa");

		this.mvc.perform(get(LEAKS).param("month", "2026-11").with(user()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(0))
			.andExpect(jsonPath("$.subscriptions.items", hasSize(0)))
			.andExpect(jsonPath("$.fees.items", hasSize(0)));
	}

	@Test
	void invalidMonthIsRejected() throws Exception {
		perform(get(LEAKS).param("month", "2026-13")).andExpect(status().isBadRequest());
	}

	@Test
	void requestsWithoutTokenAreRejected() throws Exception {
		this.mvc.perform(get(LEAKS)).andExpect(status().isUnauthorized());
	}

	@Test
	void endpointIsDocumented() throws Exception {
		this.mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/leaks'].get").exists());
	}

	private String createAccount(String name) throws Exception {
		String payload = "{\"name\":\"%s\",\"type\":\"CHECKING\",\"initialBalance\":0}".formatted(name);
		return idOf(perform(json(post(ACCOUNTS), payload)));
	}

	private String createTransaction(String category, String amount, String date, String description)
			throws Exception {
		String payload = "{\"accountId\":\"%s\",\"categoryId\":\"%s\",\"amount\":%s,\"occurredOn\":\"%s\",\"description\":%s}"
			.formatted(this.account, category, amount, date,
					(description != null) ? "\"" + description + "\"" : "null");
		return idOf(perform(json(post(TRANSACTIONS), payload)));
	}

	private String defaultCategory(String name) throws Exception {
		String response = perform(get(CATEGORIES)).andReturn().getResponse().getContentAsString();
		List<String> ids = JsonPath.read(response, "$[?(@.name == '%s')].id".formatted(name));
		return ids.get(0);
	}

	private static String idOf(ResultActions result) throws Exception {
		String response = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
		return this.mvc.perform(request.with(this.user));
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String payload) {
		return request.contentType(MediaType.APPLICATION_JSON).content(payload);
	}

	private static RequestPostProcessor user() {
		return jwt().jwt((token) -> token.subject(UUID.randomUUID().toString()));
	}

}
