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
import com.example.expensetracker.calculator.BalanceDetail;
import com.example.expensetracker.calculator.BalanceDetailCalculator;
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
    private ExpenseScreenState lastEmittedState;
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
        lastEmittedState = state;
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

        BalanceDetail balanceDetail =
                BalanceDetailCalculator.calculate(safeExpenses, safeMembers);

        String selectedMemberFilter =
                expenseListQuery != null ? expenseListQuery.getMemberIdFilter() : null;
        ExpenseListQuery.TypeFilter selectedTypeFilter =
                expenseListQuery != null ? expenseListQuery.getTypeFilter() : null;

        List<Expense> filteredExpensesForCategories =
                ExpenseFilterSorter.apply(
                        safeExpenses,
                        new ExpenseListQuery(selectedMemberFilter, selectedTypeFilter, null)
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
                balanceDetail,
                categorySummary,
                visibleExpenses,
                selectedTab,
                expenseListQuery != null ? expenseListQuery.getMemberIdFilter() : null,
                expenseListQuery != null ? expenseListQuery.getTypeFilter() : null,
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

        if (lastEmittedState != null) {
            emitState(new ExpenseScreenState(
                    lastEmittedState.tracker,
                    lastEmittedState.members,
                    lastEmittedState.expenses,
                    lastEmittedState.categories,
                    lastEmittedState.expenseSummary,
                    lastEmittedState.debtSummary,
                    lastEmittedState.balanceDetail,
                    lastEmittedState.categorySummary,
                    lastEmittedState.visibleExpenses,
                    selectedTab,
                    lastEmittedState.selectedMemberFilter,
                    lastEmittedState.selectedTypeFilter,
                    lastEmittedState.selectedSortType,
                    new ArrayList<>(expandedCategoryIds),
                    lastEmittedState.loading,
                    lastEmittedState.errorMessage
            ));
            return;
        }

        emitCurrentState();
    }

    public void setMemberFilter(String memberId) {
        ExpenseListQuery.SortType currentSortType =
                expenseListQuery != null ? expenseListQuery.getSortType() : ExpenseListQuery.SortType.DATE_DESC;
        ExpenseListQuery.TypeFilter currentTypeFilter =
                expenseListQuery != null ? expenseListQuery.getTypeFilter() : null;

        expenseListQuery = new ExpenseListQuery(memberId, currentTypeFilter, currentSortType);
        emitCurrentState();
    }

    public void setTypeFilter(ExpenseListQuery.TypeFilter typeFilter) {
        String currentMemberFilter =
                expenseListQuery != null ? expenseListQuery.getMemberIdFilter() : null;
        ExpenseListQuery.SortType currentSortType =
                expenseListQuery != null ? expenseListQuery.getSortType() : ExpenseListQuery.SortType.DATE_DESC;

        expenseListQuery = new ExpenseListQuery(currentMemberFilter, typeFilter, currentSortType);
        emitCurrentState();
    }

    public void setSortType(ExpenseListQuery.SortType sortType) {
        String currentMemberFilter =
                expenseListQuery != null ? expenseListQuery.getMemberIdFilter() : null;
        ExpenseListQuery.TypeFilter currentTypeFilter =
                expenseListQuery != null ? expenseListQuery.getTypeFilter() : null;

        expenseListQuery = new ExpenseListQuery(currentMemberFilter, currentTypeFilter, sortType);
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
        updateExpense(expenseId, description, amount, participantId, categoryId, date, false);
    }

    public void updateExpense(
            String expenseId,
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date,
            boolean individual
    ) {
        updateExpense(expenseId, description, amount, participantId, categoryId, date, individual, null);
    }

    public void updateExpense(
            String expenseId,
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date,
            boolean individual,
            RepositoryCallback<Void> callback
    ) {

        if (trackerId == null) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Tracker id is missing"));
            }
            return;
        }

        trackerRepository.updateExpense(
                trackerId,
                expenseId,
                description,
                amount,
                participantId,
                categoryId,
                date,
                individual,
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        refresh();
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (callback != null) {
                            callback.onError(exception);
                        }
                    }
                }
        );
    }

    public void createExpense(
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date
    ) {
        createExpense(description, amount, participantId, categoryId, date, false);
    }

    public void createExpense(
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date,
            boolean individual
    ) {
        createExpense(description, amount, participantId, categoryId, date, individual, null);
    }

    public void createExpense(
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date,
            boolean individual,
            RepositoryCallback<Void> callback
    ) {
        if (trackerId == null || trackerId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Tracker id is missing"));
            }
            return;
        }

        trackerRepository.createExpense(
                trackerId,
                description,
                amount,
                participantId,
                categoryId,
                date,
                individual,
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        refresh();
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (callback != null) {
                            callback.onError(exception);
                        }
                    }
                }
        );
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
