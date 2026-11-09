package com.finansmart_server.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.finansmart_server.category.CategoryType;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

	@Query("""
			select t.account.id as accountId,
			       sum(case when t.category.type = com.finansmart_server.category.CategoryType.INCOME
			                then t.amount else -t.amount end) as net
			from Transaction t
			where t.userId = :userId
			group by t.account.id
			""")
	List<AccountNet> sumNetByAccount(@Param("userId") String userId);

	@Query("""
			select coalesce(sum(case when t.category.type = com.finansmart_server.category.CategoryType.INCOME
			                         then t.amount else -t.amount end), 0)
			from Transaction t
			where t.account.id = :accountId
			""")
	BigDecimal sumNetOfAccount(@Param("accountId") UUID accountId);

	/**
	 * @param from primeiro dia do período (inclusive)
	 * @param to primeiro dia depois do período (exclusive)
	 */
	@Query("""
			select t.category.type as type, sum(t.amount) as total
			from Transaction t
			where t.userId = :userId and t.occurredOn >= :from and t.occurredOn < :to
			group by t.category.type
			""")
	List<TypeTotal> sumByType(@Param("userId") String userId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	@Query("""
			select t.category.id as categoryId, sum(t.amount) as total
			from Transaction t
			where t.userId = :userId and t.occurredOn >= :from and t.occurredOn < :to
			  and t.category.type = com.finansmart_server.category.CategoryType.EXPENSE
			group by t.category.id
			""")
	List<CategoryTotal> sumExpensesByCategory(@Param("userId") String userId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	@Query("""
			select coalesce(sum(t.amount), 0)
			from Transaction t
			where t.userId = :userId and t.category.id = :categoryId
			  and t.occurredOn >= :from and t.occurredOn < :to
			""")
	BigDecimal sumOfCategory(@Param("userId") String userId, @Param("categoryId") UUID categoryId,
			@Param("from") LocalDate from, @Param("to") LocalDate to);

	@EntityGraph(attributePaths = { "account", "category" })
	Optional<Transaction> findByIdAndUserId(UUID id, String userId);

	@Override
	@EntityGraph(attributePaths = { "account", "category" })
	Page<Transaction> findAll(Specification<Transaction> specification, Pageable pageable);

	interface AccountNet {

		UUID getAccountId();

		BigDecimal getNet();

	}

	interface TypeTotal {

		CategoryType getType();

		BigDecimal getTotal();

	}

	interface CategoryTotal {

		UUID getCategoryId();

		BigDecimal getTotal();

	}

}
