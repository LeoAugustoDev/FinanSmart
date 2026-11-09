package com.finansmart_server.budget;

import java.net.URI;
import java.time.YearMonth;
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

import com.finansmart_server.budget.dto.BudgetResponse;
import com.finansmart_server.budget.dto.CreateBudgetRequest;
import com.finansmart_server.budget.dto.UpdateBudgetRequest;

@RestController
@RequestMapping("/api/v1/budgets")
class BudgetController {

	private final BudgetService budgets;

	BudgetController(BudgetService budgets) {
		this.budgets = budgets;
	}

	@PostMapping
	ResponseEntity<BudgetResponse> create(@Valid @RequestBody CreateBudgetRequest request) {
		BudgetResponse budget = this.budgets.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(budget.id())
			.toUri();
		return ResponseEntity.created(location).body(budget);
	}

	@GetMapping
	List<BudgetResponse> list(@RequestParam YearMonth month) {
		return this.budgets.list(month);
	}

	@GetMapping("/{id}")
	BudgetResponse get(@PathVariable UUID id) {
		return this.budgets.get(id);
	}

	@PutMapping("/{id}")
	BudgetResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateBudgetRequest request) {
		return this.budgets.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID id) {
		this.budgets.delete(id);
	}

}
