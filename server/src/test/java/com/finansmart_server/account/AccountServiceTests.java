package com.finansmart_server.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.finansmart_server.account.dto.AccountResponse;
import com.finansmart_server.account.dto.CreateAccountRequest;
import com.finansmart_server.account.dto.UpdateAccountRequest;
import com.finansmart_server.account.exception.AccountNotFoundException;
import com.finansmart_server.account.exception.DuplicateAccountNameException;
import com.finansmart_server.shared.CurrentUser;

class AccountServiceTests {

	private static final String USER = "user-1";

	private final AccountRepository repository = mock(AccountRepository.class);

	private final CurrentUser currentUser = mock(CurrentUser.class);

	private final AccountService service = new AccountService(this.repository, this.currentUser);

	@BeforeEach
	void setUp() {
		given(this.currentUser.id()).willReturn(USER);
		given(this.repository.saveAndFlush(any())).willAnswer((invocation) -> invocation.getArgument(0));
	}

	@Test
	void createsValidAccount() {
		given(this.repository.findActiveByName(USER, "Nubank")).willReturn(Optional.empty());

		AccountResponse response = this.service.create(createRequest("Nubank"));

		assertThat(response.name()).isEqualTo("Nubank");
		assertThat(response.type()).isEqualTo(AccountType.CHECKING);
		assertThat(response.initialBalance()).isEqualByComparingTo("1500.00");
		assertThat(response.currency()).isEqualTo("BRL");
		assertThat(response.archived()).isFalse();
	}

	@Test
	void rejectsDuplicateNameAmongActiveAccounts() {
		given(this.repository.findActiveByName(USER, "Nubank")).willReturn(Optional.of(account("Nubank")));

		assertThatThrownBy(() -> this.service.create(createRequest("Nubank")))
			.isInstanceOf(DuplicateAccountNameException.class);
		verify(this.repository, never()).saveAndFlush(any());
	}

	@Test
	void translatesConcurrentDuplicateFromTheDatabase() {
		given(this.repository.findActiveByName(USER, "Nubank")).willReturn(Optional.empty());
		given(this.repository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("duplicado"));

		assertThatThrownBy(() -> this.service.create(createRequest("Nubank")))
			.isInstanceOf(DuplicateAccountNameException.class);
	}

	@Test
	void allowsReusingTheNameOfAnArchivedAccount() {
		// Contas arquivadas não aparecem na busca por nome entre as ativas.
		given(this.repository.findActiveByName(USER, "Nubank")).willReturn(Optional.empty());

		assertThat(this.service.create(createRequest("Nubank")).name()).isEqualTo("Nubank");
	}

	@Test
	void updateKeepingItsOwnNameIsAllowed() {
		Account account = account("Nubank");
		given(this.repository.findByIdAndUserId(account.getId(), USER)).willReturn(Optional.of(account));
		given(this.repository.findActiveByName(USER, "NUBANK")).willReturn(Optional.of(account));

		AccountResponse response = this.service.update(account.getId(),
				new UpdateAccountRequest("NUBANK", AccountType.SAVINGS, BigDecimal.TEN));

		assertThat(response.name()).isEqualTo("NUBANK");
		assertThat(response.type()).isEqualTo(AccountType.SAVINGS);
	}

	@Test
	void unarchivingWithConflictingNameFails() {
		Account archived = account("Nubank");
		archived.archive();
		given(this.repository.findByIdAndUserId(archived.getId(), USER)).willReturn(Optional.of(archived));
		given(this.repository.findActiveByName(USER, "Nubank")).willReturn(Optional.of(account("Nubank")));

		assertThatThrownBy(() -> this.service.unarchive(archived.getId()))
			.isInstanceOf(DuplicateAccountNameException.class);
		assertThat(archived.isArchived()).isTrue();
	}

	@Test
	void accountOfAnotherUserIsNotFound() {
		UUID id = UUID.randomUUID();
		given(this.repository.findByIdAndUserId(id, USER)).willReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.get(id)).isInstanceOf(AccountNotFoundException.class);
	}

	private static CreateAccountRequest createRequest(String name) {
		return new CreateAccountRequest(name, AccountType.CHECKING, new BigDecimal("1500.00"));
	}

	private static Account account(String name) {
		Account account = new Account(USER, name, AccountType.CHECKING, BigDecimal.ZERO);
		ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
		return account;
	}

}
