package com.finansmart_server.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "accounts")
public class Account {

	private static final String DEFAULT_CURRENCY = "BRL";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private String userId;

	@Column(nullable = false, length = 60)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountType type;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal initialBalance;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 3)
	private String currency = DEFAULT_CURRENCY;

	@Column(nullable = false)
	private boolean archived;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Account() {
	}

	public Account(String userId, String name, AccountType type, BigDecimal initialBalance) {
		this.userId = userId;
		this.name = name;
		this.type = type;
		this.initialBalance = initialBalance;
	}

	public void update(String name, AccountType type, BigDecimal initialBalance) {
		this.name = name;
		this.type = type;
		this.initialBalance = initialBalance;
	}

	public void archive() {
		this.archived = true;
	}

	public void unarchive() {
		this.archived = false;
	}

	public UUID getId() {
		return this.id;
	}

	public String getUserId() {
		return this.userId;
	}

	public String getName() {
		return this.name;
	}

	public AccountType getType() {
		return this.type;
	}

	public BigDecimal getInitialBalance() {
		return this.initialBalance;
	}

	public String getCurrency() {
		return this.currency;
	}

	public boolean isArchived() {
		return this.archived;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

}
