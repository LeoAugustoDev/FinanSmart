package com.finansmart_server.account.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.finansmart_server.account.AccountType;

public record CreateAccountRequest(
		@NotBlank(message = "Informe o nome da conta") @Size(max = 60, message = "O nome deve ter até 60 caracteres") String name,
		@NotNull(message = "Informe o tipo da conta") AccountType type,
		@NotNull(message = "Informe o saldo inicial") @Digits(integer = 17, fraction = 2, message = "O saldo inicial deve ter no máximo 2 casas decimais") BigDecimal initialBalance) {

	public CreateAccountRequest {
		name = (name != null) ? name.strip() : null;
	}

}
