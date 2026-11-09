package com.finansmart_server.budget.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record CreateBudgetRequest(
		@NotNull(message = "Informe a categoria") UUID categoryId,
		@NotNull(message = "Informe o mês") YearMonth month,
		@NotNull(message = "Informe o limite") @DecimalMin(value = "0.01", message = "O limite deve ser maior que zero") @Digits(integer = 17, fraction = 2, message = "O limite deve ter no máximo 2 casas decimais") BigDecimal limit) {
}
