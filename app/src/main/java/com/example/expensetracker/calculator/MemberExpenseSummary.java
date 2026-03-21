package com.example.expensetracker.calculator;

public class MemberExpenseSummary {

    private String memberId;
    private String memberName;
    private double amount;

    public MemberExpenseSummary(String memberId, String memberName, double amount) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.amount = amount;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public double getAmount() {
        return amount;
    }
}
