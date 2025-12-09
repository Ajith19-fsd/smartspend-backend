package com.smartspend.report.dto;

import java.util.List;

public class MonthlySummaryDto {
    private String month;
    private double totalExpense;
    private double totalIncome;
    private List<CategorySummaryDto> categories;

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }
    public double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }
    public List<CategorySummaryDto> getCategories() { return categories; }
    public void setCategories(List<CategorySummaryDto> categories) { this.categories = categories; }
}
