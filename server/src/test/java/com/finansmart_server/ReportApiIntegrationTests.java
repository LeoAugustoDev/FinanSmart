package com.finansmart_server;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class ReportApiIntegrationTests {

	private static final String REPORT = "/api/v1/reports/monthly";

	private static final String ACCOUNTS = "/api/v1/accounts";

	private static final String CATEGORIES = "/api/v1/categories";

	private static final String TRANSACTIONS = "/api/v1/transactions";

	@Autowired
	private MockMvc mvc;

	private final RequestPostProcessor user = user();

	private String account;

	private String leisure;

	private String food;

	private String transport;

	private String salary;

	@BeforeEach
	void setUp() throws Exception {
		this.account = createAccount("Nubank");
		this.leisure = defaultCategory("Lazer");
		this.food = defaultCategory("Alimentação");
		this.transport = defaultCategory("Transporte");
		this.salary = defaultCategory("Salário");
	}

	@Test
	void comparesIncomeExpenseAndResultWithThePreviousMonth() throws Exception {
		createTransaction(this.salary, "2500.00", "2026-10-05");
		createTransaction(this.food, "1500.00", "2026-10-31");
		createTransaction(this.salary, "3000.00", "2026-11-01");
		createTransaction(this.food, "900.00", "2026-11-10");
		createTransaction(this.leisure, "300.00", "2026-11-30");
		createTransaction(this.leisure, "999.00", "2026-12-01");
		createTransaction(this.salary, "999.00", "2026-09-30");

		perform(get(REPORT).param("month", "2026-11")).andExpect(status().isOk())
			.andExpect(jsonPath("$.month").value("2026-11"))
			.andExpect(jsonPath("$.previousMonth").value("2026-10"))
			.andExpect(jsonPath("$.income.current").value(3000.00))
			.andExpect(jsonPath("$.income.previous").value(2500.00))
			.andExpect(jsonPath("$.income.change").value(500.00))
			.andExpect(jsonPath("$.income.changePercent").value(20.0))
			.andExpect(jsonPath("$.expense.current").value(1200.00))
			.andExpect(jsonPath("$.expense.previous").value(1500.00))
			.andExpect(jsonPath("$.expense.change").value(-300.00))
			.andExpect(jsonPath("$.expense.changePercent").value(-20.0))
			.andExpect(jsonPath("$.result.current").value(1800.00))
			.andExpect(jsonPath("$.result.previous").value(1000.00))
			.andExpect(jsonPath("$.result.changePercent").value(80.0));
	}

	@Test
	void expensesByCategoryShowShareAndComparison() throws Exception {
		createTransaction(this.food, "200.00", "2026-10-10");
		createTransaction(this.transport, "80.00", "2026-10-11");
		createTransaction(this.food, "250.00", "2026-11-05");
		createTransaction(this.food, "50.00", "2026-11-20");
		createTransaction(this.leisure, "100.00", "2026-11-06");
		createTransaction(this.salary, "5000.00", "2026-11-01");

		perform(get(REPORT).param("month", "2026-11")).andExpect(status().isOk())
			.andExpect(jsonPath("$.expensesByCategory[*].categoryName", contains("Alimentação", "Lazer", "Transporte")))
			.andExpect(jsonPath("$.expensesByCategory[0].categoryId").value(this.food))
			.andExpect(jsonPath("$.expensesByCategory[0].total").value(300.00))
			.andExpect(jsonPath("$.expensesByCategory[0].share").value(75.0))
			.andExpect(jsonPath("$.expensesByCategory[0].previousTotal").value(200.00))
			.andExpect(jsonPath("$.expensesByCategory[0].change").value(100.00))
			.andExpect(jsonPath("$.expensesByCategory[0].changePercent").value(50.0))
			.andExpect(jsonPath("$.expensesByCategory[1].share").value(25.0))
			.andExpect(jsonPath("$.expensesByCategory[1].previousTotal").value(0))
			.andExpect(jsonPath("$.expensesByCategory[1].changePercent").value(nullValue()))
			.andExpect(jsonPath("$.expensesByCategory[2].total").value(0))
			.andExpect(jsonPath("$.expensesByCategory[2].share").value(0))
			.andExpect(jsonPath("$.expensesByCategory[2].change").value(-80.00))
			.andExpect(jsonPath("$.expensesByCategory[2].changePercent").value(-100.0));
	}

	@Test
	void januaryIsComparedWithDecemberOfThePreviousYear() throws Exception {
		createTransaction(this.food, "100.00", "2025-12-31");
		createTransaction(this.food, "150.00", "2026-01-01");

		perform(get(REPORT).param("month", "2026-01")).andExpect(status().isOk())
			.andExpect(jsonPath("$.previousMonth").value("2025-12"))
			.andExpect(jsonPath("$.expense.current").value(150.00))
			.andExpect(jsonPath("$.expense.previous").value(100.00))
			.andExpect(jsonPath("$.expense.changePercent").value(50.0));
	}

	@Test
	void transactionsOfArchivedAccountsStillCount() throws Exception {
		createTransaction(this.food, "70.00", "2026-11-05");
		perform(patch(ACCOUNTS + "/" + this.account + "/archive")).andExpect(status().isNoContent());

		perform(get(REPORT).param("month", "2026-11")).andExpect(jsonPath("$.expense.current").value(70.00));
	}

	@Test
	void monthWithoutTransactionsReturnsZeros() throws Exception {
		perform(get(REPORT).param("month", "2026-11")).andExpect(status().isOk())
			.andExpect(jsonPath("$.income.current").value(0))
			.andExpect(jsonPath("$.income.changePercent").value(nullValue()))
			.andExpect(jsonPath("$.expense.current").value(0))
			.andExpect(jsonPath("$.result.current").value(0))
			.andExpect(jsonPath("$.expensesByCategory", hasSize(0)));
	}

	@Test
	void monthDefaultsToTheCurrentOne() throws Exception {
		perform(get(REPORT)).andExpect(status().isOk()).andExpect(jsonPath("$.month").isNotEmpty());
	}

	@Test
	void reportIsIsolatedBetweenUsers() throws Exception {
		createTransaction(this.salary, "1000.00", "2026-11-01");
		createTransaction(this.food, "100.00", "2026-11-02");

		this.mvc.perform(get(REPORT).param("month", "2026-11").with(user()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.income.current").value(0))
			.andExpect(jsonPath("$.expense.current").value(0))
			.andExpect(jsonPath("$.expensesByCategory", hasSize(0)));
	}

	@Test
	void invalidMonthIsRejected() throws Exception {
		perform(get(REPORT).param("month", "2026-13")).andExpect(status().isBadRequest());
	}

	@Test
	void requestsWithoutTokenAreRejected() throws Exception {
		this.mvc.perform(get(REPORT)).andExpect(status().isUnauthorized());
	}

	@Test
	void endpointIsDocumented() throws Exception {
		this.mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/reports/monthly'].get").exists());
	}

	private String createAccount(String name) throws Exception {
		String payload = "{\"name\":\"%s\",\"type\":\"CHECKING\",\"initialBalance\":0}".formatted(name);
		return idOf(perform(json(post(ACCOUNTS), payload)));
	}

	private String createTransaction(String category, String amount, String date) throws Exception {
		String payload = "{\"accountId\":\"%s\",\"categoryId\":\"%s\",\"amount\":%s,\"occurredOn\":\"%s\"}"
			.formatted(this.account, category, amount, date);
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
