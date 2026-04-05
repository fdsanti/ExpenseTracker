package com.example.expensetracker.calculator;

import java.util.List;

public class CategorySummaryItem {

    private String categoryId;
    private String categoryName;
    private double amount;
    private List<CategoryExpenseItem> expenses;

    public CategorySummaryItem(
            String categoryId,
            String categoryName,
            double amount,
            List<CategoryExpenseItem> expenses
    ) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
        this.expenses = expenses;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public double getAmount() {
        return amount;
    }

    public List<CategoryExpenseItem> getExpenses() {
        return expenses;
    }
}
