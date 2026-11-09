package com.finansmart_server;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class DashboardApiIntegrationTests {

	private static final String DASHBOARD = "/api/v1/dashboard";

	private static final String ACCOUNTS = "/api/v1/accounts";

	private static final String CATEGORIES = "/api/v1/categories";

	private static final String TRANSACTIONS = "/api/v1/transactions";

	@Autowired
	private MockMvc mvc;

	private final RequestPostProcessor user = user();

	private String expense;

	private String income;

	@BeforeEach
	void setUp() throws Exception {
		this.expense = defaultCategory("Lazer");
		this.income = defaultCategory("Salário");
	}

	@Test
	void accountBalanceAddsIncomesAndSubtractsExpenses() throws Exception {
		String account = createAccount("Nubank", "1000.00");
		createTransaction(account, this.income, "500.25", "2026-11-01");
		createTransaction(account, this.expense, "200.10", "2026-11-02");

		perform(get(ACCOUNTS + "/" + account)).andExpect(status().isOk())
			.andExpect(jsonPath("$.initialBalance").value(1000.00))
			.andExpect(jsonPath("$.currentBalance").value(1300.15));
		perform(get(ACCOUNTS)).andExpect(jsonPath("$[0].currentBalance").value(1300.15));
	}

	@Test
	void newAccountBalanceIsTheInitialBalance() throws Exception {
		perform(json(post(ACCOUNTS), accountBody("Nova", "250.00"))).andExpect(status().isCreated())
			.andExpect(jsonPath("$.currentBalance").value(250.00));
	}

	@Test
	void updateResponseAndBalanceFollowTheInitialBalanceAndTransactions() throws Exception {
		String account = createAccount("Nubank", "100.00");
		String transaction = createTransaction(account, this.expense, "30.00", "2026-11-01");

		perform(json(put(ACCOUNTS + "/" + account), "{\"name\":\"Nubank\",\"type\":\"CHECKING\",\"initialBalance\":200}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.currentBalance").value(170.00));

		perform(delete(TRANSACTIONS + "/" + transaction)).andExpect(status().isNoContent());
		perform(get(ACCOUNTS + "/" + account)).andExpect(jsonPath("$.currentBalance").value(200.00));
	}

	@Test
	void balanceCanBeNegative() throws Exception {
		String account = createAccount("Nubank", "10.00");
		createTransaction(account, this.expense, "25.50", "2026-11-01");

		perform(get(DASHBOARD).param("month", "2026-11")).andExpect(jsonPath("$.totalBalance").value(-15.50));
	}

	@Test
	void totalSumsActiveAccountsOnly() throws Exception {
		createAccount("Nubank", "100.00");
		String archived = createAccount("Antiga", "50.00");
		perform(patch(ACCOUNTS + "/" + archived + "/archive")).andExpect(status().isNoContent());
		createAccount("Carteira", "25.00");

		perform(get(DASHBOARD).param("month", "2026-11")).andExpect(status().isOk())
			.andExpect(jsonPath("$.totalBalance").value(125.00))
			.andExpect(jsonPath("$.accounts[*].name", contains("Carteira", "Nubank")));
	}

	@Test
	void monthTotalsIncludeOnlyTheRequestedMonth() throws Exception {
		String account = createAccount("Nubank", "0");
		createTransaction(account, this.income, "1000.00", "2026-10-31");
		createTransaction(account, this.income, "300.00", "2026-11-01");
		createTransaction(account, this.income, "200.00", "2026-11-30");
		createTransaction(account, this.expense, "120.00", "2026-11-15");
		createTransaction(account, this.expense, "999.00", "2026-12-01");

		perform(get(DASHBOARD).param("month", "2026-11")).andExpect(status().isOk())
			.andExpect(jsonPath("$.month").value("2026-11"))
			.andExpect(jsonPath("$.income").value(500.00))
			.andExpect(jsonPath("$.expense").value(120.00))
			.andExpect(jsonPath("$.result").value(380.00));
	}

	@Test
	void recentTransactionsShowTheLatestFive() throws Exception {
		String account = createAccount("Nubank", "0");
		for (int day = 1; day <= 6; day++) {
			createTransaction(account, this.expense, "1.00", "2026-11-0" + day);
		}

		perform(get(DASHBOARD).param("month", "2026-11"))
			.andExpect(jsonPath("$.recentTransactions", hasSize(5)))
			.andExpect(jsonPath("$.recentTransactions[0].occurredOn").value("2026-11-06"))
			.andExpect(jsonPath("$.recentTransactions[4].occurredOn").value("2026-11-02"));
	}

	@Test
	void newUserStartsEmpty() throws Exception {
		perform(get(DASHBOARD).param("month", "2026-11")).andExpect(status().isOk())
			.andExpect(jsonPath("$.totalBalance").value(0))
			.andExpect(jsonPath("$.income").value(0))
			.andExpect(jsonPath("$.expense").value(0))
			.andExpect(jsonPath("$.result").value(0))
			.andExpect(jsonPath("$.accounts", hasSize(0)))
			.andExpect(jsonPath("$.recentTransactions", hasSize(0)));
	}

	@Test
	void dashboardIsIsolatedBetweenUsers() throws Exception {
		String account = createAccount("Nubank", "100.00");
		createTransaction(account, this.income, "50.00", "2026-11-01");

		this.mvc.perform(get(DASHBOARD).param("month", "2026-11").with(user()))
			.andExpect(jsonPath("$.totalBalance").value(0))
			.andExpect(jsonPath("$.income").value(0))
			.andExpect(jsonPath("$.accounts", hasSize(0)))
			.andExpect(jsonPath("$.recentTransactions", hasSize(0)));
	}

	@Test
	void invalidMonthIsRejected() throws Exception {
		perform(get(DASHBOARD).param("month", "2026-13")).andExpect(status().isBadRequest());
	}

	@Test
	void requestsWithoutTokenAreRejected() throws Exception {
		this.mvc.perform(get(DASHBOARD)).andExpect(status().isUnauthorized());
	}

	@Test
	void endpointIsDocumented() throws Exception {
		this.mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/dashboard'].get").exists());
	}

	private String createAccount(String name, String initialBalance) throws Exception {
		return idOf(perform(json(post(ACCOUNTS), accountBody(name, initialBalance))));
	}

	private String createTransaction(String account, String category, String amount, String date) throws Exception {
		String payload = "{\"accountId\":\"%s\",\"categoryId\":\"%s\",\"amount\":%s,\"occurredOn\":\"%s\"}"
			.formatted(account, category, amount, date);
		return idOf(perform(json(post(TRANSACTIONS), payload)));
	}

	private String defaultCategory(String name) throws Exception {
		String response = perform(get(CATEGORIES)).andReturn().getResponse().getContentAsString();
		List<String> ids = JsonPath.read(response, "$[?(@.name == '%s')].id".formatted(name));
		return ids.get(0);
	}

	private static String accountBody(String name, String initialBalance) {
		return "{\"name\":\"%s\",\"type\":\"CHECKING\",\"initialBalance\":%s}".formatted(name, initialBalance);
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
