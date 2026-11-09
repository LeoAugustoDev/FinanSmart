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
import org.junit.jupiter.params.provider.ValueSource;
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
class TransactionApiIntegrationTests {

	private static final String TRANSACTIONS = "/api/v1/transactions";

	private static final String ACCOUNTS = "/api/v1/accounts";

	private static final String CATEGORIES = "/api/v1/categories";

	@Autowired
	private MockMvc mvc;

	private final RequestPostProcessor userA = user();

	private final RequestPostProcessor userB = user();

	private String account;

	private String expense;

	private String income;

	@BeforeEach
	void setUp() throws Exception {
		this.account = createAccount(this.userA, "Nubank");
		this.expense = defaultCategory(this.userA, "Lazer");
		this.income = defaultCategory(this.userA, "Salário");
	}

	@Test
	void fullLifecycle() throws Exception {
		String id = createTransaction(this.userA, this.account, this.expense, "50.00", "2026-11-09");
		String path = TRANSACTIONS + "/" + id;

		perform(get(path), this.userA).andExpect(status().isOk())
			.andExpect(jsonPath("$.accountName").value("Nubank"))
			.andExpect(jsonPath("$.categoryName").value("Lazer"))
			.andExpect(jsonPath("$.type").value("EXPENSE"))
			.andExpect(jsonPath("$.amount").value(50.00))
			.andExpect(jsonPath("$.occurredOn").value("2026-11-09"));

		perform(json(put(path), body(this.account, this.income, "75.50", "2026-11-10", "Bônus")), this.userA)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.type").value("INCOME"))
			.andExpect(jsonPath("$.amount").value(75.50))
			.andExpect(jsonPath("$.description").value("Bônus"));

		perform(delete(path), this.userA).andExpect(status().isNoContent());
		perform(get(path), this.userA).andExpect(status().isNotFound());
	}

	@Test
	void createReturnsLocationAndNormalizesDescription() throws Exception {
		perform(json(post(TRANSACTIONS), body(this.account, this.expense, "1", "2026-11-09", "   ")), this.userA)
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", containsString(TRANSACTIONS + "/")))
			.andExpect(jsonPath("$.description").doesNotExist());
	}

	@Test
	void listsNewestFirstWithPagination() throws Exception {
		String oldest = createTransaction(this.userA, this.account, this.expense, "1", "2026-01-01");
		String middle = createTransaction(this.userA, this.account, this.expense, "2", "2026-02-01");
		String newest = createTransaction(this.userA, this.account, this.expense, "3", "2026-03-01");

		perform(get(TRANSACTIONS).param("size", "2"), this.userA).andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id", contains(newest, middle)))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(2));

		perform(get(TRANSACTIONS).param("size", "2").param("page", "1"), this.userA)
			.andExpect(jsonPath("$.content[*].id", contains(oldest)));
	}

	@Test
	void filtersByPeriodIncludingTheLimits() throws Exception {
		createTransaction(this.userA, this.account, this.expense, "1", "2026-01-31");
		String first = createTransaction(this.userA, this.account, this.expense, "2", "2026-02-01");
		String last = createTransaction(this.userA, this.account, this.expense, "3", "2026-02-28");
		createTransaction(this.userA, this.account, this.expense, "4", "2026-03-01");

		perform(get(TRANSACTIONS).param("from", "2026-02-01").param("to", "2026-02-28"), this.userA)
			.andExpect(jsonPath("$.content[*].id", contains(last, first)));
	}

	@Test
	void filtersByAccountAndCategory() throws Exception {
		String otherAccount = createAccount(this.userA, "Itaú");
		String target = createTransaction(this.userA, otherAccount, this.income, "1", "2026-02-01");
		createTransaction(this.userA, this.account, this.income, "2", "2026-02-01");
		createTransaction(this.userA, otherAccount, this.expense, "3", "2026-02-01");

		perform(get(TRANSACTIONS).param("accountId", otherAccount).param("categoryId", this.income), this.userA)
			.andExpect(jsonPath("$.content[*].id", contains(target)));
	}

	@Test
	void invertedPeriodIsRejected() throws Exception {
		perform(get(TRANSACTIONS).param("from", "2026-02-02").param("to", "2026-02-01"), this.userA)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.from").exists());
	}

