package com.example.expensetracker.ui.expense;

import com.example.expensetracker.calculator.CategorySummaryItem;
import com.example.expensetracker.calculator.DebtSummary;
import com.example.expensetracker.calculator.ExpenseSummary;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.Member;
import com.example.expensetracker.model.Tracker;
import com.example.expensetracker.calculator.ExpenseListQuery;


import java.util.List;
public class ExpenseScreenState {

    public Tracker tracker;
    public List<Member> members;
    public List<Expense> expenses;
    public List<Category> categories;

    public ExpenseSummary expenseSummary;
    public DebtSummary debtSummary;
    public List<CategorySummaryItem> categorySummary;
    public List<Expense> visibleExpenses;
    public String errorMessage;
    public ContentTab selectedTab;
    public String selectedMemberFilter;
    public ExpenseListQuery.SortType selectedSortType;
    public List<String> expandedCategoryIds;
    public String selectedMemberFilterId; // null = todos
    public SortType sortType;
    public boolean loading;

    public ExpenseScreenState(
            Tracker tracker,
            List<Member> members,
            List<Expense> expenses,
            List<Category> categories,
            ExpenseSummary expenseSummary,
            DebtSummary debtSummary,
            List<CategorySummaryItem> categorySummary,
            List<Expense> visibleExpenses,
            ContentTab selectedTab,
            String selectedMemberFilter,
            ExpenseListQuery.SortType selectedSortType,
            List<String> expandedCategoryIds,
            boolean loading,
            String errorMessage
    ) {
        this.tracker = tracker;
        this.members = members;
        this.expenses = expenses;
        this.categories = categories;
        this.expenseSummary = expenseSummary;
        this.debtSummary = debtSummary;
        this.categorySummary = categorySummary;
        this.visibleExpenses = visibleExpenses;
        this.selectedTab = selectedTab;
        this.selectedMemberFilter = selectedMemberFilter;
        this.selectedSortType = selectedSortType;
        this.expandedCategoryIds = expandedCategoryIds;
        this.loading = loading;
        this.errorMessage = errorMessage;
        this.selectedMemberFilterId = null;
        this.sortType = SortType.DATE_DESC;
    }

    public enum SortType {
        DATE_DESC,
        DATE_ASC,
        AMOUNT_DESC,
        AMOUNT_ASC
    }

    public enum ContentTab {
        CATEGORIES,
        EXPENSES
    }

}
