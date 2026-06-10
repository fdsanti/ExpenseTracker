package com.example.expensetracker.model;

public class Expense {

    private String id;
    private String description;
    private double amount;
    private String categoryId;
    private String paidByMemberId;
    private long date;
    private boolean individual;

    public Expense() {
    }

    public Expense(String id, String name, double amount, String categoryId, String paidByMemberId, long date) {
        this(id, name, amount, categoryId, paidByMemberId, date, false);
    }

    public Expense(String id, String name, double amount, String categoryId, String paidByMemberId, long date, boolean individual) {
        this.id = id;
        this.description = name;
        this.amount = amount;
        this.categoryId = categoryId;
        this.paidByMemberId = paidByMemberId;
        this.date = date;
        this.individual = individual;
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

    public boolean isIndividual() {
        return individual;
    }

    public boolean isGroupExpense() {
        return !individual;
    }

}
