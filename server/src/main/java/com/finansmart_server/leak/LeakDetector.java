package com.finansmart_server.leak;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.finansmart_server.leak.dto.Charge;
import com.finansmart_server.leak.dto.ChargeGroup;
import com.finansmart_server.leak.dto.LeakResponse;
import com.finansmart_server.leak.dto.Overspend;
import com.finansmart_server.leak.dto.OverspendGroup;

/**
 * Regras do detector de vazamentos, sem acesso a banco.
 *
 * <ul>
 * <li>Assinaturas: despesas na categoria padrão "Assinaturas" e cobranças recorrentes
 * (mesma descrição, com valor parecido, no mês e nos dois anteriores).</li>
 * <li>Tarifas e juros: despesas na categoria padrão "Tarifas e juros".</li>
 * <li>Acima da média: categorias cujo gasto passou em mais de 20% da média dos meses
 * com gasto entre os três anteriores; conta como vazamento só o excedente.</li>
 * </ul>
 */
final class LeakDetector {

	/** Quantos meses anteriores ao analisado o detector precisa receber. */
	static final int HISTORY_MONTHS = 3;

	static final String SUBSCRIPTIONS = "Assinaturas";

	static final String FEES = "Tarifas e juros";

	/** Gastos fixos que se repetem todo mês sem serem vazamento, como aluguel e mensalidade. */
	private static final Set<String> ESSENTIALS = Set.of("Moradia", "Saúde", "Educação");

	/** Meses anteriores seguidos em que a cobrança precisa aparecer para ser recorrente. */
	private static final int RECURRENCE_MONTHS = 2;

	private static final BigDecimal RECURRENCE_TOLERANCE = new BigDecimal("0.10");

	/** Meses do histórico com gasto para a média da categoria ser confiável. */
	private static final int MIN_MONTHS_WITH_SPENDING = 2;

	private static final BigDecimal OVERSPEND_FACTOR = new BigDecimal("1.20");

	private static final Comparator<Charge> CHARGES_BY_AMOUNT = Comparator.comparing(Charge::amount)
		.reversed()
		.thenComparing((charge) -> (charge.description() != null) ? charge.description() : "",
				String.CASE_INSENSITIVE_ORDER);

	private static final Comparator<Overspend> OVERSPENDS_BY_EXCESS = Comparator.comparing(Overspend::excess)
		.reversed()
		.thenComparing(Overspend::categoryName, String.CASE_INSENSITIVE_ORDER);

	private LeakDetector() {
	}

	/**
	 * @param expenses despesas do mês analisado e dos {@value #HISTORY_MONTHS} anteriores
	 */
	static LeakResponse detect(YearMonth month, List<Expense> expenses) {
		Set<String> recurring = recurringKeys(month, expenses);

		List<Expense> current = expenses.stream().filter((expense) -> expense.isIn(month)).toList();
		ChargeGroup subscriptions = charges(
				current.stream().filter((expense) -> isSubscription(expense, recurring)).toList(), recurring);
		ChargeGroup fees = charges(current.stream().filter(Expense::isFee).toList(), Set.of());
		OverspendGroup aboveAverage = aboveAverage(month,
				expenses.stream()
					.filter((expense) -> !expense.isFee() && !isSubscription(expense, recurring))
					.toList());

		BigDecimal total = subscriptions.total().add(fees.total()).add(aboveAverage.total());
		return new LeakResponse(month, total, subscriptions, fees, aboveAverage);
	}

	private static boolean isSubscription(Expense expense, Set<String> recurring) {
		return expense.isSubscriptionCategory() || (expense.canRecur() && recurring.contains(expense.key()));
	}

	/** Descrições que se repetem, com valor parecido, no mês e nos meses anteriores seguidos. */
	private static Set<String> recurringKeys(YearMonth month, List<Expense> expenses) {
		Map<YearMonth, Map<String, BigDecimal>> byMonth = new HashMap<>();
		for (Expense expense : expenses) {
			if (expense.canRecur() && expense.key() != null) {
				byMonth.computeIfAbsent(YearMonth.from(expense.occurredOn()), (key) -> new HashMap<>())
					.merge(expense.key(), expense.amount(), BigDecimal::add);
			}
		}

		Set<String> recurring = new HashSet<>();
		byMonth.getOrDefault(month, Map.of()).forEach((key, amount) -> {
			BigDecimal tolerance = amount.multiply(RECURRENCE_TOLERANCE);
			boolean repeats = true;
			for (int back = 1; back <= RECURRENCE_MONTHS; back++) {
				BigDecimal previous = byMonth.getOrDefault(month.minusMonths(back), Map.of()).get(key);
				repeats &= previous != null && previous.subtract(amount).abs().compareTo(tolerance) <= 0;
			}
			if (repeats) {
				recurring.add(key);
			}
		});
		return recurring;
	}

