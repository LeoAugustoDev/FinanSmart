package com.finansmart_server.budget;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finansmart_server.budget.dto.BudgetResponse;
import com.finansmart_server.budget.dto.CreateBudgetRequest;
import com.finansmart_server.budget.dto.UpdateBudgetRequest;
import com.finansmart_server.budget.exception.BudgetNotFoundException;
import com.finansmart_server.budget.exception.DuplicateBudgetException;
import com.finansmart_server.budget.exception.InvalidBudgetCategoryException;
import com.finansmart_server.category.Category;
import com.finansmart_server.category.CategoryRepository;
import com.finansmart_server.category.CategoryType;
import com.finansmart_server.shared.CurrentUser;
import com.finansmart_server.transaction.TransactionRepository;

@Service
@Transactional
public class BudgetService {

	private final BudgetRepository budgets;

	private final CategoryRepository categories;

	private final TransactionRepository transactions;

	private final CurrentUser currentUser;

	BudgetService(BudgetRepository budgets, CategoryRepository categories, TransactionRepository transactions,
			CurrentUser currentUser) {
		this.budgets = budgets;
		this.categories = categories;
		this.transactions = transactions;
		this.currentUser = currentUser;
	}

	public BudgetResponse create(CreateBudgetRequest request) {
		String userId = this.currentUser.id();
		Category category = this.categories.findVisibleById(request.categoryId(), userId)
			.orElseThrow(() -> new InvalidBudgetCategoryException("Categoria não encontrada"));
		if (category.getType() != CategoryType.EXPENSE) {
			throw new InvalidBudgetCategoryException("Orçamentos só podem ser criados para categorias de despesa");
		}
		if (this.budgets.existsByUserIdAndCategoryIdAndReferenceMonth(userId, category.getId(),
				request.month().atDay(1))) {
			throw new DuplicateBudgetException();
		}
		Budget budget = new Budget(userId, category, request.month(), request.limit());
		try {
			return withSpent(this.budgets.saveAndFlush(budget));
		}
		catch (DataIntegrityViolationException ex) {
			throw new DuplicateBudgetException();
		}
	}

	@Transactional(readOnly = true)
	public List<BudgetResponse> list(YearMonth month) {
		String userId = this.currentUser.id();
		Map<UUID, BigDecimal> spentByCategory = this.transactions
			.sumExpensesByCategory(userId, month.atDay(1), month.plusMonths(1).atDay(1))
			.stream()
			.collect(Collectors.toMap(TransactionRepository.CategoryTotal::getCategoryId,
					TransactionRepository.CategoryTotal::getTotal));
		return this.budgets.findAllOfMonth(userId, month.atDay(1))
			.stream()
			.map((budget) -> BudgetResponse.from(budget,
					spentByCategory.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO)))
			.toList();
	}

	@Transactional(readOnly = true)
	public BudgetResponse get(UUID id) {
		return withSpent(findOwned(id));
	}

	public BudgetResponse update(UUID id, UpdateBudgetRequest request) {
		Budget budget = findOwned(id);
		budget.changeLimit(request.limit());
		return withSpent(this.budgets.saveAndFlush(budget));
	}

	public void delete(UUID id) {
		this.budgets.delete(findOwned(id));
	}

	private Budget findOwned(UUID id) {
		return this.budgets.findByIdAndUserId(id, this.currentUser.id()).orElseThrow(BudgetNotFoundException::new);
	}

	private BudgetResponse withSpent(Budget budget) {
		YearMonth month = budget.getMonth();
		BigDecimal spent = this.transactions.sumOfCategory(budget.getUserId(), budget.getCategory().getId(),
				month.atDay(1), month.plusMonths(1).atDay(1));
		return BudgetResponse.from(budget, spent);
	}

}
