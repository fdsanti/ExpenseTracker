package com.example.expensetracker.calculator;

import java.util.List;

public class ExpenseSummary {

    private double totalAmount;
    private List<MemberExpenseSummary> memberSummaries;

    public ExpenseSummary(double totalAmount, List<MemberExpenseSummary> memberSummaries) {
        this.totalAmount = totalAmount;
        this.memberSummaries = memberSummaries;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public List<MemberExpenseSummary> getMemberSummaries() {
        return memberSummaries;
    }
}
