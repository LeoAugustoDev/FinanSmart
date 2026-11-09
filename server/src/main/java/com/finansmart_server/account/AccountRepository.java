package com.finansmart_server.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {

	Optional<Account> findByIdAndUserId(UUID id, String userId);

	List<Account> findByUserIdOrderByNameAsc(String userId);

	List<Account> findByUserIdAndArchivedFalseOrderByNameAsc(String userId);

	@Query("""
			select a from Account a
			where a.userId = :userId and a.archived = false and lower(a.name) = lower(:name)
			""")
	Optional<Account> findActiveByName(@Param("userId") String userId, @Param("name") String name);

}
