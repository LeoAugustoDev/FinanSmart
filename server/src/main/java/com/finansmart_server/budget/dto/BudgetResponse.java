package com.finansmart_server.budget.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.UUID;

import com.finansmart_server.budget.Budget;
import com.finansmart_server.budget.BudgetStatus;

public record BudgetResponse(UUID id, UUID categoryId, String categoryName, YearMonth month, BigDecimal limit,
		BigDecimal spent, BigDecimal remaining, BigDecimal percentUsed, BudgetStatus status) {

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private static final BigDecimal WARNING_PERCENT = BigDecimal.valueOf(80);

	public static BudgetResponse from(Budget budget, BigDecimal spent) {
		BigDecimal limit = budget.getLimitAmount();
		BigDecimal percentUsed = spent.multiply(HUNDRED).divide(limit, 1, RoundingMode.HALF_UP);
		return new BudgetResponse(budget.getId(), budget.getCategory().getId(), budget.getCategory().getName(),
				budget.getMonth(), limit, spent, limit.subtract(spent), percentUsed, statusOf(spent, limit));
	}

	/** Compara os valores exatos, para o arredondamento do percentual não antecipar um alerta. */
	private static BudgetStatus statusOf(BigDecimal spent, BigDecimal limit) {
		if (spent.compareTo(limit) > 0) {
			return BudgetStatus.EXCEEDED;
		}
		return (spent.multiply(HUNDRED).compareTo(limit.multiply(WARNING_PERCENT)) > 0) ? BudgetStatus.WARNING
				: BudgetStatus.OK;
	}

}
