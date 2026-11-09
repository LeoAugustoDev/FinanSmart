package com.finansmart_server.account;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finansmart_server.account.dto.AccountResponse;
import com.finansmart_server.account.dto.CreateAccountRequest;
import com.finansmart_server.account.dto.UpdateAccountRequest;
import com.finansmart_server.account.exception.AccountNotFoundException;
import com.finansmart_server.account.exception.DuplicateAccountNameException;
import com.finansmart_server.shared.CurrentUser;

@Service
@Transactional
public class AccountService {

	private final AccountRepository accounts;

	private final CurrentUser currentUser;

	AccountService(AccountRepository accounts, CurrentUser currentUser) {
		this.accounts = accounts;
		this.currentUser = currentUser;
	}

	public AccountResponse create(CreateAccountRequest request) {
		String userId = this.currentUser.id();
		assertNameAvailable(userId, request.name(), null);
		Account account = new Account(userId, request.name(), request.type(), request.initialBalance());
		return AccountResponse.from(saveEnforcingUniqueName(account));
	}

	@Transactional(readOnly = true)
	public List<AccountResponse> list(boolean includeArchived) {
		String userId = this.currentUser.id();
		List<Account> result = includeArchived ? this.accounts.findByUserIdOrderByNameAsc(userId)
				: this.accounts.findByUserIdAndArchivedFalseOrderByNameAsc(userId);
		return result.stream().map(AccountResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public AccountResponse get(UUID id) {
		return AccountResponse.from(findOwned(id));
	}

	public AccountResponse update(UUID id, UpdateAccountRequest request) {
		Account account = findOwned(id);
		if (!account.isArchived()) {
			assertNameAvailable(account.getUserId(), request.name(), account.getId());
		}
		account.update(request.name(), request.type(), request.initialBalance());
		return AccountResponse.from(saveEnforcingUniqueName(account));
	}

	public void archive(UUID id) {
		findOwned(id).archive();
	}

	public void unarchive(UUID id) {
		Account account = findOwned(id);
		if (account.isArchived()) {
			assertNameAvailable(account.getUserId(), account.getName(), account.getId());
			account.unarchive();
			saveEnforcingUniqueName(account);
		}
	}

	public void delete(UUID id) {
		this.accounts.delete(findOwned(id));
	}

	private Account findOwned(UUID id) {
		return this.accounts.findByIdAndUserId(id, this.currentUser.id()).orElseThrow(AccountNotFoundException::new);
	}

	private void assertNameAvailable(String userId, String name, UUID ignoredAccountId) {
		boolean taken = this.accounts.findActiveByName(userId, name)
			.filter((existing) -> !existing.getId().equals(ignoredAccountId))
			.isPresent();
		if (taken) {
			throw new DuplicateAccountNameException();
		}
	}

	/** O índice único do banco cobre requisições simultâneas que passam pela verificação prévia. */
	private Account saveEnforcingUniqueName(Account account) {
		try {
			return this.accounts.saveAndFlush(account);
		}
		catch (DataIntegrityViolationException ex) {
			throw new DuplicateAccountNameException();
		}
	}

}
