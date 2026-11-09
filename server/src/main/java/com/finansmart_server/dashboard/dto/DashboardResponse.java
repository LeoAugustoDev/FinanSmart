package com.finansmart_server.dashboard.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import com.finansmart_server.account.dto.AccountResponse;
import com.finansmart_server.transaction.dto.TransactionResponse;

public record DashboardResponse(YearMonth month, BigDecimal totalBalance, BigDecimal income, BigDecimal expense,
		BigDecimal result, List<AccountResponse> accounts, List<TransactionResponse> recentTransactions) {
}
