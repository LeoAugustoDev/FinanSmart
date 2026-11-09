package com.finansmart_server;

import static org.hamcrest.Matchers.contains;
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

import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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
class BudgetApiIntegrationTests {

	private static final String BUDGETS = "/api/v1/budgets";

	private static final String ACCOUNTS = "/api/v1/accounts";

	private static final String CATEGORIES = "/api/v1/categories";

	private static final String TRANSACTIONS = "/api/v1/transactions";

	private static final String MONTH = "2026-11";

	@Autowired
	private MockMvc mvc;

	private final RequestPostProcessor userA = user();

	private final RequestPostProcessor userB = user();

	private String account;

	private String leisure;

	private String food;

	private String salary;

	@BeforeEach
	void setUp() throws Exception {
		this.account = createAccount(this.userA, "Nubank");
		this.leisure = defaultCategory("Lazer");
		this.food = defaultCategory("Alimentação");
		this.salary = defaultCategory("Salário");
	}

	@Test
	void fullLifecycle() throws Exception {
		createTransaction(this.account, this.leisure, "50.00", "2026-11-10");
		String id = createBudget(this.userA, this.leisure, MONTH, "200.00");
		String path = BUDGETS + "/" + id;

		perform(get(path), this.userA).andExpect(status().isOk())
			.andExpect(jsonPath("$.categoryName").value("Lazer"))
			.andExpect(jsonPath("$.month").value(MONTH))
			.andExpect(jsonPath("$.limit").value(200.00))
			.andExpect(jsonPath("$.spent").value(50.00))
			.andExpect(jsonPath("$.remaining").value(150.00))
			.andExpect(jsonPath("$.percentUsed").value(25.0))
			.andExpect(jsonPath("$.status").value("OK"));

		perform(json(put(path), "{\"limit\":60}"), this.userA).andExpect(status().isOk())
			.andExpect(jsonPath("$.limit").value(60))
			.andExpect(jsonPath("$.status").value("WARNING"));

		perform(delete(path), this.userA).andExpect(status().isNoContent());
		perform(get(path), this.userA).andExpect(status().isNotFound());
	}

	@Test
	void createReturnsLocation() throws Exception {
		perform(json(post(BUDGETS), body(this.leisure, MONTH, "100")), this.userA).andExpect(status().isCreated())
			.andExpect(header().string("Location", containsString(BUDGETS + "/")))
			.andExpect(jsonPath("$.spent").value(0));
	}

	@Test
	void spentCountsOnlyTheCategoryAndMonthAcrossAccounts() throws Exception {
		String other = createAccount(this.userA, "Itaú");
		createTransaction(this.account, this.leisure, "1.00", "2026-10-31");
		createTransaction(this.account, this.leisure, "10.00", "2026-11-01");
		createTransaction(other, this.leisure, "20.00", "2026-11-30");
		createTransaction(this.account, this.leisure, "4.00", "2026-12-01");
		createTransaction(this.account, this.food, "99.00", "2026-11-15");
		createBudget(this.userA, this.leisure, MONTH, "100");

		perform(get(BUDGETS).param("month", MONTH), this.userA).andExpect(jsonPath("$[0].spent").value(30.00));
	}

	@Test
	void transactionsOfArchivedAccountsStillCount() throws Exception {
		createTransaction(this.account, this.leisure, "40.00", "2026-11-05");
		createBudget(this.userA, this.leisure, MONTH, "100");
		perform(patch(ACCOUNTS + "/" + this.account + "/archive"), this.userA).andExpect(status().isNoContent());

		perform(get(BUDGETS).param("month", MONTH), this.userA).andExpect(jsonPath("$[0].spent").value(40.00));
	}

	@ParameterizedTest
	@CsvSource({ "80.00, OK", "80.01, WARNING", "100.00, WARNING", "100.01, EXCEEDED" })
	void statusFollowsTheThresholds(String spent, String expected) throws Exception {
		createTransaction(this.account, this.leisure, spent, "2026-11-10");
		createBudget(this.userA, this.leisure, MONTH, "100.00");

		perform(get(BUDGETS).param("month", MONTH), this.userA).andExpect(jsonPath("$[0].status").value(expected));
	}

	@Test
	void exceededBudgetHasNegativeRemaining() throws Exception {
		createTransaction(this.account, this.leisure, "150.00", "2026-11-10");
		createBudget(this.userA, this.leisure, MONTH, "100.00");

		perform(get(BUDGETS).param("month", MONTH), this.userA)
			.andExpect(jsonPath("$[0].remaining").value(-50.00))
			.andExpect(jsonPath("$[0].percentUsed").value(150.0));
	}

