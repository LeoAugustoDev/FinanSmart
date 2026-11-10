package com.finansmart_server.demo;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.finansmart_server.account.Account;
import com.finansmart_server.account.AccountRepository;
import com.finansmart_server.budget.Budget;
import com.finansmart_server.budget.BudgetRepository;
import com.finansmart_server.category.Category;
import com.finansmart_server.category.CategoryRepository;
import com.finansmart_server.category.CategoryType;
import com.finansmart_server.demo.DemoDataPlan.AccountPlan;
import com.finansmart_server.demo.DemoDataPlan.BudgetPlan;
import com.finansmart_server.demo.DemoDataPlan.Entry;
import com.finansmart_server.transaction.Transaction;
import com.finansmart_server.transaction.TransactionRepository;

/**
 * Grava os dados de demonstração para o usuário demo do Keycloak (id fixo no realm importado).
 */
@Service
@Profile("demo")
public class DemoDataSeeder {

	private final AccountRepository accounts;

	private final CategoryRepository categories;

	private final TransactionRepository transactions;

	private final BudgetRepository budgets;

	private final String userId;

	private final JdbcTemplate jdbc;

	DemoDataSeeder(AccountRepository accounts, CategoryRepository categories, TransactionRepository transactions,
			BudgetRepository budgets, JdbcTemplate jdbc, @Value("${app.demo.user-id}") String userId) {
		this.accounts = accounts;
		this.categories = categories;
		this.transactions = transactions;
		this.budgets = budgets;
		this.userId = userId;
		this.jdbc = jdbc;
	}

	/**
	 * Carga única e transacional. Reinícios preservam os dados e as edições dos visitantes.
	 * @return quantidade de lançamentos criados
	 */
	@Transactional
	public int seed(LocalDate today) {
		int claimed = this.jdbc.update("""
				insert into demo_seed_runs (user_id, seeded_on) values (?, ?)
				on conflict (user_id) do nothing
				""", this.userId, today);
		if (claimed == 0) {
			return 0;
		}
		if (!this.accounts.findByUserIdOrderByNameAsc(this.userId).isEmpty()
				|| this.categories.findVisible(this.userId).stream().anyMatch(c -> !c.isSystem())) {
			throw new IllegalStateException("Usuário demo já possui dados; use uma base demo vazia.");
		}

		DemoDataPlan plan = DemoDataPlan.build(today);
		this.categories.save(new Category(this.userId, DemoDataPlan.PETS, CategoryType.EXPENSE));
		Map<String, Account> accountsByName = new HashMap<>();
		for (AccountPlan account : plan.accounts()) {
			accountsByName.put(account.name(),
					this.accounts.save(new Account(this.userId, account.name(), account.type(), account.initialBalance())));
		}
		Map<String, Category> categoriesByKey = new HashMap<>();
		for (Entry entry : plan.entries()) {
			this.transactions.save(new Transaction(this.userId, accountsByName.get(entry.account()),
					category(categoriesByKey, entry.category(), entry.type()), entry.amount(), entry.date(),
					entry.description()));
		}
		for (BudgetPlan budget : plan.budgets()) {
			this.budgets.save(new Budget(this.userId,
					category(categoriesByKey, budget.category(), CategoryType.EXPENSE), budget.month(), budget.limit()));
		}
		return plan.entries().size();
	}

	private Category category(Map<String, Category> cache, String name, CategoryType type) {
		return cache.computeIfAbsent(type + ":" + name,
				(key) -> this.categories.findVisibleByNameAndType(this.userId, name, type)
					.stream()
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Categoria dos dados de demonstração não existe: " + name)));
	}

}
