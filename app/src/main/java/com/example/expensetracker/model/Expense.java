package com.example.expensetracker.model;

public class Expense {

    private String id;
    private String description;
    private double amount;
    private String categoryId;
    private String paidByMemberId;
    private long date;

    public Expense() {
    }

    public Expense(String id, String name, double amount, String categoryId, String paidByMemberId, long date) {
        this.id = id;
        this.description = name;
        this.amount = amount;
        this.categoryId = categoryId;
        this.paidByMemberId = paidByMemberId;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getPaidByMemberId() {
        return paidByMemberId;
    }

    public long getDate() {
        return date;
    }

}
