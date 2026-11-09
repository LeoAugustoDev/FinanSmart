package com.finansmart_server.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransactionRequest(
		@NotNull(message = "Informe a conta") UUID accountId,
		@NotNull(message = "Informe a categoria") UUID categoryId,
		@NotNull(message = "Informe o valor") @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero") @Digits(integer = 17, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais") BigDecimal amount,
		@NotNull(message = "Informe a data") LocalDate occurredOn,
		@Size(max = 255, message = "A descrição deve ter até 255 caracteres") String description) {

	public TransactionRequest {
		description = ((description != null) && !description.isBlank()) ? description.strip() : null;
	}

}
