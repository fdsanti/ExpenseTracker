package com.example.expensetracker.calculator;

public class BalanceDetail {

    private final String member1Name;
    private final double member1TotalSpent;
    private final int member1Percentage;
    private final double member1Proportional;
    private final double member1Balance;

    private final String member2Name;
    private final double member2TotalSpent;
    private final int member2Percentage;
    private final double member2Proportional;
    private final double member2Balance;

    private final double totalExpenses;
    private final String debtorName;
    private final String creditorName;
    private final double debtAmount;

    public BalanceDetail(
            String member1Name,
            double member1TotalSpent,
            int member1Percentage,
            double member1Proportional,
            double member1Balance,
            String member2Name,
            double member2TotalSpent,
            int member2Percentage,
            double member2Proportional,
            double member2Balance,
            double totalExpenses,
            String debtorName,
            String creditorName,
            double debtAmount
    ) {
        this.member1Name = member1Name;
        this.member1TotalSpent = member1TotalSpent;
        this.member1Percentage = member1Percentage;
        this.member1Proportional = member1Proportional;
        this.member1Balance = member1Balance;
        this.member2Name = member2Name;
        this.member2TotalSpent = member2TotalSpent;
        this.member2Percentage = member2Percentage;
        this.member2Proportional = member2Proportional;
        this.member2Balance = member2Balance;
        this.totalExpenses = totalExpenses;
        this.debtorName = debtorName;
        this.creditorName = creditorName;
        this.debtAmount = debtAmount;
    }

    public String getMember1Name() {
        return member1Name;
    }

    public double getMember1TotalSpent() {
        return member1TotalSpent;
    }

    public int getMember1Percentage() {
        return member1Percentage;
    }

    public double getMember1Proportional() {
        return member1Proportional;
    }

    public double getMember1Balance() {
        return member1Balance;
    }

    public String getMember2Name() {
        return member2Name;
    }

    public double getMember2TotalSpent() {
        return member2TotalSpent;
    }

    public int getMember2Percentage() {
        return member2Percentage;
    }

    public double getMember2Proportional() {
        return member2Proportional;
    }

    public double getMember2Balance() {
        return member2Balance;
    }

    public double getTotalExpenses() {
        return totalExpenses;
    }

    public String getDebtorName() {
        return debtorName;
    }

    public String getCreditorName() {
        return creditorName;
    }

    public double getDebtAmount() {
        return debtAmount;
    }
}