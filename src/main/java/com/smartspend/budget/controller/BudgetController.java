package com.smartspend.budget.controller;

import com.smartspend.budget.model.Budget;
import com.smartspend.budget.service.BudgetService;
import com.smartspend.auth.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@CrossOrigin("*")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private JwtUtils jwtUtils;

    // Extract userId safely from JWT Token
    private Long extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Long userId = jwtUtils.extractUserId(token);
            if (userId != null) return userId;
        }
        throw new RuntimeException("Unauthorized! Invalid or expired token.");
    }

    // ➕ Create Budget
    @PostMapping
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody Budget budget, HttpServletRequest request) {
        Long userId = extractUserId(request);
        budget.setUserId(userId);
        Budget saved = budgetService.saveBudget(budget);
        return ResponseEntity.ok(saved);
    }

    // 📌 Get all budgets for the user
    @GetMapping
    public ResponseEntity<List<Budget>> getBudgets(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(budgetService.getBudgets(userId));
    }

    // 🔍 Get one budget by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getBudgetById(@PathVariable Long id, HttpServletRequest request) {
        Long userId = extractUserId(request);
        Budget budget = budgetService.getBudgetById(id, userId);
        if (budget == null) return ResponseEntity.status(404).body("Budget not found");
        return ResponseEntity.ok(budget);
    }

    // 🔍 Get budget by category (case-insensitive)
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getBudgetByCategory(
            @PathVariable String category,
            HttpServletRequest request) {

        Long userId = extractUserId(request);
        Budget budget = budgetService.getBudgetByCategory(userId, category);
        if (budget == null) return ResponseEntity.status(404).body("Budget not found for category: " + category);
        return ResponseEntity.ok(budget);
    }

    // 🖊 Update Budget
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody Budget budget,
            HttpServletRequest request) {

        Long userId = extractUserId(request);
        budget.setUserId(userId); // required for ownership check
        Budget updated = budgetService.updateBudget(id, budget);
        if (updated == null) return ResponseEntity.status(404).body("Budget not found");
        return ResponseEntity.ok(updated);
    }

    // ❌ Delete Budget
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBudget(@PathVariable Long id, HttpServletRequest request) {
        Long userId = extractUserId(request);
        boolean deleted = budgetService.deleteBudget(id, userId);
        if (!deleted) return ResponseEntity.status(404).body("Budget not found or unauthorized");
        return ResponseEntity.ok("Budget deleted successfully");
    }
}
