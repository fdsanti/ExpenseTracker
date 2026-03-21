package com.example.expensetracker.calculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.Member;

public class ExpenseSummaryCalculator {

    public static ExpenseSummary calculate(List<Expense> expenses, List<Member> members) {

        double total = 0;
        HashMap<String, Double> amountByMemberId = new HashMap<>();

        if (members != null) {
            for (Member member : members) {
                amountByMemberId.put(member.getId(), 0.0);
            }
        }

        if (expenses != null) {
            for (Expense expense : expenses) {
                total += expense.getAmount();

                String memberId = expense.getPaidByMemberId();
                double currentAmount = amountByMemberId.containsKey(memberId)
                        ? amountByMemberId.get(memberId)
                        : 0.0;

                amountByMemberId.put(memberId, currentAmount + expense.getAmount());
            }
        }

        List<MemberExpenseSummary> memberSummaries = new ArrayList<>();

        if (members != null) {
            for (Member member : members) {
                double amount = amountByMemberId.containsKey(member.getId())
                        ? amountByMemberId.get(member.getId())
                        : 0.0;

                memberSummaries.add(new MemberExpenseSummary(
                        member.getId(),
                        member.getName(),
                        amount
                ));
            }
        }

        return new ExpenseSummary(total, memberSummaries);
    }
}