	private static ChargeGroup charges(List<Expense> expenses, Set<String> recurring) {
		Map<String, List<Expense>> groups = new LinkedHashMap<>();
		for (Expense expense : expenses) {
			String group = (expense.key() != null) ? "d:" + expense.key() : "c:" + expense.categoryId();
			groups.computeIfAbsent(group, (key) -> new ArrayList<>()).add(expense);
		}

		List<Charge> items = groups.values().stream().map((group) -> {
			Expense first = group.get(0);
			String description = (first.key() != null) ? first.description().strip() : null;
			return new Charge(description, first.categoryName(), sum(group.stream().map(Expense::amount).toList()),
					first.key() != null && recurring.contains(first.key()));
		}).sorted(CHARGES_BY_AMOUNT).toList();
		return new ChargeGroup(sum(items.stream().map(Charge::amount).toList()), items);
	}

	private static OverspendGroup aboveAverage(YearMonth month, List<Expense> expenses) {
		Map<UUID, String> names = new LinkedHashMap<>();
		Map<UUID, BigDecimal> spent = new HashMap<>();
		Map<UUID, Map<YearMonth, BigDecimal>> history = new HashMap<>();
		for (Expense expense : expenses) {
			YearMonth expenseMonth = YearMonth.from(expense.occurredOn());
			if (expenseMonth.equals(month)) {
				names.put(expense.categoryId(), expense.categoryName());
				spent.merge(expense.categoryId(), expense.amount(), BigDecimal::add);
			}
			else if (expenseMonth.isBefore(month) && !expenseMonth.isBefore(month.minusMonths(HISTORY_MONTHS))) {
				history.computeIfAbsent(expense.categoryId(), (key) -> new HashMap<>())
					.merge(expenseMonth, expense.amount(), BigDecimal::add);
			}
		}

		List<Overspend> items = new ArrayList<>();
		names.forEach((categoryId, name) -> {
			Map<YearMonth, BigDecimal> previous = history.getOrDefault(categoryId, Map.of());
			if (previous.size() < MIN_MONTHS_WITH_SPENDING) {
				return;
			}
			// Mês sem gasto fica fora da média: contá-lo como zero acusaria quem começou a
			// registrar há pouco tempo.
			BigDecimal average = sum(previous.values()).divide(BigDecimal.valueOf(previous.size()), 2,
					RoundingMode.HALF_UP);
			BigDecimal total = spent.get(categoryId);
			if (total.compareTo(average.multiply(OVERSPEND_FACTOR)) > 0) {
				items.add(new Overspend(categoryId, name, total, average, total.subtract(average)));
			}
		});
		items.sort(OVERSPENDS_BY_EXCESS);
		return new OverspendGroup(sum(items.stream().map(Overspend::excess).toList()), List.copyOf(items));
	}

	private static BigDecimal sum(Iterable<BigDecimal> values) {
		BigDecimal total = BigDecimal.ZERO;
		for (BigDecimal value : values) {
			total = total.add(value);
		}
		return total;
	}

	/**
	 * @param systemCategory se a categoria é padrão do sistema; as regras por nome só
	 * valem para elas, porque o usuário pode criar uma categoria com o mesmo nome
	 */
	record Expense(UUID categoryId, String categoryName, boolean systemCategory, BigDecimal amount,
			LocalDate occurredOn, String description) {

		boolean isIn(YearMonth month) {
			return YearMonth.from(this.occurredOn).equals(month);
		}

		boolean isSubscriptionCategory() {
			return this.systemCategory && SUBSCRIPTIONS.equals(this.categoryName);
		}

		boolean isFee() {
			return this.systemCategory && FEES.equals(this.categoryName);
		}

		/** Tarifas têm detector próprio e gastos essenciais não são vazamento. */
		boolean canRecur() {
			return !isFee() && !(this.systemCategory && ESSENTIALS.contains(this.categoryName));
		}

		/** Descrição sem diferenças de caixa e espaços; nulo quando não há descrição. */
		String key() {
			if (this.description == null || this.description.isBlank()) {
				return null;
			}
			return this.description.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
		}

	}

}
