package com.finansmart_server.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Gasto de uma categoria no mês do relatório e no mês anterior.
 *
 * @param share participação no total de despesas do mês, com uma casa decimal
 */
public record CategorySpending(UUID categoryId, String categoryName, BigDecimal total, BigDecimal share,
		BigDecimal previousTotal, BigDecimal change, BigDecimal changePercent) {
}
