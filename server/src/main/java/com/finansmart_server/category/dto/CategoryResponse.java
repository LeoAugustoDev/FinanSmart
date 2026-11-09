package com.finansmart_server.category.dto;

import java.time.Instant;
import java.util.UUID;

import com.finansmart_server.category.Category;
import com.finansmart_server.category.CategoryType;

public record CategoryResponse(UUID id, String name, CategoryType type, boolean system, Instant createdAt,
		Instant updatedAt) {

	public static CategoryResponse from(Category category) {
		return new CategoryResponse(category.getId(), category.getName(), category.getType(), category.isSystem(),
				category.getCreatedAt(), category.getUpdatedAt());
	}

}
