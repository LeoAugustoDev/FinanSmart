package com.finansmart_server.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
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

import com.finansmart_server.category.Category;

@Entity
@Table(name = "budgets")
public class Budget {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private String userId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false, updatable = false)
	private Category category;

	@Column(nullable = false, updatable = false)
	private LocalDate referenceMonth;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal limitAmount;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Budget() {
	}

	public Budget(String userId, Category category, YearMonth month, BigDecimal limitAmount) {
		this.userId = userId;
		this.category = category;
		this.referenceMonth = month.atDay(1);
		this.limitAmount = limitAmount;
	}

	public void changeLimit(BigDecimal limitAmount) {
		this.limitAmount = limitAmount;
	}

	public YearMonth getMonth() {
		return YearMonth.from(this.referenceMonth);
	}

	public UUID getId() {
		return this.id;
	}

	public String getUserId() {
		return this.userId;
	}

	public Category getCategory() {
		return this.category;
	}

	public BigDecimal getLimitAmount() {
		return this.limitAmount;
	}

}
