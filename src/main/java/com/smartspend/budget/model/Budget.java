package com.smartspend.budget.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ❌ Do NOT validate userId since we assign it from JWT in the controller
    private Long userId;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Budget amount is required")
    private Double amount;

    public Budget() {}

    public Budget(Long userId, String category, Double amount) {
        this.userId = userId;
        this.category = category;
        this.amount = amount;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
