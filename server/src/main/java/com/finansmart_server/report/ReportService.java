package com.finansmart_server.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finansmart_server.category.CategoryType;
import com.finansmart_server.report.dto.CategorySpending;
import com.finansmart_server.report.dto.Comparison;
import com.finansmart_server.report.dto.MonthlyReportResponse;
import com.finansmart_server.shared.CurrentUser;
import com.finansmart_server.transaction.TransactionRepository;
import com.finansmart_server.transaction.TransactionRepository.NamedCategoryTotal;

@Service
@Transactional(readOnly = true)
public class ReportService {

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private static final Comparator<CategorySpending> BY_TOTAL_THEN_NAME = Comparator
		.comparing(CategorySpending::total)
		.reversed()
		.thenComparing(CategorySpending::categoryName, String.CASE_INSENSITIVE_ORDER);

	private final TransactionRepository transactions;

	private final CurrentUser currentUser;

	ReportService(TransactionRepository transactions, CurrentUser currentUser) {
		this.transactions = transactions;
		this.currentUser = currentUser;
	}

	public MonthlyReportResponse monthly(YearMonth month) {
		String userId = this.currentUser.id();
		YearMonth previousMonth = month.minusMonths(1);

		Map<CategoryType, BigDecimal> current = totalsByType(userId, month);
		Map<CategoryType, BigDecimal> previous = totalsByType(userId, previousMonth);

		Comparison income = Comparison.of(total(current, CategoryType.INCOME), total(previous, CategoryType.INCOME));
		Comparison expense = Comparison.of(total(current, CategoryType.EXPENSE),
				total(previous, CategoryType.EXPENSE));
		Comparison result = Comparison.of(income.current().subtract(expense.current()),
				income.previous().subtract(expense.previous()));

		return new MonthlyReportResponse(month, previousMonth, income, expense, result,
				expensesByCategory(userId, month, previousMonth, expense.current()));
	}

	/**
	 * Inclui as categorias com gasto no mês ou no mês anterior, para que a comparação
	 * mostre também o que deixou de ser gasto.
	 */
	private List<CategorySpending> expensesByCategory(String userId, YearMonth month, YearMonth previousMonth,
			BigDecimal monthExpense) {
		Map<UUID, NamedCategoryTotal> current = byCategory(userId, month);
		Map<UUID, NamedCategoryTotal> previous = byCategory(userId, previousMonth);

		Map<UUID, String> names = new LinkedHashMap<>();
		current.values().forEach((row) -> names.put(row.getCategoryId(), row.getCategoryName()));
		previous.values().forEach((row) -> names.putIfAbsent(row.getCategoryId(), row.getCategoryName()));

		return names.entrySet().stream().map((entry) -> {
			BigDecimal total = totalOf(current.get(entry.getKey()));
			Comparison comparison = Comparison.of(total, totalOf(previous.get(entry.getKey())));
			return new CategorySpending(entry.getKey(), entry.getValue(), total, share(total, monthExpense),
					comparison.previous(), comparison.change(), comparison.changePercent());
		}).sorted(BY_TOTAL_THEN_NAME).toList();
	}

	private Map<CategoryType, BigDecimal> totalsByType(String userId, YearMonth month) {
		return this.transactions.sumByType(userId, month.atDay(1), month.plusMonths(1).atDay(1))
			.stream()
			.collect(Collectors.toMap(TransactionRepository.TypeTotal::getType,
					TransactionRepository.TypeTotal::getTotal));
	}

	private Map<UUID, NamedCategoryTotal> byCategory(String userId, YearMonth month) {
		return this.transactions.sumExpensesByNamedCategory(userId, month.atDay(1), month.plusMonths(1).atDay(1))
			.stream()
			.collect(Collectors.toMap(NamedCategoryTotal::getCategoryId, (row) -> row));
	}

	private static BigDecimal total(Map<CategoryType, BigDecimal> totals, CategoryType type) {
		return totals.getOrDefault(type, BigDecimal.ZERO);
	}

	private static BigDecimal totalOf(NamedCategoryTotal row) {
		return (row != null) ? row.getTotal() : BigDecimal.ZERO;
	}

	private static BigDecimal share(BigDecimal total, BigDecimal monthExpense) {
		if (monthExpense.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return total.multiply(HUNDRED).divide(monthExpense, 1, RoundingMode.HALF_UP);
	}

}
