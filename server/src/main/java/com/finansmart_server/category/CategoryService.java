package com.finansmart_server.category;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finansmart_server.category.dto.CategoryResponse;
import com.finansmart_server.category.dto.CreateCategoryRequest;
import com.finansmart_server.category.dto.UpdateCategoryRequest;
import com.finansmart_server.category.exception.CategoryNotFoundException;
import com.finansmart_server.category.exception.DuplicateCategoryNameException;
import com.finansmart_server.category.exception.SystemCategoryModificationException;
import com.finansmart_server.shared.CurrentUser;

@Service
@Transactional
public class CategoryService {

	private final CategoryRepository categories;

	private final CurrentUser currentUser;

	CategoryService(CategoryRepository categories, CurrentUser currentUser) {
		this.categories = categories;
		this.currentUser = currentUser;
	}

	public CategoryResponse create(CreateCategoryRequest request) {
		String userId = this.currentUser.id();
		assertNameAvailable(userId, request.name(), request.type(), null);
		Category category = new Category(userId, request.name(), request.type());
		return CategoryResponse.from(saveEnforcingUniqueName(category));
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> list(CategoryType type) {
		String userId = this.currentUser.id();
		List<Category> result = (type != null) ? this.categories.findVisibleByType(userId, type)
				: this.categories.findVisible(userId);
		return result.stream().map(CategoryResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse get(UUID id) {
		return CategoryResponse.from(findVisible(id));
	}

	public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
		Category category = findOwned(id);
		assertNameAvailable(category.getUserId(), request.name(), category.getType(), category.getId());
		category.rename(request.name());
		return CategoryResponse.from(saveEnforcingUniqueName(category));
	}

	public void delete(UUID id) {
		this.categories.delete(findOwned(id));
	}

	private Category findVisible(UUID id) {
		return this.categories.findVisibleById(id, this.currentUser.id()).orElseThrow(CategoryNotFoundException::new);
	}

	private Category findOwned(UUID id) {
		Category category = findVisible(id);
		if (category.isSystem()) {
			throw new SystemCategoryModificationException();
		}
		return category;
	}

	private void assertNameAvailable(String userId, String name, CategoryType type, UUID ignoredCategoryId) {
		boolean taken = this.categories.findVisibleByNameAndType(userId, name, type)
			.stream()
			.anyMatch((existing) -> !existing.getId().equals(ignoredCategoryId));
		if (taken) {
			throw new DuplicateCategoryNameException();
		}
	}

	/** O índice único do banco cobre requisições simultâneas que passam pela verificação prévia. */
	private Category saveEnforcingUniqueName(Category category) {
		try {
			return this.categories.saveAndFlush(category);
		}
		catch (DataIntegrityViolationException ex) {
			throw new DuplicateCategoryNameException();
		}
	}

}
