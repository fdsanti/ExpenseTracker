package com.example.expensetracker.calculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.Member;

public class CategorySummaryCalculator {

    public static List<CategorySummaryItem> calculate(
            List<Expense> expenses,
            List<Category> categories,
            List<Member> members
    ) {
        HashMap<String, Double> amountsByCategoryId = new HashMap<>();
        HashMap<String, String> namesByCategoryId = new HashMap<>();
        HashMap<String, List<CategoryExpenseItem>> expensesByCategoryId = new HashMap<>();
        HashMap<String, String> memberNamesById = new HashMap<>();

        if (categories != null) {
            for (Category category : categories) {
                namesByCategoryId.put(category.getId(), category.getName());
            }
        }

        if (members != null) {
            for (Member member : members) {
                memberNamesById.put(member.getId(), member.getName());
            }
        }

        if (expenses != null) {
            for (Expense expense : expenses) {
                String categoryId = expense.getCategoryId();

                if (categoryId == null) {
                    continue;
                }

                double currentAmount = amountsByCategoryId.containsKey(categoryId)
                        ? amountsByCategoryId.get(categoryId)
                        : 0.0;

                amountsByCategoryId.put(categoryId, currentAmount + expense.getAmount());

                List<CategoryExpenseItem> categoryExpenses = expensesByCategoryId.get(categoryId);
                if (categoryExpenses == null) {
                    categoryExpenses = new ArrayList<>();
                    expensesByCategoryId.put(categoryId, categoryExpenses);
                }

                String memberName = memberNamesById.get(expense.getPaidByMemberId());
                if (memberName == null) {
                    memberName = "";
                }

                categoryExpenses.add(new CategoryExpenseItem(
                        expense.getId(),
                        expense.getDescription(),
                        memberName,
                        expense.getAmount(),
                        expense.getDate()
                ));
            }
        }

        List<CategorySummaryItem> result = new ArrayList<>();

        for (String categoryId : amountsByCategoryId.keySet()) {
            String categoryName = namesByCategoryId.get(categoryId);

            if (categoryName == null) {
                categoryName = categoryId;
            }

            List<CategoryExpenseItem> categoryExpenses = expensesByCategoryId.get(categoryId);
            if (categoryExpenses == null) {
                categoryExpenses = new ArrayList<>();
            }

            Collections.sort(categoryExpenses, new Comparator<CategoryExpenseItem>() {
                @Override
                public int compare(CategoryExpenseItem item1, CategoryExpenseItem item2) {
                    return Long.compare(item2.getDate(), item1.getDate());
                }
            });

            result.add(new CategorySummaryItem(
                    categoryId,
                    categoryName,
                    amountsByCategoryId.get(categoryId),
                    categoryExpenses
            ));
        }

        Collections.sort(result, new Comparator<CategorySummaryItem>() {
            @Override
            public int compare(CategorySummaryItem item1, CategorySummaryItem item2) {
                return Double.compare(item2.getAmount(), item1.getAmount());
            }
        });

        return result;
    }
}