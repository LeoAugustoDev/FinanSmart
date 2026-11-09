package com.finansmart_server.category;

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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "categories")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Dono da categoria. Nulo nas categorias padrão do sistema. */
	@Column(updatable = false)
	private String userId;

	@Column(nullable = false, length = 60)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private CategoryType type;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Category() {
	}

	public Category(String userId, String name, CategoryType type) {
		this.userId = userId;
		this.name = name;
		this.type = type;
	}

	public void rename(String name) {
		this.name = name;
	}

	public boolean isSystem() {
		return this.userId == null;
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

	public CategoryType getType() {
		return this.type;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

}
