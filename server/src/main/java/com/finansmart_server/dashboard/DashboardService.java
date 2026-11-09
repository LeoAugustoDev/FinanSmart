package com.finansmart_server.dashboard;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finansmart_server.account.AccountService;
import com.finansmart_server.account.dto.AccountResponse;
import com.finansmart_server.category.CategoryType;
import com.finansmart_server.dashboard.dto.DashboardResponse;
import com.finansmart_server.shared.CurrentUser;
import com.finansmart_server.transaction.TransactionFilter;
import com.finansmart_server.transaction.TransactionRepository;
import com.finansmart_server.transaction.TransactionService;
import com.finansmart_server.transaction.dto.TransactionResponse;

@Service
@Transactional(readOnly = true)
public class DashboardService {

	private static final int RECENT_TRANSACTIONS = 5;

	private final AccountService accounts;

	private final TransactionService transactions;

	private final TransactionRepository transactionRepository;

	private final CurrentUser currentUser;

	DashboardService(AccountService accounts, TransactionService transactions,
			TransactionRepository transactionRepository, CurrentUser currentUser) {
		this.accounts = accounts;
		this.transactions = transactions;
		this.transactionRepository = transactionRepository;
		this.currentUser = currentUser;
	}

	public DashboardResponse get(YearMonth month) {
		List<AccountResponse> activeAccounts = this.accounts.list(false);
		BigDecimal totalBalance = activeAccounts.stream()
			.map(AccountResponse::currentBalance)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		Map<CategoryType, BigDecimal> totals = this.transactionRepository
			.sumByType(this.currentUser.id(), month.atDay(1), month.plusMonths(1).atDay(1))
			.stream()
			.collect(Collectors.toMap(TransactionRepository.TypeTotal::getType,
					TransactionRepository.TypeTotal::getTotal));
		BigDecimal income = totals.getOrDefault(CategoryType.INCOME, BigDecimal.ZERO);
		BigDecimal expense = totals.getOrDefault(CategoryType.EXPENSE, BigDecimal.ZERO);

		List<TransactionResponse> recent = this.transactions
			.list(new TransactionFilter(null, null, null, null), 0, RECENT_TRANSACTIONS)
			.content();

		return new DashboardResponse(month, totalBalance, income, expense, income.subtract(expense), activeAccounts,
				recent);
	}

}
