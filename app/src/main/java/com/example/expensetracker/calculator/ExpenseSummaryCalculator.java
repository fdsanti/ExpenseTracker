package com.example.expensetracker.calculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.Member;

public class ExpenseSummaryCalculator {

    public static ExpenseSummary calculate(List<Expense> expenses, List<Member> members) {

        double total = 0;
        HashMap<String, Double> groupAmountByMemberId = new HashMap<>();
        HashMap<String, Double> individualAmountByMemberId = new HashMap<>();

        if (members != null) {
            for (Member member : members) {
                groupAmountByMemberId.put(member.getId(), 0.0);
                individualAmountByMemberId.put(member.getId(), 0.0);
            }
        }

        if (expenses != null) {
            for (Expense expense : expenses) {
                total += expense.getAmount();

                String memberId = expense.getPaidByMemberId();
                HashMap<String, Double> targetAmounts = expense.isIndividual()
                        ? individualAmountByMemberId
                        : groupAmountByMemberId;

                double currentAmount = targetAmounts.containsKey(memberId)
                        ? targetAmounts.get(memberId)
                        : 0.0;

                targetAmounts.put(memberId, currentAmount + expense.getAmount());
            }
        }

        List<MemberExpenseSummary> memberSummaries = new ArrayList<>();

        if (members != null) {
            for (Member member : members) {
                double groupAmount = groupAmountByMemberId.containsKey(member.getId())
                        ? groupAmountByMemberId.get(member.getId())
                        : 0.0;
                double individualAmount = individualAmountByMemberId.containsKey(member.getId())
                        ? individualAmountByMemberId.get(member.getId())
                        : 0.0;
                double amount = groupAmount + individualAmount;

                memberSummaries.add(new MemberExpenseSummary(
                        member.getId(),
                        member.getName(),
                        amount,
                        groupAmount,
                        individualAmount
                ));
            }
        }

        return new ExpenseSummary(total, memberSummaries);
    }
}
