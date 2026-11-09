package com.finansmart_server.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
		@NotBlank(message = "Informe o nome da categoria") @Size(max = 60, message = "O nome deve ter até 60 caracteres") String name) {

	public UpdateCategoryRequest {
		name = (name != null) ? name.strip() : null;
	}

}
