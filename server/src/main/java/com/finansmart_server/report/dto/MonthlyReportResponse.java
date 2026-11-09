package com.finansmart_server.report.dto;

import java.time.YearMonth;
import java.util.List;

public record MonthlyReportResponse(YearMonth month, YearMonth previousMonth, Comparison income, Comparison expense,
		Comparison result, List<CategorySpending> expensesByCategory) {
}
