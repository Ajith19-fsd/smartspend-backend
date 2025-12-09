package com.smartspend.expense.service;

import com.smartspend.budget.model.Budget;
import com.smartspend.budget.service.BudgetService;
import com.smartspend.email.EmailService;
import com.smartspend.expense.dto.ExpenseRequest;
import com.smartspend.expense.model.Expense;
import com.smartspend.expense.repository.ExpenseRepository;
import com.smartspend.notification.model.Notification;
import com.smartspend.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    // Build Expense entity
    public Expense buildExpenseEntity(ExpenseRequest req, Long userId) {
        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setTitle(req.getTitle());
        expense.setAmount(req.getAmount());
        expense.setCategory(req.getCategory());
        expense.setType(req.getType());
        expense.setDate(req.getDate() != null ? req.getDate() : LocalDate.now());
        expense.setDescription(req.getDescription());
        return expense;
    }

    // Save expense & check budget
    public Expense saveExpense(Expense expense) {
        if (expense.getUserId() == null) {
            throw new RuntimeException("Expense must have a userId");
        }
        Expense saved = expenseRepository.save(expense);
        checkBudgetAndNotify(saved);
        return saved;
    }

    // Update expense
    public Expense updateExpense(Long id, Expense expense) {
        Expense existing = getExpenseById(id, expense.getUserId());

        existing.setTitle(expense.getTitle());
        existing.setAmount(expense.getAmount());
        existing.setCategory(expense.getCategory());
        existing.setType(expense.getType());
        existing.setDate(expense.getDate());
        existing.setDescription(expense.getDescription());

        Expense updated = expenseRepository.save(existing);
        checkBudgetAndNotify(updated);

        return updated;
    }

    // Check budget & send notifications
    private void checkBudgetAndNotify(Expense expense) {
        if (expense == null || expense.getUserId() == null || expense.getCategory() == null) return;

        Long userId = expense.getUserId();
        String category = expense.getCategory();

        Optional<Budget> budgetOpt = Optional.ofNullable(
                budgetService.getBudgetByCategory(userId, category)
        );

        budgetOpt.ifPresent(budget -> {
            Double budgetAmount = budget.getAmount();
            if (budgetAmount == null) return;

            double totalSpent = expenseRepository
                    .findByUserIdAndCategory(userId, category)
                    .stream()
                    .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0)
                    .sum();

            boolean exceeded = totalSpent > budgetAmount;
            boolean nearing = totalSpent >= (budgetAmount * 0.9) && !exceeded;

            if (exceeded || nearing) {
                String title = exceeded ? "Budget Exceeded: " + category : "Budget Nearing Limit: " + category;
                String body = "Spent: ₹" + totalSpent + " / Budget: ₹" + budgetAmount;

                // In-app notification
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setTitle(title);
                notification.setMessage(body);
                notification.setCategory(category);
                notificationService.sendNotification(notification);

                // Email alert
                try {
                    if (emailService != null) {
                        emailService.sendEmail(userId, title, "Hi,\n\n" + body + "\n\n- SmartSpend");
                    }
                } catch (Exception e) {
                    System.err.println("Failed to send budget email alert to userId " + userId + ": " + e.getMessage());
                }
            }
        });
    }

    // Get all expenses
    public List<Expense> getAllExpenses(Long userId) {
        return expenseRepository.findByUserId(userId);
    }

    // Get recent 5 expenses
    public List<Expense> getRecentExpenses(Long userId) {
        return expenseRepository.findByUserIdOrderByDateDesc(userId)
                .stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    // Get expense by ID with user verification
    public Expense getExpenseById(Long id, Long userId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        if (!Objects.equals(expense.getUserId(), userId)) {
            throw new RuntimeException("Unauthorized access");
        }
        return expense;
    }

    // Delete expense
    public void deleteExpense(Long id, Long userId) {
        Expense expense = getExpenseById(id, userId);
        expenseRepository.delete(expense);
    }

    // Filter expenses
    public List<Expense> filterExpenses(Long userId, String category, String start, String end,
                                        Double minAmount, Double maxAmount, String search) {
        return expenseRepository.findByUserId(userId)
                .stream()
                .filter(e -> category == null || e.getCategory().equalsIgnoreCase(category))
                .filter(e -> search == null || (e.getTitle() != null &&
                        e.getTitle().toLowerCase().contains(search.toLowerCase())))
                .filter(e -> minAmount == null || (e.getAmount() != null && e.getAmount() >= minAmount))
                .filter(e -> maxAmount == null || (e.getAmount() != null && e.getAmount() <= maxAmount))
                .filter(e -> {
                    if (start == null && end == null) return true;
                    LocalDate date = e.getDate();
                    boolean afterStart = start == null || !date.isBefore(LocalDate.parse(start));
                    boolean beforeEnd = end == null || !date.isAfter(LocalDate.parse(end));
                    return afterStart && beforeEnd;
                })
                .collect(Collectors.toList());
    }
}
