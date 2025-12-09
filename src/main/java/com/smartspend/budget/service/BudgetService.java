package com.smartspend.budget.service;

import com.smartspend.budget.model.Budget;
import com.smartspend.budget.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    // ➕ Create Budget
    public Budget saveBudget(Budget budget) {
        if (budget.getUserId() == null) {
            throw new RuntimeException("Cannot create budget without userId");
        }
        return budgetRepository.save(budget);
    }

    // 📌 Get all budgets of a user
    public List<Budget> getBudgets(Long userId) {
        return budgetRepository.findByUserId(userId);
    }

    // 🔍 Get budget by ID safely
    public Budget getBudgetById(Long id, Long userId) {
        return budgetRepository.findById(id)
                .filter(b -> Objects.equals(b.getUserId(), userId))
                .orElse(null);
    }

    // 🔍 Get budget by category safely (case-insensitive)
    public Budget getBudgetByCategory(Long userId, String category) {
        return budgetRepository.findByUserIdAndCategoryIgnoreCase(userId, category)
                .orElse(null);
    }

    // 🖊 Update Budget safely
    public Budget updateBudget(Long id, Budget updated) {
        return budgetRepository.findById(id)
                .filter(b -> Objects.equals(b.getUserId(), updated.getUserId()))
                .map(b -> {
                    b.setCategory(updated.getCategory());
                    b.setAmount(updated.getAmount());
                    return budgetRepository.save(b);
                })
                .orElse(null);
    }

    // ❌ Delete Budget safely
    public boolean deleteBudget(Long id, Long userId) {
        return budgetRepository.findById(id)
                .filter(b -> Objects.equals(b.getUserId(), userId))
                .map(b -> {
                    budgetRepository.delete(b);
                    return true;
                })
                .orElse(false);
    }
}
