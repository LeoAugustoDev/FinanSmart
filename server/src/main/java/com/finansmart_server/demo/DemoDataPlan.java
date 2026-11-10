package com.finansmart_server.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.finansmart_server.account.AccountType;
import com.finansmart_server.category.CategoryType;

/**
 * Roteiro dos dados de demonstração: seis meses de vida financeira de uma pessoa, terminando no
 * mês de {@code today}. Não acessa banco, para que o conteúdo possa ser testado sozinho.
 *
 * <p>
 * Os valores são fixos (sem sorteio) e pensados para os detectores de vazamento: as cobranças que
 * devem aparecer como recorrentes repetem descrição e valor, e os gastos do dia a dia mudam de
 * descrição ou de valor de um mês para o outro para não parecerem assinatura.
 */
final class DemoDataPlan {

	static final int MONTHS = 6;

	static final String CHECKING = "Conta corrente";

	static final String SAVINGS = "Poupança";

	static final String WALLET = "Carteira";

	static final String PETS = "Pets";

	private static final String[] MARKETS = { "Supermercado Bom Preço", "Atacado da Família", "Mercado do Bairro",
			"Hortifruti Sabor da Terra" };

	private static final String[] MARKET_BASE = { "420.00", "310.00", "240.00", "180.00" };

	private static final String[] MARKET_ADJUST = { "0.00", "12.35", "-8.10", "5.75", "-3.40", "9.20" };

	private static final String[] MEALS = { "Delivery de comida", "Restaurante Sabor da Casa", "Padaria Estrela",
			"Pizzaria Bella Massa" };

	private static final String[] MEAL_BASE = { "62.00", "95.00", "38.00", "78.00" };

	private static final String[] MEAL_ADJUST = { "0.00", "4.50", "-3.20", "6.90", "-1.80", "2.40" };

	private static final int[] WEEKLY_DAYS = { 3, 10, 17, 24 };

	private static final String[] LEISURE = { "Cinema", "Bar com amigos", "Teatro", "Parque de diversões" };

	private static final String[] SHOPS = { "Loja de eletrônicos", "Livraria Página Um", "Loja de roupas",
			"Loja de departamentos", "Artigos esportivos", "Loja de eletrônicos" };

	private static final String[] PET_SHOPS = { "Ração", "Petshop Amigo Fiel", "Veterinário" };

	private final LocalDate today;

	private final List<AccountPlan> accounts = new ArrayList<>();

	private final List<Entry> entries = new ArrayList<>();

	private final List<BudgetPlan> budgets = new ArrayList<>();

	private DemoDataPlan(LocalDate today) {
		this.today = today;
	}

	static DemoDataPlan build(LocalDate today) {
		DemoDataPlan plan = new DemoDataPlan(today);
		plan.accounts.add(new AccountPlan(CHECKING, AccountType.CHECKING, new BigDecimal("1200.00")));
		plan.accounts.add(new AccountPlan(SAVINGS, AccountType.SAVINGS, new BigDecimal("8000.00")));
		plan.accounts.add(new AccountPlan(WALLET, AccountType.WALLET, new BigDecimal("500.00")));
		YearMonth current = YearMonth.from(today);
		for (int index = 0; index < MONTHS; index++) {
			plan.month(index, current.minusMonths(MONTHS - 1 - index));
		}
		for (YearMonth month : List.of(current.minusMonths(1), current)) {
			plan.budgets.add(new BudgetPlan("Alimentação", month, new BigDecimal("1500.00")));
			plan.budgets.add(new BudgetPlan("Transporte", month, new BigDecimal("500.00")));
			plan.budgets.add(new BudgetPlan("Lazer", month, new BigDecimal("400.00")));
			plan.budgets.add(new BudgetPlan("Compras", month, new BigDecimal("300.00")));
		}
		return plan;
	}

	List<AccountPlan> accounts() {
		return List.copyOf(this.accounts);
	}

	List<Entry> entries() {
		return List.copyOf(this.entries);
	}

	List<BudgetPlan> budgets() {
		return List.copyOf(this.budgets);
	}

