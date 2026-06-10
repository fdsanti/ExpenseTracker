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
                    if (matchesTypeFilter(expense, query)) {
                        result.add(expense);
                    }
                    continue;
                }

                if (query.getMemberIdFilter().equals(expense.getPaidByMemberId())
                        && matchesTypeFilter(expense, query)) {
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
                        return compareByDateThenId(e1, e2, true);

                    case DATE_DESC:
                        return compareByDateThenId(e1, e2, false);

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

    private static boolean matchesTypeFilter(Expense expense, ExpenseListQuery query) {
        if (query == null || query.getTypeFilter() == null) {
            return true;
        }

        switch (query.getTypeFilter()) {
            case GROUP:
                return expense.isGroupExpense();

            case INDIVIDUAL:
                return expense.isIndividual();

            default:
                return true;
        }
    }

    private static int compareByDateThenId(Expense e1, Expense e2, boolean ascending) {
        int dateCompare = ascending
                ? Long.compare(e1.getDate(), e2.getDate())
                : Long.compare(e2.getDate(), e1.getDate());

        if (dateCompare != 0) {
            return dateCompare;
        }

        String id1 = e1.getId();
        String id2 = e2.getId();

        if (id1 == null && id2 == null) {
            return 0;
        }

        if (id1 == null) {
            return 1;
        }

        if (id2 == null) {
            return -1;
        }

        return ascending
                ? id1.compareTo(id2)
                : id2.compareTo(id1);
    }
}
