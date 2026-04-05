package com.example.expensetracker.ui.expense;

import com.example.expensetracker.calculator.ExpenseListQuery;
import com.example.expensetracker.data.TrackerRepository;
import com.example.expensetracker.model.Tracker;
import com.example.expensetracker.data.TrackerRepository.RepositoryCallback;
import java.util.List;

import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.Member;
import android.util.Log;

import java.util.ArrayList;

import com.example.expensetracker.calculator.CategorySummaryCalculator;
import com.example.expensetracker.calculator.CategorySummaryItem;
import com.example.expensetracker.calculator.DebtCalculator;
import com.example.expensetracker.calculator.DebtSummary;
import com.example.expensetracker.calculator.ExpenseFilterSorter;
import com.example.expensetracker.calculator.ExpenseSummary;
import com.example.expensetracker.calculator.ExpenseSummaryCalculator;
import com.example.expensetracker.ui.expense.ExpenseScreenState.ContentTab;

public class ExpenseScreenController {

    private final TrackerRepository trackerRepository;
    private String trackerId;
    private ExpenseListQuery expenseListQuery;
    private ExpenseScreenListener listener;
    private Tracker tracker;
    private List<Member> members;
    private List<Expense> expenses;
    private List<Category> categories;
    private int pendingLoads;
    private boolean hasLoadError;
    private String errorMessage;
    private ContentTab selectedTab;
    private final List<String> expandedCategoryIds = new ArrayList<>();


    public ExpenseScreenController(TrackerRepository trackerRepository) {
        this.trackerRepository = trackerRepository;
        this.expenseListQuery = new ExpenseListQuery(
                null,
                ExpenseListQuery.SortType.DATE_DESC
        );
        this.selectedTab = ContentTab.CATEGORIES;
    }

    public void setTrackerId(String trackerId) {
        this.trackerId = trackerId;
    }

    public void setExpenseListQuery(ExpenseListQuery query) {
        this.expenseListQuery = query;
    }

    public void setListener(ExpenseScreenListener listener) {
        this.listener = listener;
    }


    private void emitState(ExpenseScreenState state) {
        if (listener != null) {
            listener.onStateChanged(state);
        }
    }