	@Test
	void listsOnlyTheMonthOrderedByCategory() throws Exception {
		createBudget(this.userA, this.leisure, MONTH, "100");
		createBudget(this.userA, this.food, MONTH, "100");
		createBudget(this.userA, this.food, "2026-12", "100");

		perform(get(BUDGETS).param("month", MONTH), this.userA).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].categoryName", contains("Alimentação", "Lazer")));
	}

	@Test
	void duplicateForTheSameCategoryAndMonthIsConflict() throws Exception {
		createBudget(this.userA, this.leisure, MONTH, "100");

		perform(json(post(BUDGETS), body(this.leisure, MONTH, "200")), this.userA).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors.categoryId").exists());
	}

	@Test
	void sameCategoryIsAllowedInAnotherMonthAndForAnotherUser() throws Exception {
		createBudget(this.userA, this.leisure, MONTH, "100");
		createBudget(this.userA, this.leisure, "2026-12", "100");
		createBudget(this.userB, this.leisure, MONTH, "100");
	}

	@Test
	void incomeCategoryIsRejected() throws Exception {
		perform(json(post(BUDGETS), body(this.salary, MONTH, "100")), this.userA).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.categoryId").exists());
	}

	@Test
	void personalCategoryOfAnotherUserIsRejected() throws Exception {
		String other = createPersonalCategory(this.userB);

		perform(json(post(BUDGETS), body(other, MONTH, "100")), this.userA).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.categoryId").exists());
	}

	@ParameterizedTest
	@CsvSource({ "0", "-1", "1.999" })
	void invalidLimitIsRejected(String limit) throws Exception {
		perform(json(post(BUDGETS), body(this.leisure, MONTH, limit)), this.userA).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.limit").exists());
	}

	@Test
	void requiredFieldsReturn400WithTheFields() throws Exception {
		perform(json(post(BUDGETS), "{}"), this.userA).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.categoryId").exists())
			.andExpect(jsonPath("$.errors.month").exists())
			.andExpect(jsonPath("$.errors.limit").exists());
	}

	@Test
	void invalidOrMissingMonthIsRejected() throws Exception {
		perform(get(BUDGETS), this.userA).andExpect(status().isBadRequest());
		perform(get(BUDGETS).param("month", "2026-13"), this.userA).andExpect(status().isBadRequest());
		perform(json(post(BUDGETS), body(this.leisure, "2026-13", "1")), this.userA).andExpect(status().isBadRequest());
	}

	@Test
	void categoryWithBudgetCannotBeDeleted() throws Exception {
		String personal = createPersonalCategory(this.userA);
		String budget = createBudget(this.userA, personal, MONTH, "100");

		perform(delete(CATEGORIES + "/" + personal), this.userA).andExpect(status().isConflict());

		perform(delete(BUDGETS + "/" + budget), this.userA).andExpect(status().isNoContent());
		perform(delete(CATEGORIES + "/" + personal), this.userA).andExpect(status().isNoContent());
	}

	@Test
	void budgetsOfAnotherUserRespondNotFound() throws Exception {
		String id = createBudget(this.userA, this.leisure, MONTH, "100");
		String path = BUDGETS + "/" + id;

		perform(get(BUDGETS).param("month", MONTH), this.userB).andExpect(jsonPath("$", hasSize(0)));
		perform(get(path), this.userB).andExpect(status().isNotFound());
		perform(json(put(path), "{\"limit\":1}"), this.userB).andExpect(status().isNotFound());
		perform(delete(path), this.userB).andExpect(status().isNotFound());

		perform(get(path), this.userA).andExpect(status().isOk()).andExpect(jsonPath("$.limit").value(100));
	}

	@Test
	void spentIgnoresTransactionsOfOtherUsers() throws Exception {
		String otherAccount = createAccount(this.userB, "Conta B");
		perform(json(post(TRANSACTIONS), transactionBody(otherAccount, this.leisure, "70.00", "2026-11-10")),
				this.userB)
			.andExpect(status().isCreated());
		createBudget(this.userA, this.leisure, MONTH, "100");

		perform(get(BUDGETS).param("month", MONTH), this.userA).andExpect(jsonPath("$[0].spent").value(0));
	}

	@Test
	void requestsWithoutTokenAreRejected() throws Exception {
		this.mvc.perform(get(BUDGETS).param("month", MONTH)).andExpect(status().isUnauthorized());
		this.mvc.perform(json(post(BUDGETS), "{}")).andExpect(status().isUnauthorized());
	}

	@Test
	void endpointsAreDocumented() throws Exception {
		this.mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/budgets'].post").exists())
			.andExpect(jsonPath("$.paths['/api/v1/budgets/{id}'].delete").exists());
	}

	private String createBudget(RequestPostProcessor user, String category, String month, String limit)
			throws Exception {
		return idOf(perform(json(post(BUDGETS), body(category, month, limit)), user));
	}

	private void createTransaction(String accountId, String category, String amount, String date) throws Exception {
		idOf(perform(json(post(TRANSACTIONS), transactionBody(accountId, category, amount, date)), this.userA));
	}

	private String createAccount(RequestPostProcessor user, String name) throws Exception {
		String payload = "{\"name\":\"%s\",\"type\":\"CHECKING\",\"initialBalance\":0}".formatted(name);
		return idOf(perform(json(post(ACCOUNTS), payload), user));
	}

	private String createPersonalCategory(RequestPostProcessor user) throws Exception {
		return idOf(perform(json(post(CATEGORIES), "{\"name\":\"Pets\",\"type\":\"EXPENSE\"}"), user));
	}

	private String defaultCategory(String name) throws Exception {
		String response = perform(get(CATEGORIES), this.userA).andReturn().getResponse().getContentAsString();
		List<String> ids = JsonPath.read(response, "$[?(@.name == '%s')].id".formatted(name));
		return ids.get(0);
	}

	private static String idOf(ResultActions result) throws Exception {
		String response = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private ResultActions perform(MockHttpServletRequestBuilder request, RequestPostProcessor user) throws Exception {
		return this.mvc.perform(request.with(user));
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String payload) {
		return request.contentType(MediaType.APPLICATION_JSON).content(payload);
	}

	private static String body(String category, String month, String limit) {
		return "{\"categoryId\":\"%s\",\"month\":\"%s\",\"limit\":%s}".formatted(category, month, limit);
	}

	private static String transactionBody(String account, String category, String amount, String date) {
		return "{\"accountId\":\"%s\",\"categoryId\":\"%s\",\"amount\":%s,\"occurredOn\":\"%s\"}".formatted(account,
				category, amount, date);
	}

	private static RequestPostProcessor user() {
		return jwt().jwt((token) -> token.subject(UUID.randomUUID().toString()));
	}

}
