package com.finansmart_server.transaction;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.finansmart_server.shared.PageResponse;
import com.finansmart_server.transaction.dto.TransactionRequest;
import com.finansmart_server.transaction.dto.TransactionResponse;

@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController {

	private final TransactionService transactions;

	TransactionController(TransactionService transactions) {
		this.transactions = transactions;
	}

	@PostMapping
	ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
		TransactionResponse transaction = this.transactions.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(transaction.id())
			.toUri();
		return ResponseEntity.created(location).body(transaction);
	}

	@GetMapping
	PageResponse<TransactionResponse> list(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) UUID accountId, @RequestParam(required = false) UUID categoryId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return this.transactions.list(new TransactionFilter(from, to, accountId, categoryId), page, size);
	}

	@GetMapping("/{id}")
	TransactionResponse get(@PathVariable UUID id) {
		return this.transactions.get(id);
	}

	@PutMapping("/{id}")
	TransactionResponse update(@PathVariable UUID id, @Valid @RequestBody TransactionRequest request) {
		return this.transactions.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID id) {
		this.transactions.delete(id);
	}

}
