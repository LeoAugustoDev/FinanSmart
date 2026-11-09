package com.finansmart_server.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.finansmart_server.category.CategoryType;
import com.finansmart_server.report.dto.CategorySpending;
import com.finansmart_server.report.dto.Comparison;
import com.finansmart_server.report.dto.MonthlyReportResponse;
import com.finansmart_server.shared.CurrentUser;
import com.finansmart_server.transaction.TransactionRepository;
import com.finansmart_server.transaction.TransactionRepository.NamedCategoryTotal;
import com.finansmart_server.transaction.TransactionRepository.TypeTotal;

class ReportServiceTests {

	private static final String USER = "user-1";

	private static final YearMonth MONTH = YearMonth.of(2026, 1);

	private static final LocalDate JANUARY = LocalDate.of(2026, 1, 1);

	private static final LocalDate DECEMBER = LocalDate.of(2025, 12, 1);

	private static final LocalDate FEBRUARY = LocalDate.of(2026, 2, 1);

	private static final UUID FOOD = UUID.randomUUID();

	private static final UUID LEISURE = UUID.randomUUID();

	private static final UUID TRANSPORT = UUID.randomUUID();

	private final TransactionRepository transactions = mock(TransactionRepository.class);

	private final CurrentUser currentUser = mock(CurrentUser.class);

	private final ReportService service = new ReportService(this.transactions, this.currentUser);

	@BeforeEach
	void setUp() {
		given(this.currentUser.id()).willReturn(USER);
		given(this.transactions.sumByType(any(), any(), any())).willReturn(List.of());
		given(this.transactions.sumExpensesByNamedCategory(any(), any(), any())).willReturn(List.of());
	}

	@Test
	void comparesTheMonthWithThePreviousOneAcrossTheYearBoundary() {
		given(this.transactions.sumByType(USER, JANUARY, FEBRUARY))
			.willReturn(List.of(type(CategoryType.INCOME, "3000.00"), type(CategoryType.EXPENSE, "1200.00")));
		given(this.transactions.sumByType(USER, DECEMBER, JANUARY))
			.willReturn(List.of(type(CategoryType.INCOME, "2500.00"), type(CategoryType.EXPENSE, "1500.00")));

		MonthlyReportResponse report = this.service.monthly(MONTH);

		assertThat(report.month()).isEqualTo(MONTH);
		assertThat(report.previousMonth()).isEqualTo(YearMonth.of(2025, 12));
		assertComparison(report.income(), "3000.00", "2500.00", "500.00", "20.0");
		assertComparison(report.expense(), "1200.00", "1500.00", "-300.00", "-20.0");
		assertComparison(report.result(), "1800.00", "1000.00", "800.00", "80.0");
	}

	@Test
	void emptyMonthsProduceZerosWithoutPercent() {
		MonthlyReportResponse report = this.service.monthly(MONTH);

		assertThat(report.income().current()).isEqualByComparingTo("0");
		assertThat(report.income().changePercent()).isNull();
		assertThat(report.result().changePercent()).isNull();
		assertThat(report.expensesByCategory()).isEmpty();
	}

	@Test
	void categoriesShowShareAndComparisonOrderedBySpending() {
		given(this.transactions.sumByType(USER, JANUARY, FEBRUARY))
			.willReturn(List.of(type(CategoryType.EXPENSE, "400.00")));
		given(this.transactions.sumExpensesByNamedCategory(USER, JANUARY, FEBRUARY))
			.willReturn(List.of(category(LEISURE, "Lazer", "100.00"), category(FOOD, "Alimentação", "300.00")));
		given(this.transactions.sumExpensesByNamedCategory(USER, DECEMBER, JANUARY))
			.willReturn(List.of(category(FOOD, "Alimentação", "200.00"), category(TRANSPORT, "Transporte", "80.00")));

		List<CategorySpending> categories = this.service.monthly(MONTH).expensesByCategory();

		assertThat(categories).extracting(CategorySpending::categoryName)
			.containsExactly("Alimentação", "Lazer", "Transporte");

		CategorySpending food = categories.get(0);
		assertThat(food.share()).isEqualByComparingTo("75.0");
		assertThat(food.previousTotal()).isEqualByComparingTo("200.00");
		assertThat(food.change()).isEqualByComparingTo("100.00");
		assertThat(food.changePercent()).isEqualByComparingTo("50.0");

		CategorySpending leisure = categories.get(1);
		assertThat(leisure.share()).isEqualByComparingTo("25.0");
		assertThat(leisure.previousTotal()).isEqualByComparingTo("0");
		assertThat(leisure.changePercent()).isNull();

		CategorySpending transport = categories.get(2);
		assertThat(transport.total()).isEqualByComparingTo("0");
		assertThat(transport.share()).isEqualByComparingTo("0");
		assertThat(transport.change()).isEqualByComparingTo("-80.00");
		assertThat(transport.changePercent()).isEqualByComparingTo("-100.0");
	}

	@Test
	void categoriesWithTheSameSpendingAreOrderedByName() {
		given(this.transactions.sumByType(USER, JANUARY, FEBRUARY))
			.willReturn(List.of(type(CategoryType.EXPENSE, "100.00")));
		given(this.transactions.sumExpensesByNamedCategory(USER, JANUARY, FEBRUARY))
			.willReturn(List.of(category(LEISURE, "lazer", "50.00"), category(FOOD, "Alimentação", "50.00")));

		assertThat(this.service.monthly(MONTH).expensesByCategory()).extracting(CategorySpending::categoryName)
			.containsExactly("Alimentação", "lazer");
	}

	@ParameterizedTest
	@CsvSource({ "150.00, 100.00, 50.0", "100.00, 300.00, -66.7", "100.00, 100.00, 0.0", "50.00, -100.00, 150.0",
			"-200.00, -100.00, -100.0", "0.00, 80.00, -100.0" })
	void changePercentIsRelativeToThePreviousAbsoluteValue(String current, String previous, String expected) {
		Comparison comparison = Comparison.of(new BigDecimal(current), new BigDecimal(previous));

		assertThat(comparison.changePercent()).isEqualByComparingTo(expected);
		assertThat(comparison.changePercent().scale()).isEqualTo(1);
	}

	@Test
	void changePercentIsUndefinedWhenThePreviousValueIsZero() {
		Comparison comparison = Comparison.of(new BigDecimal("80.00"), BigDecimal.ZERO);

		assertThat(comparison.change()).isEqualByComparingTo("80.00");
		assertThat(comparison.changePercent()).isNull();
	}

	private static void assertComparison(Comparison comparison, String current, String previous, String change,
			String changePercent) {
		assertThat(comparison.current()).isEqualByComparingTo(current);
		assertThat(comparison.previous()).isEqualByComparingTo(previous);
		assertThat(comparison.change()).isEqualByComparingTo(change);
		assertThat(comparison.changePercent()).isEqualByComparingTo(changePercent);
	}

	private static TypeTotal type(CategoryType type, String total) {
		return new TypeTotal() {
			@Override
			public CategoryType getType() {
				return type;
			}

			@Override
			public BigDecimal getTotal() {
				return new BigDecimal(total);
			}
		};
	}

	private static NamedCategoryTotal category(UUID id, String name, String total) {
		return new NamedCategoryTotal() {
			@Override
			public UUID getCategoryId() {
				return id;
			}

			@Override
			public String getCategoryName() {
				return name;
			}

			@Override
			public BigDecimal getTotal() {
				return new BigDecimal(total);
			}
		};
	}

}
