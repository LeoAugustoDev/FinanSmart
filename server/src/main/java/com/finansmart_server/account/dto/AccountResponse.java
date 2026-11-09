package com.finansmart_server.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.finansmart_server.account.Account;
import com.finansmart_server.account.AccountType;

public record AccountResponse(UUID id, String name, AccountType type, BigDecimal initialBalance,
		BigDecimal currentBalance, String currency, boolean archived, Instant createdAt, Instant updatedAt) {

	/**
	 * @param transactionsNet soma das receitas menos as despesas da conta
	 */
	public static AccountResponse from(Account account, BigDecimal transactionsNet) {
		return new AccountResponse(account.getId(), account.getName(), account.getType(), account.getInitialBalance(),
				account.getInitialBalance().add(transactionsNet), account.getCurrency(), account.isArchived(),
				account.getCreatedAt(), account.getUpdatedAt());
	}

}
