package com.smartspend.report.dto;

public class CategorySummaryDto {
    private String category;
    private double spent;

    public CategorySummaryDto(String category, double spent) {
        this.category = category;
        this.spent = spent;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getSpent() { return spent; }
    public void setSpent(double spent) { this.spent = spent; }
}
