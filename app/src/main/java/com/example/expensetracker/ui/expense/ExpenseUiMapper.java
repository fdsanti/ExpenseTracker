package com.example.expensetracker.ui.expense;

import com.example.expensetracker.calculator.CategorySummaryItem;
import com.example.expensetracker.calculator.ExpenseListQuery;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.Member;
import com.example.expensetracker.ui.expense.components.ContentCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ExpenseUiMapper {

    private ExpenseUiMapper() {
    }

    public static ContentCardView.Model toContentCardModel(ExpenseScreenState state) {
        return new ContentCardView.Model(
                mapSelectedTab(state.selectedTab),
                mapCategoryItems(state.categorySummary, state.expandedCategoryIds),
                mapExpenseItems(state.visibleExpenses, state.members, state.categories),
                isContentEmpty(state),
                mapMemberFilterOptions(state.members),
                state.selectedMemberFilter,
                mapSortOptions(),
                state.selectedSortType != null ? state.selectedSortType.name() : null
        );
    }

    private static ContentCardView.Tab mapSelectedTab(ExpenseScreenState.ContentTab selectedTab) {
        if (selectedTab == ExpenseScreenState.ContentTab.EXPENSES) {
            return ContentCardView.Tab.EXPENSES;
        }
        return ContentCardView.Tab.CATEGORIES;
    }

    private static List<ContentCardView.CategoryItemUi> mapCategoryItems(
            List<CategorySummaryItem> summaryItems,
            List<String> expandedCategoryIds
    ) {
        List<ContentCardView.CategoryItemUi> items = new ArrayList<>();

        if (summaryItems == null) {
            return items;
        }

        for (CategorySummaryItem item : summaryItems) {
            boolean expanded =
                    expandedCategoryIds != null &&
                            expandedCategoryIds.contains(item.getCategoryId());

            items.add(new ContentCardView.CategoryItemUi(
                    item.getCategoryId(),
                    safeString(item.getCategoryName()),
                    item.getAmount(),
                    expanded,
                    item.getExpenses()
            ));
        }

        return items;
    }

    private static List<ContentCardView.ExpenseItemUi> mapExpenseItems(
            List<Expense> expenses,
            List<Member> members,
            List<Category> categories
    ) {
        List<ContentCardView.ExpenseItemUi> items = new ArrayList<>();

        if (expenses == null) {
            return items;
        }

        for (Expense expense : expenses) {
            items.add(new ContentCardView.ExpenseItemUi(
                    safeString(expense.getId()),
                    safeString(expense.getDescription()),
                    formatDate(expense.getDate()),
                    resolveMemberName(expense.getPaidByMemberId(), members),
                    expense.getAmount(),
                    resolveCategoryName(expense.getCategoryId(), categories)
            ));
        }

        return items;
    }

    private static List<ContentCardView.FilterOptionUi> mapMemberFilterOptions(List<Member> members) {
        List<ContentCardView.FilterOptionUi> items = new ArrayList<>();

        if (members == null) {
            return items;
        }

        for (Member member : members) {
            if (member == null) {
                continue;
            }

            items.add(new ContentCardView.FilterOptionUi(
                    member.getId(),
                    safeString(member.getName())
            ));
        }

        return items;
    }

    private static List<ContentCardView.SortOptionUi> mapSortOptions() {
        List<ContentCardView.SortOptionUi> items = new ArrayList<>();

        items.add(new ContentCardView.SortOptionUi(
                ExpenseListQuery.SortType.DATE_DESC.name(),
                "Más recientes"
        ));

        items.add(new ContentCardView.SortOptionUi(
                ExpenseListQuery.SortType.AMOUNT_DESC.name(),
                "Precio: Mayor a menor"
        ));

        items.add(new ContentCardView.SortOptionUi(
                ExpenseListQuery.SortType.AMOUNT_ASC.name(),
                "Precio: Menor a mayor"
        ));

        return items;
    }

    private static String mapSortLabel(String sortTypeName) {
        if ("DATE_DESC".equals(sortTypeName)) {
            return "Más recientes";
        }
        if ("AMOUNT_DESC".equals(sortTypeName)) {
            return "Precio: Mayor a menor";
        }
        if ("AMOUNT_ASC".equals(sortTypeName)) {
            return "Precio: Menor a mayor";
        }
        return sortTypeName;
    }

    private static boolean isContentEmpty(ExpenseScreenState state) {
        if (state.selectedTab == ExpenseScreenState.ContentTab.EXPENSES) {
            return state.visibleExpenses == null || state.visibleExpenses.isEmpty();
        }

        return state.categorySummary == null || state.categorySummary.isEmpty();
    }

    private static String resolveMemberName(String memberId, List<Member> members) {
        if (members == null) {
            return "";
        }

        for (Member member : members) {
            if (member != null && safeString(member.getId()).equals(safeString(memberId))) {
                return safeString(member.getName());
            }
        }

        return "";
    }

    private static String resolveCategoryName(String categoryId, List<Category> categories) {
        if (categories == null) {
            return "";
        }

        for (Category category : categories) {
            if (category != null && safeString(category.getId()).equals(safeString(categoryId))) {
                return safeString(category.getName());
            }
        }

        return "";
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}