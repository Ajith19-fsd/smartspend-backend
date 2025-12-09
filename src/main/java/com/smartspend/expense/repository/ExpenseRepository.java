package com.smartspend.expense.repository;

import com.smartspend.expense.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserId(Long userId);

    List<Expense> findByUserIdAndCategory(Long userId, String category);

    List<Expense> findByUserIdOrderByDateDesc(Long userId);

    List<Expense> findTop5ByUserIdOrderByDateDesc(Long userId);

    // 🆕 Date Range Queries for Reports
    List<Expense> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Expense> findByUserIdAndCategoryAndDateBetween(Long userId, String category,
                                                        LocalDate start, LocalDate end);
}
