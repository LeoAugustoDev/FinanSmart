package com.finansmart_server.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.finansmart_server.category.dto.CategoryResponse;
import com.finansmart_server.category.dto.CreateCategoryRequest;
import com.finansmart_server.category.dto.UpdateCategoryRequest;
import com.finansmart_server.category.exception.CategoryNotFoundException;
import com.finansmart_server.category.exception.DuplicateCategoryNameException;
import com.finansmart_server.category.exception.SystemCategoryModificationException;
import com.finansmart_server.shared.CurrentUser;

class CategoryServiceTests {

	private static final String USER = "user-1";

	private final CategoryRepository repository = mock(CategoryRepository.class);

	private final CurrentUser currentUser = mock(CurrentUser.class);

	private final CategoryService service = new CategoryService(this.repository, this.currentUser);

	@BeforeEach
	void setUp() {
		given(this.currentUser.id()).willReturn(USER);
		given(this.repository.saveAndFlush(any())).willAnswer((invocation) -> invocation.getArgument(0));
	}

	@Test
	void createsPersonalCategory() {
		given(this.repository.findVisibleByNameAndType(USER, "Pets", CategoryType.EXPENSE)).willReturn(List.of());

		CategoryResponse response = this.service.create(new CreateCategoryRequest("Pets", CategoryType.EXPENSE));

		assertThat(response.name()).isEqualTo("Pets");
		assertThat(response.type()).isEqualTo(CategoryType.EXPENSE);
		assertThat(response.system()).isFalse();
	}

	@Test
	void rejectsNameAlreadyUsedByOwnCategory() {
		given(this.repository.findVisibleByNameAndType(USER, "Pets", CategoryType.EXPENSE))
			.willReturn(List.of(category(USER, "Pets")));

		assertThatThrownBy(() -> this.service.create(new CreateCategoryRequest("Pets", CategoryType.EXPENSE)))
			.isInstanceOf(DuplicateCategoryNameException.class);
		verify(this.repository, never()).saveAndFlush(any());
	}

	@Test
	void rejectsNameAlreadyUsedByDefaultCategory() {
		given(this.repository.findVisibleByNameAndType(USER, "Lazer", CategoryType.EXPENSE))
			.willReturn(List.of(category(null, "Lazer")));

		assertThatThrownBy(() -> this.service.create(new CreateCategoryRequest("Lazer", CategoryType.EXPENSE)))
			.isInstanceOf(DuplicateCategoryNameException.class);
	}

	@Test
	void translatesConcurrentDuplicateFromTheDatabase() {
		given(this.repository.findVisibleByNameAndType(USER, "Pets", CategoryType.EXPENSE)).willReturn(List.of());
		given(this.repository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("duplicado"));

		assertThatThrownBy(() -> this.service.create(new CreateCategoryRequest("Pets", CategoryType.EXPENSE)))
			.isInstanceOf(DuplicateCategoryNameException.class);
	}

	@Test
	void renamingKeepingItsOwnNameIsAllowed() {
		Category category = category(USER, "Pets");
		given(this.repository.findVisibleById(category.getId(), USER)).willReturn(Optional.of(category));
		given(this.repository.findVisibleByNameAndType(USER, "PETS", CategoryType.EXPENSE))
			.willReturn(List.of(category));

		assertThat(this.service.update(category.getId(), new UpdateCategoryRequest("PETS")).name()).isEqualTo("PETS");
	}

	@Test
	void defaultCategoryCannotBeRenamed() {
		Category category = category(null, "Lazer");
		given(this.repository.findVisibleById(category.getId(), USER)).willReturn(Optional.of(category));

		assertThatThrownBy(() -> this.service.update(category.getId(), new UpdateCategoryRequest("Diversão")))
			.isInstanceOf(SystemCategoryModificationException.class);
		assertThat(category.getName()).isEqualTo("Lazer");
	}

	@Test
	void defaultCategoryCannotBeDeleted() {
		Category category = category(null, "Lazer");
		given(this.repository.findVisibleById(category.getId(), USER)).willReturn(Optional.of(category));

		assertThatThrownBy(() -> this.service.delete(category.getId()))
			.isInstanceOf(SystemCategoryModificationException.class);
		verify(this.repository, never()).delete(any());
	}

	@Test
	void categoryOfAnotherUserIsNotFound() {
		UUID id = UUID.randomUUID();
		given(this.repository.findVisibleById(id, USER)).willReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.get(id)).isInstanceOf(CategoryNotFoundException.class);
	}

	private static Category category(String userId, String name) {
		Category category = new Category(userId, name, CategoryType.EXPENSE);
		ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
		return category;
	}

}