	/** @param index posição do mês no roteiro: 0 é o mais antigo e {@code MONTHS - 1} o atual */
	private void month(int index, YearMonth month) {
		boolean current = index == MONTHS - 1;

		income(month, 1, CHECKING, "Salário", "6500.00", "Salário");
		income(month, 3, SAVINGS, "Rendimentos", pick(index, "48.20", "49.10", "50.35", "51.02", "52.40", "53.15"),
				"Rendimento da poupança");
		if (index % 2 == 1) {
			income(month, 12, CHECKING, "Freelance", pick(index / 2, "1200.00", "850.00", "1500.00"),
					"Projeto freelance");
		}

		// Contas fixas: repetem todo mês, mas não são vazamento.
		expense(month, 5, CHECKING, "Moradia", "1800.00", "Aluguel");
		expense(month, 5, CHECKING, "Moradia", "420.00", "Condomínio");
		expense(month, 8, CHECKING, "Moradia", "109.90", "Internet");
		expense(month, 10, CHECKING, "Moradia",
				pick(index, "162.30", "148.90", "171.45", "155.20", "168.75", "159.60"), "Conta de luz");
		expense(month, 7, CHECKING, "Saúde", "389.00", "Plano de saúde");
		expense(month, 6, CHECKING, "Educação", "320.00", "Curso de inglês");

		// Vazamentos: assinaturas, uma cobrança recorrente fora da categoria e tarifas.
		expense(month, current ? 1 : 2, CHECKING, "Assinaturas", "44.90", "Streaming de filmes");
		expense(month, current ? 1 : 3, CHECKING, "Assinaturas", "21.90", "Streaming de música");
		expense(month, current ? 1 : 3, CHECKING, "Assinaturas", "14.90", "Armazenamento em nuvem");
		expense(month, current ? 1 : 4, CHECKING, "Compras", "89.90", "Clube de vinhos");
		expense(month, 1, CHECKING, "Tarifas e juros", "29.90", "Tarifa de manutenção de conta");
		if (index == 3) {
			expense(month, 9, CHECKING, "Tarifas e juros", "54.10", "Juros do cartão de crédito");
		}
		if (index == 4) {
			expense(month, 11, CHECKING, "Tarifas e juros", "35.00", "Anuidade do cartão");
		}
		if (current) {
			expense(month, 1, CHECKING, "Tarifas e juros", "86.40", "Juros do cartão de crédito");
		}

		// Dia a dia: cada loja troca de semana (e de valor) a cada mês.
		for (int week = 0; week < WEEKLY_DAYS.length; week++) {
			int rotated = (week + index) % WEEKLY_DAYS.length;
			expense(month, WEEKLY_DAYS[week], CHECKING, "Alimentação",
					sum(MARKET_BASE[week], MARKET_ADJUST[index]), MARKETS[rotated]);
			expense(month, WEEKLY_DAYS[week] + 3, CHECKING, "Alimentação", sum(MEAL_BASE[week], MEAL_ADJUST[index]),
					MEALS[rotated]);
		}
		expense(month, 8, CHECKING, "Transporte", pick(index, "180.00", "240.00", "150.00", "260.00", "200.00", "230.00"),
				"Posto Avenida");
		expense(month, 22, CHECKING, "Transporte",
				pick(index, "230.00", "150.00", "260.00", "180.00", "240.00", "200.00"), "Posto Central");
		expense(month, 14, CHECKING, "Transporte", pick(index, "32.40", "27.90", "41.20", "29.50", "36.80", "24.60"),
				"Corrida por aplicativo");
		expense(month, 15, WALLET, "Saúde", pick(index, "64.90", "38.50", "72.30", "45.00", "58.20", "41.70"),
				"Farmácia");
		expense(month, 18, CHECKING, "Compras", pick(index, "189.90", "245.00", "132.50", "210.00", "174.90", "185.00"),
				SHOPS[index]);
		expense(month, 19, CHECKING, PETS, pick(index, "145.00", "160.00", "138.00", "152.00", "149.00", "158.00"),
				PET_SHOPS[index % PET_SHOPS.length]);
		expense(month, 9, CHECKING, "Lazer", pick(index, "85.00", "120.00", "95.00", "110.00", "90.00", "105.00"),
				LEISURE[index % LEISURE.length]);
		expense(month, 21, CHECKING, "Lazer", pick(index, "140.00", "110.00", "150.00", "125.00", "160.00", "130.00"),
				LEISURE[(index + 2) % LEISURE.length]);
		if (current) {
			// Gasto fora da curva: põe o Lazer acima da média e estoura o orçamento do mês.
			expense(month, 1, CHECKING, "Lazer", "480.00", "Ingressos de festival");
		}
	}

	private void income(YearMonth month, int day, String account, String category, String amount, String description) {
		add(month, day, account, category, CategoryType.INCOME, amount, description);
	}

	private void expense(YearMonth month, int day, String account, String category, String amount,
			String description) {
		add(month, day, account, category, CategoryType.EXPENSE, amount, description);
	}

	/** Lançamentos com data depois de hoje ficam de fora: a demonstração não mostra o futuro. */
	private void add(YearMonth month, int day, String account, String category, CategoryType type, String amount,
			String description) {
		LocalDate date = month.atDay(day);
		if (!date.isAfter(this.today)) {
			this.entries.add(new Entry(account, category, type, date, new BigDecimal(amount), description));
		}
	}

	private static String pick(int index, String... values) {
		return values[index];
	}

	private static String sum(String base, String adjust) {
		return new BigDecimal(base).add(new BigDecimal(adjust)).toPlainString();
	}

	record AccountPlan(String name, AccountType type, BigDecimal initialBalance) {
	}

	record Entry(String account, String category, CategoryType type, LocalDate date, BigDecimal amount,
			String description) {
	}

	record BudgetPlan(String category, YearMonth month, BigDecimal limit) {
	}

}
