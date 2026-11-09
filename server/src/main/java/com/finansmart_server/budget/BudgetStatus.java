package com.finansmart_server.budget;

public enum BudgetStatus {

	/** Consumo de até 80% do limite. */
	OK,

	/** Consumo acima de 80%, sem passar do limite. */
	WARNING,

	/** Consumo acima de 100% do limite. */
	EXCEEDED

}
