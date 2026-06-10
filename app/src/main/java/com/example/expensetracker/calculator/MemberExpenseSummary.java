package com.example.expensetracker.calculator;

public class MemberExpenseSummary {

    private String memberId;
    private String memberName;
    private double amount;
    private double groupAmount;
    private double individualAmount;

    public MemberExpenseSummary(String memberId, String memberName, double amount) {
        this(memberId, memberName, amount, amount, 0d);
    }

    public MemberExpenseSummary(
            String memberId,
            String memberName,
            double amount,
            double groupAmount,
            double individualAmount
    ) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.amount = amount;
        this.groupAmount = groupAmount;
        this.individualAmount = individualAmount;
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

    public double getGroupAmount() {
        return groupAmount;
    }

    public double getIndividualAmount() {
        return individualAmount;
    }
}
