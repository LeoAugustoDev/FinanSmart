package com.finansmart_server.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.finansmart_server.budget.dto.BudgetResponse;
import com.finansmart_server.budget.dto.CreateBudgetRequest;
import com.finansmart_server.budget.exception.BudgetNotFoundException;
import com.finansmart_server.budget.exception.DuplicateBudgetException;
import com.finansmart_server.budget.exception.InvalidBudgetCategoryException;
import com.finansmart_server.category.Category;
import com.finansmart_server.category.CategoryRepository;
import com.finansmart_server.category.CategoryType;
import com.finansmart_server.shared.CurrentUser;
import com.finansmart_server.transaction.TransactionRepository;

class BudgetServiceTests {

	private static final String USER = "user-1";

	private static final YearMonth MONTH = YearMonth.of(2026, 11);

	private final BudgetRepository budgets = mock(BudgetRepository.class);

	private final CategoryRepository categories = mock(CategoryRepository.class);

	private final TransactionRepository transactions = mock(TransactionRepository.class);

	private final CurrentUser currentUser = mock(CurrentUser.class);

	private final BudgetService service = new BudgetService(this.budgets, this.categories, this.transactions,
			this.currentUser);

	private final Category expense = withId(new Category(null, "Lazer", CategoryType.EXPENSE));

	@BeforeEach
	void setUp() {
		given(this.currentUser.id()).willReturn(USER);
		given(this.budgets.saveAndFlush(any())).willAnswer((invocation) -> invocation.getArgument(0));
		given(this.categories.findVisibleById(this.expense.getId(), USER)).willReturn(Optional.of(this.expense));
		given(this.transactions.sumOfCategory(any(), any(), any(), any())).willReturn(BigDecimal.ZERO);
	}

	@Test
	void createsBudgetForExpenseCategory() {
		BudgetResponse response = this.service.create(request(this.expense.getId()));

		assertThat(response.categoryName()).isEqualTo("Lazer");
		assertThat(response.month()).isEqualTo(MONTH);
		assertThat(response.limit()).isEqualByComparingTo("500.00");
		assertThat(response.spent()).isEqualByComparingTo("0");
		assertThat(response.status()).isEqualTo(BudgetStatus.OK);
	}

	@Test
	void rejectsIncomeCategory() {
		Category income = withId(new Category(null, "Salário", CategoryType.INCOME));
		given(this.categories.findVisibleById(income.getId(), USER)).willReturn(Optional.of(income));

		assertThatThrownBy(() -> this.service.create(request(income.getId())))
			.isInstanceOf(InvalidBudgetCategoryException.class);
		verify(this.budgets, never()).saveAndFlush(any());
	}

	@Test
	void rejectsCategoryThatIsNotVisibleToTheUser() {
		UUID other = UUID.randomUUID();
		given(this.categories.findVisibleById(other, USER)).willReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.create(request(other)))
			.isInstanceOf(InvalidBudgetCategoryException.class);
	}

	@Test
	void rejectsSecondBudgetForTheSameCategoryAndMonth() {
		given(this.budgets.existsByUserIdAndCategoryIdAndReferenceMonth(USER, this.expense.getId(), MONTH.atDay(1)))
			.willReturn(true);

		assertThatThrownBy(() -> this.service.create(request(this.expense.getId())))
			.isInstanceOf(DuplicateBudgetException.class);
	}

	@Test
	void translatesConcurrentDuplicateFromTheDatabase() {
		given(this.budgets.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("duplicado"));

		assertThatThrownBy(() -> this.service.create(request(this.expense.getId())))
			.isInstanceOf(DuplicateBudgetException.class);
	}

	@Test
	void budgetOfAnotherUserIsNotFound() {
		UUID id = UUID.randomUUID();
		given(this.budgets.findByIdAndUserId(id, USER)).willReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.get(id)).isInstanceOf(BudgetNotFoundException.class);
	}

	@ParameterizedTest
	@CsvSource({ "0.00,  0.0,   OK", "80.00, 80.0,  OK", "80.01, 80.0,  WARNING", "100.00, 100.0, WARNING",
			"100.01, 100.0, EXCEEDED", "150.00, 150.0, EXCEEDED" })
	void classifiesConsumptionAgainstTheLimit(String spent, String percent, BudgetStatus status) {
		Budget budget = withId(new Budget(USER, this.expense, MONTH, new BigDecimal("100.00")));

		BudgetResponse response = BudgetResponse.from(budget, new BigDecimal(spent));

		assertThat(response.percentUsed()).isEqualByComparingTo(percent);
		assertThat(response.status()).isEqualTo(status);
		assertThat(response.remaining()).isEqualByComparingTo(new BigDecimal("100.00").subtract(new BigDecimal(spent)));
	}

	private static CreateBudgetRequest request(UUID categoryId) {
		return new CreateBudgetRequest(categoryId, MONTH, new BigDecimal("500.00"));
	}

	private static <T> T withId(T entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
		return entity;
	}

}
