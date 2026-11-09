package com.finansmart_server.transaction;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finansmart_server.account.Account;
import com.finansmart_server.account.AccountRepository;
import com.finansmart_server.category.Category;
import com.finansmart_server.category.CategoryRepository;
import com.finansmart_server.shared.CurrentUser;
import com.finansmart_server.shared.PageResponse;
import com.finansmart_server.transaction.dto.TransactionRequest;
import com.finansmart_server.transaction.dto.TransactionResponse;
import com.finansmart_server.transaction.exception.InvalidTransactionFieldException;
import com.finansmart_server.transaction.exception.TransactionNotFoundException;

@Service
@Transactional
public class TransactionService {

	private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("occurredOn"), Sort.Order.desc("createdAt"),
			Sort.Order.desc("id"));

	private final TransactionRepository transactions;

	private final AccountRepository accounts;

	private final CategoryRepository categories;

	private final CurrentUser currentUser;

	TransactionService(TransactionRepository transactions, AccountRepository accounts, CategoryRepository categories,
			CurrentUser currentUser) {
		this.transactions = transactions;
		this.accounts = accounts;
		this.categories = categories;
		this.currentUser = currentUser;
	}

	public TransactionResponse create(TransactionRequest request) {
		String userId = this.currentUser.id();
		Account account = findAccount(request.accountId(), userId);
		assertActive(account);
		Category category = findCategory(request.categoryId(), userId);
		Transaction transaction = new Transaction(userId, account, category, request.amount(), request.occurredOn(),
				request.description());
		return TransactionResponse.from(this.transactions.saveAndFlush(transaction));
	}

	@Transactional(readOnly = true)
	public PageResponse<TransactionResponse> list(TransactionFilter filter, int page, int size) {
		if ((filter.from() != null) && (filter.to() != null) && filter.from().isAfter(filter.to())) {
			throw new InvalidTransactionFieldException("from", "A data inicial deve ser anterior à data final");
		}
		var result = this.transactions.findAll(filter.toSpecification(this.currentUser.id()),
				PageRequest.of(page, size, NEWEST_FIRST));
		return PageResponse.of(result, TransactionResponse::from);
	}

	@Transactional(readOnly = true)
	public TransactionResponse get(UUID id) {
		return TransactionResponse.from(findOwned(id));
	}

	public TransactionResponse update(UUID id, TransactionRequest request) {
		Transaction transaction = findOwned(id);
		String userId = transaction.getUserId();
		Account account = findAccount(request.accountId(), userId);
		if (!account.getId().equals(transaction.getAccount().getId())) {
			assertActive(account);
		}
		Category category = findCategory(request.categoryId(), userId);
		transaction.update(account, category, request.amount(), request.occurredOn(), request.description());
		return TransactionResponse.from(this.transactions.saveAndFlush(transaction));
	}

	public void delete(UUID id) {
		this.transactions.delete(findOwned(id));
	}

	private Transaction findOwned(UUID id) {
		return this.transactions.findByIdAndUserId(id, this.currentUser.id())
			.orElseThrow(TransactionNotFoundException::new);
	}

	private Account findAccount(UUID id, String userId) {
		return this.accounts.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new InvalidTransactionFieldException("accountId", "Conta não encontrada"));
	}

	private Category findCategory(UUID id, String userId) {
		return this.categories.findVisibleById(id, userId)
			.orElseThrow(() -> new InvalidTransactionFieldException("categoryId", "Categoria não encontrada"));
	}

	private void assertActive(Account account) {
		if (account.isArchived()) {
			throw new InvalidTransactionFieldException("accountId", "Contas arquivadas não recebem lançamentos");
		}
	}

}
