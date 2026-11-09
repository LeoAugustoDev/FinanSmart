package com.finansmart_server.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.finansmart_server.account.Account;
import com.finansmart_server.account.AccountRepository;
import com.finansmart_server.account.AccountType;
import com.finansmart_server.category.Category;
import com.finansmart_server.category.CategoryRepository;
import com.finansmart_server.category.CategoryType;
import com.finansmart_server.shared.CurrentUser;
import com.finansmart_server.transaction.dto.TransactionRequest;
import com.finansmart_server.transaction.dto.TransactionResponse;
import com.finansmart_server.transaction.exception.InvalidTransactionFieldException;
import com.finansmart_server.transaction.exception.TransactionNotFoundException;

class TransactionServiceTests {

	private static final String USER = "user-1";

	private static final LocalDate DAY = LocalDate.of(2026, 11, 9);

	private final TransactionRepository transactions = mock(TransactionRepository.class);

	private final AccountRepository accounts = mock(AccountRepository.class);

	private final CategoryRepository categories = mock(CategoryRepository.class);

	private final CurrentUser currentUser = mock(CurrentUser.class);

	private final TransactionService service = new TransactionService(this.transactions, this.accounts,
			this.categories, this.currentUser);

	private final Account account = withId(new Account(USER, "Nubank", AccountType.CHECKING, BigDecimal.ZERO));

	private final Category category = withId(new Category(null, "Lazer", CategoryType.EXPENSE));

	@BeforeEach
	void setUp() {
		given(this.currentUser.id()).willReturn(USER);
		given(this.transactions.saveAndFlush(any())).willAnswer((invocation) -> invocation.getArgument(0));
		given(this.accounts.findByIdAndUserId(this.account.getId(), USER)).willReturn(Optional.of(this.account));
		given(this.categories.findVisibleById(this.category.getId(), USER)).willReturn(Optional.of(this.category));
	}

	@Test
	void createsTransactionWithTypeFromTheCategory() {
		TransactionResponse response = this.service.create(request(this.account.getId(), this.category.getId()));

		assertThat(response.type()).isEqualTo(CategoryType.EXPENSE);
		assertThat(response.accountName()).isEqualTo("Nubank");
		assertThat(response.categoryName()).isEqualTo("Lazer");
		assertThat(response.amount()).isEqualByComparingTo("50.00");
	}

	@Test
	void rejectsAccountThatIsNotFromTheUser() {
		UUID other = UUID.randomUUID();
		given(this.accounts.findByIdAndUserId(other, USER)).willReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.create(request(other, this.category.getId())))
			.isInstanceOf(InvalidTransactionFieldException.class);
		verify(this.transactions, never()).saveAndFlush(any());
	}

	@Test
	void rejectsCategoryThatIsNotVisibleToTheUser() {
		UUID other = UUID.randomUUID();
		given(this.categories.findVisibleById(other, USER)).willReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.create(request(this.account.getId(), other)))
			.isInstanceOf(InvalidTransactionFieldException.class);
	}

	@Test
	void rejectsNewTransactionOnArchivedAccount() {
		this.account.archive();

		assertThatThrownBy(() -> this.service.create(request(this.account.getId(), this.category.getId())))
			.isInstanceOf(InvalidTransactionFieldException.class);
	}

	@Test
	void allowsEditingTransactionThatIsAlreadyOnAnArchivedAccount() {
		Transaction existing = withId(new Transaction(USER, this.account, this.category, BigDecimal.ONE, DAY, null));
		given(this.transactions.findByIdAndUserId(existing.getId(), USER)).willReturn(Optional.of(existing));
		this.account.archive();

		TransactionResponse response = this.service.update(existing.getId(),
				request(this.account.getId(), this.category.getId()));

		assertThat(response.amount()).isEqualByComparingTo("50.00");
	}

	@Test
	void rejectsMovingTransactionToAnArchivedAccount() {
		Transaction existing = withId(new Transaction(USER, this.account, this.category, BigDecimal.ONE, DAY, null));
		Account archived = withId(new Account(USER, "Antiga", AccountType.WALLET, BigDecimal.ZERO));
		archived.archive();
		given(this.transactions.findByIdAndUserId(existing.getId(), USER)).willReturn(Optional.of(existing));
		given(this.accounts.findByIdAndUserId(archived.getId(), USER)).willReturn(Optional.of(archived));

		assertThatThrownBy(() -> this.service.update(existing.getId(), request(archived.getId(), this.category.getId())))
			.isInstanceOf(InvalidTransactionFieldException.class);
	}

	@Test
	void rejectsInvertedPeriod() {
		TransactionFilter filter = new TransactionFilter(DAY.plusDays(1), DAY, null, null);

		assertThatThrownBy(() -> this.service.list(filter, 0, 20)).isInstanceOf(InvalidTransactionFieldException.class);
	}

	@Test
	void transactionOfAnotherUserIsNotFound() {
		UUID id = UUID.randomUUID();
		given(this.transactions.findByIdAndUserId(id, USER)).willReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.get(id)).isInstanceOf(TransactionNotFoundException.class);
	}

	private static TransactionRequest request(UUID accountId, UUID categoryId) {
		return new TransactionRequest(accountId, categoryId, new BigDecimal("50.00"), DAY, "Cinema");
	}

	private static <T> T withId(T entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
		return entity;
	}

}
