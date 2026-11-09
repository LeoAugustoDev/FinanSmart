package com.finansmart_server.leak;

import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finansmart_server.leak.LeakDetector.Expense;
import com.finansmart_server.leak.dto.LeakResponse;
import com.finansmart_server.shared.CurrentUser;
import com.finansmart_server.transaction.TransactionRepository;

@Service
@Transactional(readOnly = true)
public class LeakService {

	private final TransactionRepository transactions;

	private final CurrentUser currentUser;

	LeakService(TransactionRepository transactions, CurrentUser currentUser) {
		this.transactions = transactions;
		this.currentUser = currentUser;
	}

	public LeakResponse detect(YearMonth month) {
		List<Expense> expenses = this.transactions
			.findExpenses(this.currentUser.id(), month.minusMonths(LeakDetector.HISTORY_MONTHS).atDay(1),
					month.plusMonths(1).atDay(1))
			.stream()
			.map((row) -> new Expense(row.getCategoryId(), row.getCategoryName(), row.getCategoryOwner() == null,
					row.getAmount(), row.getOccurredOn(), row.getDescription()))
			.toList();
		return LeakDetector.detect(month, expenses);
	}

}