	@Test
	void pageSizeAboveTheLimitIsRejected() throws Exception {
		perform(get(TRANSACTIONS).param("size", "101"), this.userA).andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-1", "1.999" })
	void invalidAmountIsRejected(String amount) throws Exception {
		perform(json(post(TRANSACTIONS), body(this.account, this.expense, amount, "2026-11-09", "x")), this.userA)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.amount").exists());
	}

	@Test
	void requiredFieldsReturn400WithTheFields() throws Exception {
		perform(json(post(TRANSACTIONS), "{}"), this.userA).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.accountId").exists())
			.andExpect(jsonPath("$.errors.categoryId").exists())
			.andExpect(jsonPath("$.errors.amount").exists())
			.andExpect(jsonPath("$.errors.occurredOn").exists());
	}

	@Test
	void descriptionLongerThan255CharactersIsRejected() throws Exception {
		perform(json(post(TRANSACTIONS), body(this.account, this.expense, "1", "2026-11-09", "x".repeat(256))),
				this.userA)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.description").exists());
	}

	@Test
	void personalCategoryOfTheUserIsAccepted() throws Exception {
		String personal = createCategory(this.userA, "Pets");

		perform(json(post(TRANSACTIONS), body(this.account, personal, "1", "2026-11-09", "x")), this.userA)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.categoryName").value("Pets"));
	}

	@Test
	void resourcesOfAnotherUserCannotBeReferenced() throws Exception {
		String otherAccount = createAccount(this.userB, "Conta B");
		String otherCategory = createCategory(this.userB, "Categoria B");

		perform(json(post(TRANSACTIONS), body(otherAccount, this.expense, "1", "2026-11-09", "x")), this.userA)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.accountId").exists());
		perform(json(post(TRANSACTIONS), body(this.account, otherCategory, "1", "2026-11-09", "x")), this.userA)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.categoryId").exists());
	}

	@Test
	void archivedAccountDoesNotReceiveNewTransactions() throws Exception {
		perform(patch(ACCOUNTS + "/" + this.account + "/archive"), this.userA).andExpect(status().isNoContent());

		perform(json(post(TRANSACTIONS), body(this.account, this.expense, "1", "2026-11-09", "x")), this.userA)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.accountId").exists());
	}

	@Test
	void accountWithTransactionsCannotBeDeletedButCanBeArchived() throws Exception {
		String id = createTransaction(this.userA, this.account, this.expense, "1", "2026-11-09");
		String path = ACCOUNTS + "/" + this.account;

		perform(delete(path), this.userA).andExpect(status().isConflict());
		perform(patch(path + "/archive"), this.userA).andExpect(status().isNoContent());
		perform(patch(path + "/unarchive"), this.userA).andExpect(status().isNoContent());

		perform(delete(TRANSACTIONS + "/" + id), this.userA).andExpect(status().isNoContent());
		perform(delete(path), this.userA).andExpect(status().isNoContent());
	}

	@Test
	void categoryWithTransactionsCannotBeDeleted() throws Exception {
		String personal = createCategory(this.userA, "Pets");
		String id = createTransaction(this.userA, this.account, personal, "1", "2026-11-09");
		String path = CATEGORIES + "/" + personal;

		perform(delete(path), this.userA).andExpect(status().isConflict());

		perform(delete(TRANSACTIONS + "/" + id), this.userA).andExpect(status().isNoContent());
		perform(delete(path), this.userA).andExpect(status().isNoContent());
	}

	@Test
	void transactionsOfAnotherUserRespondNotFound() throws Exception {
		String id = createTransaction(this.userA, this.account, this.expense, "1", "2026-11-09");
		String path = TRANSACTIONS + "/" + id;
		String otherAccount = createAccount(this.userB, "Conta B");

		perform(get(TRANSACTIONS), this.userB).andExpect(jsonPath("$.content", hasSize(0)));
		perform(get(path), this.userB).andExpect(status().isNotFound());
		perform(json(put(path), body(otherAccount, this.expense, "9", "2026-11-09", "x")), this.userB)
			.andExpect(status().isNotFound());
		perform(delete(path), this.userB).andExpect(status().isNotFound());

		perform(get(path), this.userA).andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(1.00));
	}

	@Test
	void requestsWithoutTokenAreRejected() throws Exception {
		this.mvc.perform(get(TRANSACTIONS)).andExpect(status().isUnauthorized());
		this.mvc.perform(json(post(TRANSACTIONS), "{}")).andExpect(status().isUnauthorized());
	}

	@Test
	void endpointsAreDocumented() throws Exception {
		this.mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/transactions'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/transactions/{id}'].put").exists());
	}

	private String createTransaction(RequestPostProcessor user, String accountId, String categoryId, String amount,
			String date) throws Exception {
		return idOf(perform(json(post(TRANSACTIONS), body(accountId, categoryId, amount, date, "Teste")), user));
	}

	private String createAccount(RequestPostProcessor user, String name) throws Exception {
		String payload = "{\"name\":\"%s\",\"type\":\"CHECKING\",\"initialBalance\":0}".formatted(name);
		return idOf(perform(json(post(ACCOUNTS), payload), user));
	}

	private String createCategory(RequestPostProcessor user, String name) throws Exception {
		String payload = "{\"name\":\"%s\",\"type\":\"EXPENSE\"}".formatted(name);
		return idOf(perform(json(post(CATEGORIES), payload), user));
	}

	private String defaultCategory(RequestPostProcessor user, String name) throws Exception {
		String response = perform(get(CATEGORIES), user).andReturn().getResponse().getContentAsString();
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

	private static String body(String accountId, String categoryId, String amount, String date, String description) {
		return "{\"accountId\":\"%s\",\"categoryId\":\"%s\",\"amount\":%s,\"occurredOn\":\"%s\",\"description\":\"%s\"}"
			.formatted(accountId, categoryId, amount, date, description);
	}

	private static RequestPostProcessor user() {
		return jwt().jwt((token) -> token.subject(UUID.randomUUID().toString()));
	}

}
