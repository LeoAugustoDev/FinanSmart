package com.finansmart_server.transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionais da listagem de lançamentos. */
public record TransactionFilter(LocalDate from, LocalDate to, UUID accountId, UUID categoryId) {

	Specification<Transaction> toSpecification(String userId) {
		List<Specification<Transaction>> conditions = new ArrayList<>();
		conditions.add((root, query, builder) -> builder.equal(root.get("userId"), userId));
		if (this.from != null) {
			conditions.add((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("occurredOn"), this.from));
		}
		if (this.to != null) {
			conditions.add((root, query, builder) -> builder.lessThanOrEqualTo(root.get("occurredOn"), this.to));
		}
		if (this.accountId != null) {
			conditions.add((root, query, builder) -> builder.equal(root.get("account").get("id"), this.accountId));
		}
		if (this.categoryId != null) {
			conditions.add((root, query, builder) -> builder.equal(root.get("category").get("id"), this.categoryId));
		}
		return Specification.allOf(conditions);
	}

}
