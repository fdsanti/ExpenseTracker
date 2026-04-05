package com.example.expensetracker.calculator;

import java.util.List;

import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.Member;

public class DebtCalculator {

    public static DebtSummary calculate(List<Expense> expenses, List<Member> members) {

        if (members == null || members.size() < 2) {
            return new DebtSummary(null, null, null, null, 0);
        }

        Member m1 = members.get(0);
        Member m2 = members.get(1);

        double totalExpenses = 0;
        double m1Paid = 0;
        double m2Paid = 0;

        if (expenses != null) {
            for (Expense expense : expenses) {
                totalExpenses += expense.getAmount();

                if (m1.getId().equals(expense.getPaidByMemberId())) {
                    m1Paid += expense.getAmount();
                }

                if (m2.getId().equals(expense.getPaidByMemberId())) {
                    m2Paid += expense.getAmount();
                }
            }
        }

        double totalSalary = m1.getSalary() + m2.getSalary();

        if (totalSalary == 0) {
            return new DebtSummary(null, null, null, null, 0);
        }

        double m1Share = (m1.getSalary() / totalSalary) * totalExpenses;
        double m2Share = (m2.getSalary() / totalSalary) * totalExpenses;

        double m1Balance = m1Paid - m1Share;
        double m2Balance = m2Paid - m2Share;

        if (m1Balance > 0) {
            return new DebtSummary(
                    m2.getId(),
                    m2.getName(),
                    m1.getId(),
                    m1.getName(),
                    Math.abs(m1Balance)
            );
        } else {
            return new DebtSummary(
                    m1.getId(),
                    m1.getName(),
                    m2.getId(),
                    m2.getName(),
                    Math.abs(m2Balance)
            );
        }
    }
}

