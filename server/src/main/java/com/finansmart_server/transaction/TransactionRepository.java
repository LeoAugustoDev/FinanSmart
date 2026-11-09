package com.finansmart_server.transaction;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

	@EntityGraph(attributePaths = { "account", "category" })
	Optional<Transaction> findByIdAndUserId(UUID id, String userId);

	@Override
	@EntityGraph(attributePaths = { "account", "category" })
	Page<Transaction> findAll(Specification<Transaction> specification, Pageable pageable);

}
