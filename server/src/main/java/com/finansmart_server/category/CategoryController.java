package com.finansmart_server.category;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.finansmart_server.category.dto.CategoryResponse;
import com.finansmart_server.category.dto.CreateCategoryRequest;
import com.finansmart_server.category.dto.UpdateCategoryRequest;

@RestController
@RequestMapping("/api/v1/categories")
class CategoryController {

	private final CategoryService categories;

	CategoryController(CategoryService categories) {
		this.categories = categories;
	}

	@PostMapping
	ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
		CategoryResponse category = this.categories.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(category.id())
			.toUri();
		return ResponseEntity.created(location).body(category);
	}

	@GetMapping
	List<CategoryResponse> list(@RequestParam(required = false) CategoryType type) {
		return this.categories.list(type);
	}

	@GetMapping("/{id}")
	CategoryResponse get(@PathVariable UUID id) {
		return this.categories.get(id);
	}

	@PutMapping("/{id}")
	CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
		return this.categories.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID id) {
		this.categories.delete(id);
	}

}
