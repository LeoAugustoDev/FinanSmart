package com.finansmart_server.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.finansmart_server.account.Account;
import com.finansmart_server.account.AccountType;

public record AccountResponse(UUID id, String name, AccountType type, BigDecimal initialBalance, String currency,
		boolean archived, Instant createdAt, Instant updatedAt) {

	public static AccountResponse from(Account account) {
		return new AccountResponse(account.getId(), account.getName(), account.getType(), account.getInitialBalance(),
				account.getCurrency(), account.isArchived(), account.getCreatedAt(), account.getUpdatedAt());
	}

}
