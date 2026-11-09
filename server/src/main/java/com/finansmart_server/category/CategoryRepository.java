package com.finansmart_server.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	@Query("""
			select c from Category c
			where c.userId = :userId or c.userId is null
			order by lower(c.name)
			""")
	List<Category> findVisible(@Param("userId") String userId);

	@Query("""
			select c from Category c
			where (c.userId = :userId or c.userId is null) and c.type = :type
			order by lower(c.name)
			""")
	List<Category> findVisibleByType(@Param("userId") String userId, @Param("type") CategoryType type);

	@Query("""
			select c from Category c
			where c.id = :id and (c.userId = :userId or c.userId is null)
			""")
	Optional<Category> findVisibleById(@Param("id") UUID id, @Param("userId") String userId);

	@Query("""
			select c from Category c
			where (c.userId = :userId or c.userId is null)
			  and c.type = :type and lower(c.name) = lower(:name)
			""")
	List<Category> findVisibleByNameAndType(@Param("userId") String userId, @Param("name") String name,
			@Param("type") CategoryType type);

}
