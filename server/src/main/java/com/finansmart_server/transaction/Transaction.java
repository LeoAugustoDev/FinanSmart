package com.finansmart_server.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.finansmart_server.account.Account;
import com.finansmart_server.category.Category;

@Entity
@Table(name = "transactions")
public class Transaction {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private String userId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false)
	private LocalDate occurredOn;

	@Column(length = 255)
	private String description;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Transaction() {
	}

	public Transaction(String userId, Account account, Category category, BigDecimal amount, LocalDate occurredOn,
			String description) {
		this.userId = userId;
		update(account, category, amount, occurredOn, description);
	}

	public final void update(Account account, Category category, BigDecimal amount, LocalDate occurredOn,
			String description) {
		this.account = account;
		this.category = category;
		this.amount = amount;
		this.occurredOn = occurredOn;
		this.description = description;
	}

	public UUID getId() {
		return this.id;
	}

	public String getUserId() {
		return this.userId;
	}

	public Account getAccount() {
		return this.account;
	}

	public Category getCategory() {
		return this.category;
	}

	public BigDecimal getAmount() {
		return this.amount;
	}

	public LocalDate getOccurredOn() {
		return this.occurredOn;
	}

	public String getDescription() {
		return this.description;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

}
