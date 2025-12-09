package com.smartspend.report.service;

import com.smartspend.budget.repository.BudgetRepository;
import com.smartspend.expense.model.Expense;
import com.smartspend.expense.repository.ExpenseRepository;
import com.smartspend.report.dto.MonthlySummaryDto;
import com.smartspend.report.dto.CategorySummaryDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    // ==============================
    // Get filtered expenses
    // ==============================
    public List<Expense> getExpensesReport(Long userId, String category, LocalDate startDate, LocalDate endDate) {
        if (category != null && startDate != null && endDate != null) {
            return expenseRepository.findByUserIdAndCategoryAndDateBetween(userId, category, startDate, endDate);
        } else if (category != null) {
            return expenseRepository.findByUserIdAndCategory(userId, category);
        } else if (startDate != null && endDate != null) {
            return expenseRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        } else {
            return expenseRepository.findByUserId(userId);
        }
    }

    // ==============================
    // Generate PDF
    // ==============================
    public ByteArrayInputStream generatePdfReport(Long userId, String category, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Expense> expenses = getExpensesReport(userId, category, startDate, endDate);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        document.add(new Paragraph("Expenses Report\n\n"));

        for (Expense e : expenses) {
            String line = "Title: " + e.getTitle() +
                    ", Amount: " + e.getAmount() +
                    ", Category: " + e.getCategory() +
                    ", Type: " + e.getType() +
                    ", Date: " + e.getDate();
            document.add(new Paragraph(line));
        }

        document.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==============================
    // Generate Excel
    // ==============================
    public ByteArrayInputStream generateExcelReport(Long userId, String category, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Expense> expenses = getExpensesReport(userId, category, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Expenses");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Title");
            header.createCell(1).setCellValue("Amount");
            header.createCell(2).setCellValue("Category");
            header.createCell(3).setCellValue("Type");
            header.createCell(4).setCellValue("Date");

            int rowIdx = 1;
            for (Expense e : expenses) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getTitle());
                row.createCell(1).setCellValue(e.getAmount());
                row.createCell(2).setCellValue(e.getCategory());
                row.createCell(3).setCellValue(e.getType());
                row.createCell(4).setCellValue(e.getDate().toString());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // ==============================
    // Dashboard Summary (cards)
    // ==============================
    public Map<String, Double> getDashboardSummary(Long userId) {
        Map<String, Double> result = new HashMap<>();

        List<Expense> allExpenses = expenseRepository.findByUserId(userId);

        double totalExpense = allExpenses.stream()
                .filter(e -> e.getType() != null && e.getType().equalsIgnoreCase("expense"))
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0.0)
                .sum();

        double totalIncome = allExpenses.stream()
                .filter(e -> e.getType() != null && e.getType().equalsIgnoreCase("income"))
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0.0)
                .sum();

        double totalBudget = budgetRepository.findByUserId(userId)
                .stream()
                .mapToDouble(b -> b.getAmount() != null ? b.getAmount() : 0.0)
                .sum();

        result.put("totalExpenses", totalExpense);
        result.put("totalIncome", totalIncome);
        result.put("totalBudget", totalBudget);
        result.put("remaining", totalBudget - totalExpense);

        return result;
    }

    // ==============================
    // Monthly Summary (Income + Expense + Categories)
    // ==============================
    public MonthlySummaryDto getMonthlySummary(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Expense> expenses = getExpensesReport(userId, null, start, end);

        double totalExpense = expenses.stream()
                .filter(e -> e.getType().equalsIgnoreCase("expense"))
                .mapToDouble(Expense::getAmount)
                .sum();

        double totalIncome = expenses.stream()
                .filter(e -> e.getType().equalsIgnoreCase("income"))
                .mapToDouble(Expense::getAmount)
                .sum();

        Map<String, Double> categoryMap = new HashMap<>();
        for (Expense e : expenses) {
            categoryMap.put(e.getCategory(),
                    categoryMap.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
        }

        List<CategorySummaryDto> categories = categoryMap.entrySet().stream()
                .map(entry -> new CategorySummaryDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        MonthlySummaryDto summary = new MonthlySummaryDto();
        summary.setMonth(ym.toString());
        summary.setTotalExpense(totalExpense);
        summary.setTotalIncome(totalIncome);
        summary.setCategories(categories);

        return summary;
    }

    // ==============================
    // CATEGORY SUMMARY (Pie Chart)
    // ==============================
    public List<CategorySummaryDto> getCategorySummary(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Expense> expenses = getExpensesReport(userId, null, start, end);

        Map<String, Double> categoryMap = new HashMap<>();
        for (Expense e : expenses) {
            categoryMap.put(e.getCategory(),
                    categoryMap.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
        }

        return categoryMap.entrySet().stream()
                .map(entry -> new CategorySummaryDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    // ==============================
    // INCOME vs EXPENSE Summary (Bar Chart)
    // ==============================
    public Map<String, Double> getIncomeExpenseSummary(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Expense> expenses = getExpensesReport(userId, null, start, end);

        double totalExpense = expenses.stream()
                .filter(e -> e.getType().equalsIgnoreCase("expense"))
                .mapToDouble(Expense::getAmount)
                .sum();

        double totalIncome = expenses.stream()
                .filter(e -> e.getType().equalsIgnoreCase("income"))
                .mapToDouble(Expense::getAmount)
                .sum();

        Map<String, Double> summary = new HashMap<>();
        summary.put("income", totalIncome);
        summary.put("expense", totalExpense);
        summary.put("balance", totalIncome - totalExpense);

        return summary;
    }

    // ==============================
    // MONTHLY TREND (Line Chart) - Income + Expense
    // ==============================
    public Map<String, Map<String, Double>> getMonthlyTrend(Long userId, int year) {
        Map<String, Map<String, Double>> trend = new LinkedHashMap<>();

        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            List<Expense> monthlyExpenses = expenseRepository.findByUserIdAndDateBetween(userId, start, end);

            double totalExpense = monthlyExpenses.stream()
                    .filter(e -> e.getType().equalsIgnoreCase("expense"))
                    .mapToDouble(Expense::getAmount)
                    .sum();

            double totalIncome = monthlyExpenses.stream()
                    .filter(e -> e.getType().equalsIgnoreCase("income"))
                    .mapToDouble(Expense::getAmount)
                    .sum();

            Map<String, Double> monthData = new HashMap<>();
            monthData.put("expense", totalExpense);
            monthData.put("income", totalIncome);

            trend.put(ym.toString(), monthData);
        }

        return trend;
    }
}
