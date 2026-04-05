package com.example.expensetracker.calculator;

public class CategoryExpenseItem {

    private String expenseId;
    private String description;
    private String memberName;
    private double amount;
    private long date;

    public CategoryExpenseItem(String expenseId, String description, String memberName, double amount, long date) {
        this.expenseId = expenseId;
        this.description = description;
        this.memberName = memberName;
        this.amount = amount;
        this.date = date;
    }

    public String getExpenseId() {
        return expenseId;
    }

    public String getDescription() {
        return description;
    }

    public String getMemberName() {
        return memberName;
    }

    public double getAmount() {
        return amount;
    }

    public long getDate() {
        return date;
    }
}
