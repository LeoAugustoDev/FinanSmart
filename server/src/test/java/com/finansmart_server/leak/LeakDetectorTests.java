package com.finansmart_server.leak;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.finansmart_server.leak.LeakDetector.Expense;
import com.finansmart_server.leak.dto.Charge;
import com.finansmart_server.leak.dto.LeakResponse;
import com.finansmart_server.leak.dto.Overspend;

class LeakDetectorTests {

	private static final YearMonth MONTH = YearMonth.of(2026, 1);

	private static final Category SUBSCRIPTIONS = Category.system("Assinaturas");

	private static final Category FEES = Category.system("Tarifas e juros");

	private static final Category LEISURE = Category.system("Lazer");

	private static final Category FOOD = Category.system("Alimentação");

	private static final Category HOUSING = Category.system("Moradia");

	@Test
	void nothingLeaksWithoutExpenses() {
		LeakResponse leaks = LeakDetector.detect(MONTH, List.of());

		assertThat(leaks.month()).isEqualTo(MONTH);
		assertThat(leaks.total()).isEqualByComparingTo("0");
		assertThat(leaks.subscriptions().items()).isEmpty();
		assertThat(leaks.fees().items()).isEmpty();
		assertThat(leaks.aboveAverage().items()).isEmpty();
	}

	@Test
	void expensesInTheSubscriptionsCategoryAreGroupedByDescription() {
		LeakResponse leaks = LeakDetector.detect(MONTH,
				List.of(expense(SUBSCRIPTIONS, "39.90", "2026-01-05", "Netflix"),
						expense(SUBSCRIPTIONS, "21.90", "2026-01-10", "Spotify"),
						expense(SUBSCRIPTIONS, "10.00", "2026-01-20", "  netflix "),
						expense(SUBSCRIPTIONS, "5.00", "2026-01-21", null),
						expense(SUBSCRIPTIONS, "99.00", "2025-12-05", "Academia"),
						expense(SUBSCRIPTIONS, "99.00", "2026-02-01", "Academia")));

		assertThat(leaks.subscriptions().items()).extracting(Charge::description)
			.containsExactly("Netflix", "Spotify", null);
		assertThat(leaks.subscriptions().items().get(0).amount()).isEqualByComparingTo("49.90");
		assertThat(leaks.subscriptions().items().get(0).recurring()).isFalse();
		assertThat(leaks.subscriptions().total()).isEqualByComparingTo("76.80");
		assertThat(leaks.total()).isEqualByComparingTo("76.80");
	}

	@Test
	void chargeRepeatedInThePreviousTwoMonthsIsARecurringSubscription() {
		LeakResponse leaks = LeakDetector.detect(MONTH,
				List.of(expense(LEISURE, "50.00", "2025-11-10", "Clube do Vinho"),
						expense(LEISURE, "52.00", "2025-12-10", "CLUBE DO  VINHO"),
						expense(LEISURE, "55.00", "2026-01-10", "Clube do vinho")));

		assertThat(leaks.subscriptions().items()).singleElement().satisfies((charge) -> {
			assertThat(charge.description()).isEqualTo("Clube do vinho");
			assertThat(charge.categoryName()).isEqualTo("Lazer");
			assertThat(charge.amount()).isEqualByComparingTo("55.00");
			assertThat(charge.recurring()).isTrue();
		});
	}

	@Test
	void chargeMissingInOneOfThePreviousMonthsIsNotRecurring() {
		LeakResponse leaks = LeakDetector.detect(MONTH, List.of(expense(LEISURE, "50.00", "2025-10-10", "Cinema"),
				expense(LEISURE, "50.00", "2025-12-10", "Cinema"), expense(LEISURE, "50.00", "2026-01-10", "Cinema")));

		assertThat(leaks.subscriptions().items()).isEmpty();
	}

	@Test
	void sameDescriptionWithVeryDifferentValuesIsNotRecurring() {
		LeakResponse leaks = LeakDetector.detect(MONTH,
				List.of(expense(FOOD, "400.00", "2025-11-10", "Mercado"),
						expense(FOOD, "620.00", "2025-12-10", "Mercado"),
						expense(FOOD, "500.00", "2026-01-10", "Mercado")));

		assertThat(leaks.subscriptions().items()).isEmpty();
	}

	@Test
	void essentialExpensesAreNeverRecurringSubscriptions() {
		LeakResponse leaks = LeakDetector.detect(MONTH,
				List.of(expense(HOUSING, "1500.00", "2025-11-05", "Aluguel"),
						expense(HOUSING, "1500.00", "2025-12-05", "Aluguel"),
						expense(HOUSING, "1500.00", "2026-01-05", "Aluguel")));

		assertThat(leaks.total()).isEqualByComparingTo("0");
	}

	@Test
	void feesAreTheExpensesOfTheFeesCategoryInTheMonth() {
		LeakResponse leaks = LeakDetector.detect(MONTH,
				List.of(expense(FEES, "12.00", "2026-01-02", "Tarifa de conta"),
						expense(FEES, "30.50", "2026-01-15", "Juros do cartão"),
						expense(FEES, "12.00", "2025-12-02", "Tarifa de conta"),
						expense(FEES, "12.00", "2025-11-02", "Tarifa de conta")));

		assertThat(leaks.fees().items()).extracting(Charge::description)
			.containsExactly("Juros do cartão", "Tarifa de conta");
		assertThat(leaks.fees().total()).isEqualByComparingTo("42.50");
		assertThat(leaks.subscriptions().items()).isEmpty();
		assertThat(leaks.aboveAverage().items()).isEmpty();
	}

