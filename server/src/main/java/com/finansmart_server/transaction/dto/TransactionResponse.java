package com.finansmart_server.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.finansmart_server.category.CategoryType;
import com.finansmart_server.transaction.Transaction;

public record TransactionResponse(UUID id, UUID accountId, String accountName, UUID categoryId, String categoryName,
		CategoryType type, BigDecimal amount, LocalDate occurredOn, String description, Instant createdAt,
		Instant updatedAt) {

	public static TransactionResponse from(Transaction transaction) {
		return new TransactionResponse(transaction.getId(), transaction.getAccount().getId(),
				transaction.getAccount().getName(), transaction.getCategory().getId(),
				transaction.getCategory().getName(), transaction.getCategory().getType(), transaction.getAmount(),
				transaction.getOccurredOn(), transaction.getDescription(), transaction.getCreatedAt(),
				transaction.getUpdatedAt());
	}

}
