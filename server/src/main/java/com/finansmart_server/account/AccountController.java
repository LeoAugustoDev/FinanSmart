package com.finansmart_server.account;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.finansmart_server.account.dto.AccountResponse;
import com.finansmart_server.account.dto.CreateAccountRequest;
import com.finansmart_server.account.dto.UpdateAccountRequest;

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController {

	private final AccountService accounts;

	AccountController(AccountService accounts) {
		this.accounts = accounts;
	}

	@PostMapping
	ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
		AccountResponse account = this.accounts.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(account.id())
			.toUri();
		return ResponseEntity.created(location).body(account);
	}

	@GetMapping
	List<AccountResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
		return this.accounts.list(includeArchived);
	}

	@GetMapping("/{id}")
	AccountResponse get(@PathVariable UUID id) {
		return this.accounts.get(id);
	}

	@PutMapping("/{id}")
	AccountResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAccountRequest request) {
		return this.accounts.update(id, request);
	}

	@PatchMapping("/{id}/archive")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void archive(@PathVariable UUID id) {
		this.accounts.archive(id);
	}

	@PatchMapping("/{id}/unarchive")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void unarchive(@PathVariable UUID id) {
		this.accounts.unarchive(id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID id) {
		this.accounts.delete(id);
	}

}
