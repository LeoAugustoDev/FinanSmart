package com.finansmart_server.budget;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

	@EntityGraph(attributePaths = "category")
	Optional<Budget> findByIdAndUserId(UUID id, String userId);

	@Query("""
			select b from Budget b join fetch b.category c
			where b.userId = :userId and b.referenceMonth = :month
			order by lower(c.name)
			""")
	List<Budget> findAllOfMonth(@Param("userId") String userId, @Param("month") LocalDate month);

	boolean existsByUserIdAndCategoryIdAndReferenceMonth(String userId, UUID categoryId, LocalDate referenceMonth);

}
