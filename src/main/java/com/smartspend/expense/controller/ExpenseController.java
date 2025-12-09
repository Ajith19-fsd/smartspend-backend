package com.smartspend.expense.controller;

import com.smartspend.expense.dto.ExpenseRequest;
import com.smartspend.expense.model.Expense;
import com.smartspend.expense.service.ExpenseService;
import com.smartspend.auth.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin("*")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private JwtUtils jwtUtils;

    // Extract userId from JWT
    private Long getUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = header.substring(7);
        Long userId = jwtUtils.extractUserId(token);
        if (userId == null) {
            throw new RuntimeException("Invalid token: userId not found");
        }
        return userId;
    }

    // ➕ Add Expense
    @PostMapping
    public ResponseEntity<Expense> addExpense(
            @Valid @RequestBody ExpenseRequest req,
            HttpServletRequest request) {

        Long userId = getUserId(request);
        Expense expense = expenseService.buildExpenseEntity(req, userId);

        Expense saved = expenseService.saveExpense(expense);
        return ResponseEntity.ok(saved);
    }

    // 📌 Get all expenses for user
    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(expenseService.getAllExpenses(userId));
    }

    // 🆕 📌 Get Recent 5 expenses
    @GetMapping("/recent")
    public ResponseEntity<List<Expense>> getRecentExpenses(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(expenseService.getRecentExpenses(userId));
    }

    // 🔍 Get expense by ID
    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpenseById(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(expenseService.getExpenseById(id, userId));
    }

    // 🖊 Update expense
    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest req,
            HttpServletRequest request) {

        Long userId = getUserId(request);
        Expense expense = expenseService.buildExpenseEntity(req, userId);

        return ResponseEntity.ok(expenseService.updateExpense(id, expense));
    }

    // ❌ Delete expense
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteExpense(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        expenseService.deleteExpense(id, userId);
        return ResponseEntity.ok("Deleted successfully");
    }

    // 🎯 Filter expenses
    @GetMapping("/filter")
    public ResponseEntity<List<Expense>> filterExpenses(
            HttpServletRequest request,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String search) {

        Long userId = getUserId(request);
        return ResponseEntity.ok(
                expenseService.filterExpenses(userId, category, start, end, minAmount, maxAmount, search)
        );
    }
}
