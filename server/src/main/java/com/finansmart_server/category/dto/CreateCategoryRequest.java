package com.finansmart_server.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.finansmart_server.category.CategoryType;

public record CreateCategoryRequest(
		@NotBlank(message = "Informe o nome da categoria") @Size(max = 60, message = "O nome deve ter até 60 caracteres") String name,
		@NotNull(message = "Informe o tipo da categoria") CategoryType type) {

	public CreateCategoryRequest {
		name = (name != null) ? name.strip() : null;
	}

}
