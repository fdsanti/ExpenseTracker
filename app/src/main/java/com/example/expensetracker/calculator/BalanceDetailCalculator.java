package com.example.expensetracker.calculator;

import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.Member;

import java.util.List;

public class BalanceDetailCalculator {

    public static BalanceDetail calculate(List<Expense> expenses, List<Member> members) {
        if (members == null || members.size() < 2) {
            return new BalanceDetail(
                    "",
                    0,
                    0,
                    0,
                    0,
                    "",
                    0,
                    0,
                    0,
                    0,
                    0,
                    "Nadie :)",
                    "",
                    0
            );
        }

        Member member1 = members.get(0);
        Member member2 = members.get(1);

        double totalExpenses = 0;
        double member1TotalSpent = 0;
        double member2TotalSpent = 0;

        if (expenses != null) {
            for (Expense expense : expenses) {
                double amount = expense.getAmount();
                totalExpenses += amount;

                if (member1.getId().equals(expense.getPaidByMemberId())) {
                    member1TotalSpent += amount;
                }

                if (member2.getId().equals(expense.getPaidByMemberId())) {
                    member2TotalSpent += amount;
                }
            }
        }

        double totalIncome = member1.getSalary() + member2.getSalary();

        if (totalIncome <= 0) {
            return new BalanceDetail(
                    member1.getName(),
                    member1TotalSpent,
                    0,
                    0,
                    member1TotalSpent,
                    member2.getName(),
                    member2TotalSpent,
                    0,
                    0,
                    member2TotalSpent,
                    totalExpenses,
                    "Nadie :)",
                    "",
                    0
            );
        }

        int member1Percentage = (int) Math.round((member1.getSalary() / totalIncome) * 100);
        int member2Percentage = (int) Math.round((member2.getSalary() / totalIncome) * 100);

        double member1Proportional = round2(totalExpenses * member1.getSalary() / totalIncome);
        double member2Proportional = round2(totalExpenses * member2.getSalary() / totalIncome);

        double member1Balance = round2(member1TotalSpent - member1Proportional);
        double member2Balance = round2(member2TotalSpent - member2Proportional);

        String debtorName = "Nadie :)";
        String creditorName = "";
        double debtAmount = 0;

        if (member1Balance < 0) {
            debtorName = member1.getName();
            creditorName = member2.getName();
            debtAmount = round2(Math.abs(member1Balance));
        } else if (member2Balance < 0) {
            debtorName = member2.getName();
            creditorName = member1.getName();
            debtAmount = round2(Math.abs(member2Balance));
        }

        return new BalanceDetail(
                member1.getName(),
                round2(member1TotalSpent),
                member1Percentage,
                member1Proportional,
                member1Balance,
                member2.getName(),
                round2(member2TotalSpent),
                member2Percentage,
                member2Proportional,
                member2Balance,
                round2(totalExpenses),
                debtorName,
                creditorName,
                debtAmount
        );
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}