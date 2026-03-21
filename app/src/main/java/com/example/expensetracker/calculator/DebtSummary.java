package com.example.expensetracker.calculator;

public class DebtSummary {

    private String fromMemberId;
    private String fromMemberName;
    private String toMemberId;
    private String toMemberName;
    private double amount;

    public DebtSummary(
            String fromMemberId,
            String fromMemberName,
            String toMemberId,
            String toMemberName,
            double amount
    ) {
        this.fromMemberId = fromMemberId;
        this.fromMemberName = fromMemberName;
        this.toMemberId = toMemberId;
        this.toMemberName = toMemberName;
        this.amount = amount;
    }

    public String getFromMemberId() {
        return fromMemberId;
    }

    public String getFromMemberName() {
        return fromMemberName;
    }

    public String getToMemberId() {
        return toMemberId;
    }

    public String getToMemberName() {
        return toMemberName;
    }

    public double getAmount() {
        return amount;
    }
}