	@Test
	void categoryNamedLikeASystemOneButCreatedByTheUserHasNoSpecialRule() {
		Category custom = new Category(UUID.randomUUID(), "Tarifas e juros", false);

		LeakResponse leaks = LeakDetector.detect(MONTH, List.of(expense(custom, "12.00", "2026-01-02", "Tarifa")));

		assertThat(leaks.fees().items()).isEmpty();
	}

	@Test
	void categoryAboveItsAverageLeaksOnlyTheExcess() {
		LeakResponse leaks = LeakDetector.detect(MONTH,
				List.of(expense(FOOD, "300.00", "2025-10-10", null), expense(FOOD, "200.00", "2025-11-10", null),
						expense(FOOD, "100.00", "2025-12-10", null), expense(FOOD, "150.00", "2026-01-10", null),
						expense(FOOD, "200.00", "2026-01-20", null),
						// Fora da janela de três meses.
						expense(FOOD, "9000.00", "2025-09-30", null)));

		assertThat(leaks.aboveAverage().items()).singleElement().satisfies((overspend) -> {
			assertThat(overspend.categoryId()).isEqualTo(FOOD.id());
			assertThat(overspend.categoryName()).isEqualTo("Alimentação");
			assertThat(overspend.spent()).isEqualByComparingTo("350.00");
			assertThat(overspend.average()).isEqualByComparingTo("200.00");
			assertThat(overspend.excess()).isEqualByComparingTo("150.00");
		});
		assertThat(leaks.total()).isEqualByComparingTo("150.00");
	}

	@Test
	void spendingUpToTwentyPercentAboveTheAverageIsNotALeak() {
		LeakResponse leaks = LeakDetector.detect(MONTH,
				List.of(expense(FOOD, "200.00", "2025-10-10", null), expense(FOOD, "200.00", "2025-11-10", null),
						expense(FOOD, "200.00", "2025-12-10", null), expense(FOOD, "240.00", "2026-01-10", null)));

		assertThat(leaks.aboveAverage().items()).isEmpty();
	}

	@Test
	void categoryWithLessThanTwoMonthsOfHistoryHasNoAverage() {
		LeakResponse leaks = LeakDetector.detect(MONTH, List.of(expense(LEISURE, "30.00", "2025-12-10", null),
				expense(LEISURE, "900.00", "2026-01-10", null), expense(FOOD, "500.00", "2026-01-11", null)));

		assertThat(leaks.aboveAverage().items()).isEmpty();
	}

	@Test
	void monthWithoutSpendingStaysOutOfTheAverage() {
		LeakResponse leaks = LeakDetector.detect(MONTH, List.of(expense(LEISURE, "100.00", "2025-10-10", null),
				expense(LEISURE, "100.00", "2025-12-10", null), expense(LEISURE, "130.00", "2026-01-10", null)));

		assertThat(leaks.aboveAverage().items()).singleElement().satisfies((overspend) -> {
			assertThat(overspend.average()).isEqualByComparingTo("100.00");
			assertThat(overspend.excess()).isEqualByComparingTo("30.00");
		});
	}

	@Test
	void noExpenseIsCountedByTwoDetectors() {
		LeakResponse leaks = LeakDetector.detect(MONTH,
				List.of(expense(LEISURE, "40.00", "2025-11-10", "Streaming"),
						expense(LEISURE, "40.00", "2025-12-10", "Streaming"),
						expense(LEISURE, "40.00", "2026-01-10", "Streaming"),
						expense(LEISURE, "60.00", "2025-11-12", null), expense(LEISURE, "60.00", "2025-12-12", null),
						expense(LEISURE, "100.00", "2026-01-12", null)));

		assertThat(leaks.subscriptions().total()).isEqualByComparingTo("40.00");
		assertThat(leaks.aboveAverage().items()).singleElement().satisfies((overspend) -> {
			assertThat(overspend.spent()).isEqualByComparingTo("100.00");
			assertThat(overspend.average()).isEqualByComparingTo("60.00");
		});
		assertThat(leaks.total()).isEqualByComparingTo("80.00");
	}

	@Test
	void overspendsAreSortedByExcess() {
		LeakResponse leaks = LeakDetector.detect(MONTH,
				List.of(expense(FOOD, "100.00", "2025-11-10", null), expense(FOOD, "100.00", "2025-12-10", null),
						expense(FOOD, "100.00", "2025-10-10", null), expense(FOOD, "150.00", "2026-01-10", null),
						expense(LEISURE, "100.00", "2025-11-10", null), expense(LEISURE, "100.00", "2025-12-10", null),
						expense(LEISURE, "100.00", "2025-10-10", null),
						expense(LEISURE, "400.00", "2026-01-10", null)));

		assertThat(leaks.aboveAverage().items()).extracting(Overspend::categoryName)
			.containsExactly("Lazer", "Alimentação");
		assertThat(leaks.aboveAverage().total()).isEqualByComparingTo("350.00");
	}

	private static Expense expense(Category category, String amount, String date, String description) {
		return new Expense(category.id(), category.name(), category.system(), new BigDecimal(amount),
				LocalDate.parse(date), description);
	}

	private record Category(UUID id, String name, boolean system) {

		static Category system(String name) {
			return new Category(UUID.randomUUID(), name, true);
		}

	}

}
