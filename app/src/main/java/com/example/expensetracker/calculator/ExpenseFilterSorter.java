package com.example.expensetracker.calculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.example.expensetracker.model.Expense;

public class ExpenseFilterSorter {

    public static List<Expense> apply(List<Expense> expenses, ExpenseListQuery query) {
        List<Expense> result = new ArrayList<>();

        if (expenses != null) {
            for (Expense expense : expenses) {
                if (query == null || query.getMemberIdFilter() == null) {
                    result.add(expense);
                    continue;
                }

                if (query.getMemberIdFilter().equals(expense.getPaidByMemberId())) {
                    result.add(expense);
                }
            }
        }

        if (query == null || query.getSortType() == null) {
            return result;
        }

        Collections.sort(result, new Comparator<Expense>() {
            @Override
            public int compare(Expense e1, Expense e2) {
                switch (query.getSortType()) {
                    case DATE_ASC:
                        return Long.compare(e1.getDate(), e2.getDate());

                    case DATE_DESC:
                        return Long.compare(e2.getDate(), e1.getDate());

                    case AMOUNT_ASC:
                        return Double.compare(e1.getAmount(), e2.getAmount());

                    case AMOUNT_DESC:
                        return Double.compare(e2.getAmount(), e1.getAmount());

                    default:
                        return 0;
                }
            }
        });

        return result;
    }
}