    public void load() {
        if (trackerId == null || trackerId.isEmpty()) {
            Log.e("ExpenseScreenController", "Cannot load tracker: trackerId is null or empty");
            return;
        }

        startLoading();

        trackerRepository.loadTracker(trackerId, new RepositoryCallback<Tracker>() {
            @Override
            public void onSuccess(Tracker tracker) {
                ExpenseScreenController.this.tracker = tracker;
                onLoadFinished();
            }

            @Override
            public void onError(Exception exception) {
                onLoadError("Error loading tracker", exception);
            }
        });

        trackerRepository.loadParticipants(trackerId, new RepositoryCallback<List<Member>>() {
            @Override
            public void onSuccess(List<Member> members) {
                ExpenseScreenController.this.members = members;
                onLoadFinished();
            }

            @Override
            public void onError(Exception exception) {
                onLoadError("Error loading participants", exception);
            }
        });

        trackerRepository.loadExpenses(trackerId, new RepositoryCallback<List<Expense>>() {
            @Override
            public void onSuccess(List<Expense> expenses) {
                ExpenseScreenController.this.expenses = expenses;
                onLoadFinished();
            }

            @Override
            public void onError(Exception exception) {
                onLoadError("Error loading expenses", exception);
            }
        });

        trackerRepository.loadCategories(trackerId, new RepositoryCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> categories) {
                ExpenseScreenController.this.categories = categories;
                onLoadFinished();
            }

            @Override
            public void onError(Exception exception) {
                onLoadError("Error loading categories", exception);
            }
        });
    }


    private ExpenseScreenState buildState(boolean loading) {

        List<Expense> safeExpenses = expenses != null ? expenses : new ArrayList<>();
        List<Member> safeMembers = members != null ? members : new ArrayList<>();
        List<Category> safeCategories = categories != null ? categories : new ArrayList<>();

        ExpenseSummary expenseSummary =
                ExpenseSummaryCalculator.calculate(safeExpenses, safeMembers);

        DebtSummary debtSummary =
                DebtCalculator.calculate(safeExpenses, safeMembers);

        String selectedMemberFilter =
                expenseListQuery != null ? expenseListQuery.getMemberIdFilter() : null;

        List<Expense> filteredExpensesForCategories =
                ExpenseFilterSorter.apply(
                        safeExpenses,
                        new ExpenseListQuery(selectedMemberFilter, null)
                );

        List<Expense> visibleExpenses =
                ExpenseFilterSorter.apply(safeExpenses, expenseListQuery);

        List<CategorySummaryItem> categorySummary =
                CategorySummaryCalculator.calculate(
                        filteredExpensesForCategories,
                        safeCategories,
                        safeMembers
                );

        return new ExpenseScreenState(
                tracker,
                safeMembers,
                safeExpenses,
                safeCategories,
                expenseSummary,
                debtSummary,
                categorySummary,
                visibleExpenses,
                selectedTab,
                expenseListQuery != null ? expenseListQuery.getMemberIdFilter() : null,
                expenseListQuery != null ? expenseListQuery.getSortType() : null,
                new ArrayList<>(expandedCategoryIds),
                loading,
                errorMessage
        );
    }

    private void startLoading() {
        pendingLoads = 4;
        hasLoadError = false;
        errorMessage = null;
        emitCurrentState(true);
    }


    private void onLoadFinished() {
        pendingLoads--;

        boolean stillLoading = pendingLoads > 0;
        emitCurrentState(stillLoading);

    }


    private void onLoadError(String message, Exception exception) {
        hasLoadError = true;
        errorMessage = message;
        Log.e("ExpenseScreenController", message, exception);
        onLoadFinished();
    }

    public void setSelectedTab(ContentTab selectedTab) {
        this.selectedTab = selectedTab;
        emitCurrentState();
    }

    public void setMemberFilter(String memberId) {
        ExpenseListQuery.SortType currentSortType =
                expenseListQuery != null ? expenseListQuery.getSortType() : ExpenseListQuery.SortType.DATE_DESC;

        expenseListQuery = new ExpenseListQuery(memberId, currentSortType);
        emitCurrentState();
    }

    public void setSortType(ExpenseListQuery.SortType sortType) {
        String currentMemberFilter =
                expenseListQuery != null ? expenseListQuery.getMemberIdFilter() : null;

        expenseListQuery = new ExpenseListQuery(currentMemberFilter, sortType);
        emitCurrentState();
    }

    public void toggleCategoryExpanded(String categoryId) {
        if (categoryId == null) {
            return;
        }

        if (expandedCategoryIds.contains(categoryId)) {
            expandedCategoryIds.remove(categoryId);
        } else {
            expandedCategoryIds.add(categoryId);
        }
        emitCurrentState();
    }
    public void refresh() {
        load();
    }
    private void emitCurrentState() {
        emitCurrentState(false);
    }

    private void emitCurrentState(boolean loading) {
        emitState(buildState(loading));
    }

    public void updateExpense(
            String expenseId,
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date
    ) {

        if (trackerId == null) {
            return;
        }

        trackerRepository.updateExpense(
                trackerId,
                expenseId,
                description,
                amount,
                participantId,
                categoryId,
                date
        );

        refresh();
    }

    public void createExpense(
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date
    ) {
        if (trackerId == null || trackerId.isEmpty()) {
            return;
        }

        trackerRepository.createExpense(
                trackerId,
                description,
                amount,
                participantId,
                categoryId,
                date
        );

        refresh();
    }

    public void deleteExpense(String expenseId) {
        if (trackerId == null || trackerId.isEmpty() || expenseId == null || expenseId.isEmpty()) {
            return;
        }

        trackerRepository.deleteExpense(trackerId, expenseId);
        refresh();
    }

    public void updateTrackerName(String newName) {
        if (trackerId == null || trackerId.isEmpty()) {
            return;
        }

        trackerRepository.updateTrackerName(trackerId, newName);
        refresh();
    }

    public void updateTrackerClosed(boolean closed) {
        if (trackerId == null || trackerId.isEmpty()) {
            return;
        }

        trackerRepository.updateTrackerClosed(trackerId, closed);
        refresh();
    }
}
