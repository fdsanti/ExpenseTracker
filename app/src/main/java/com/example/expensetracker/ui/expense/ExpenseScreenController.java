package com.example.expensetracker.ui.expense;

import com.example.expensetracker.calculator.ExpenseListQuery;
import com.example.expensetracker.data.TrackerRepository;
import com.example.expensetracker.model.Tracker;
import com.example.expensetracker.data.TrackerRepository.RepositoryCallback;
import java.util.List;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

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
import com.example.expensetracker.calculator.TotalTrend;
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
    private Tracker previousMonthlyTracker;
    private List<Expense> previousMonthlyExpenses;
    private TotalTrend totalTrend;
    private int pendingLoads;
    private boolean hasLoadError;
    private String errorMessage;
    private ContentTab selectedTab;
    private ExpenseScreenState lastEmittedState;
    private int loadGeneration;
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

        int generation = ++loadGeneration;
        startLoading();

        trackerRepository.loadTracker(trackerId, new RepositoryCallback<Tracker>() {
            @Override
            public void onSuccess(Tracker tracker) {
                if (!isCurrentLoad(generation)) {
                    return;
                }

                ExpenseScreenController.this.tracker = tracker;
                if (shouldLoadPreviousMonthlyTrend(tracker)) {
                    loadPreviousMonthlyTrend(generation);
                }
                onLoadFinished(generation);
            }

            @Override
            public void onError(Exception exception) {
                onLoadError(generation, "Error loading tracker", exception);
            }
        });

        trackerRepository.loadParticipants(trackerId, new RepositoryCallback<List<Member>>() {
            @Override
            public void onSuccess(List<Member> members) {
                if (!isCurrentLoad(generation)) {
                    return;
                }

                ExpenseScreenController.this.members = members;
                onLoadFinished(generation);
            }

            @Override
            public void onError(Exception exception) {
                onLoadError(generation, "Error loading participants", exception);
            }
        });

        trackerRepository.loadExpenses(trackerId, new RepositoryCallback<List<Expense>>() {
            @Override
            public void onSuccess(List<Expense> expenses) {
                if (!isCurrentLoad(generation)) {
                    return;
                }

                ExpenseScreenController.this.expenses = expenses;
                onLoadFinished(generation);
            }

            @Override
            public void onError(Exception exception) {
                onLoadError(generation, "Error loading expenses", exception);
            }
        });

        trackerRepository.loadCategories(trackerId, new RepositoryCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> categories) {
                if (!isCurrentLoad(generation)) {
                    return;
                }

                ExpenseScreenController.this.categories = categories;
                onLoadFinished(generation);
            }

            @Override
            public void onError(Exception exception) {
                onLoadError(generation, "Error loading categories", exception);
            }
        });
    }


    private ExpenseScreenState buildState(boolean loading) {

        List<Expense> safeExpenses = expenses != null ? expenses : new ArrayList<>();
        List<Member> safeMembers = members != null ? members : new ArrayList<>();
        List<Category> safeCategories = categories != null ? categories : new ArrayList<>();

        ExpenseSummary expenseSummary =
                ExpenseSummaryCalculator.calculate(safeExpenses, safeMembers);
        totalTrend = calculateTotalTrend(
                tracker,
                safeExpenses,
                previousMonthlyTracker,
                previousMonthlyExpenses
        );

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
                totalTrend,
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
        previousMonthlyTracker = null;
        previousMonthlyExpenses = null;
        totalTrend = null;
        emitCurrentState(true);
    }


    private void onLoadFinished(int generation) {
        if (!isCurrentLoad(generation)) {
            return;
        }

        pendingLoads--;

        boolean stillLoading = pendingLoads > 0;
        emitCurrentState(stillLoading);

    }


    private void onLoadError(int generation, String message, Exception exception) {
        if (!isCurrentLoad(generation)) {
            return;
        }

        hasLoadError = true;
        errorMessage = message;
        Log.e("ExpenseScreenController", message, exception);
        onLoadFinished(generation);
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
                    lastEmittedState.totalTrend,
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

    private void loadPreviousMonthlyTrend(int generation) {
        trackerRepository.loadPreviousMonthlyTrackerExpenses(
                tracker,
                new RepositoryCallback<TrackerRepository.PreviousTrackerExpenses>() {
                    @Override
                    public void onSuccess(TrackerRepository.PreviousTrackerExpenses result) {
                        if (!isCurrentLoad(generation)) {
                            return;
                        }

                        if (result == null) {
                            previousMonthlyTracker = null;
                            previousMonthlyExpenses = null;
                        } else {
                            previousMonthlyTracker = result.getTracker();
                            previousMonthlyExpenses = result.getExpenses();
                        }

                        emitCurrentState(lastEmittedState != null && lastEmittedState.loading);
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (!isCurrentLoad(generation)) {
                            return;
                        }

                        Log.e("ExpenseScreenController", "Error loading previous monthly tracker", exception);
                        previousMonthlyTracker = null;
                        previousMonthlyExpenses = null;
                        emitCurrentState(lastEmittedState != null && lastEmittedState.loading);
                    }
                }
        );
    }

    private boolean isCurrentLoad(int generation) {
        return generation == loadGeneration;
    }

    private boolean shouldLoadPreviousMonthlyTrend(Tracker tracker) {
        return tracker != null
                && tracker.getId() != null
                && tracker.getCreatedAt() > 0L
                && tracker.isMonthly();
    }

    private TotalTrend calculateTotalTrend(
            Tracker currentTracker,
            List<Expense> currentExpenses,
            Tracker previousTracker,
            List<Expense> previousExpenses
    ) {
        if (currentTracker == null
                || previousTracker == null
                || currentExpenses == null
                || previousExpenses == null
                || !currentTracker.isMonthly()
                || !previousTracker.isMonthly()) {
            return null;
        }

        if (daysBetween(previousTracker.getCreatedAt(), currentTracker.getCreatedAt()) < 20) {
            return null;
        }

        double currentTotal;
        double previousTotal;

        if (currentTracker.isClosed()) {
            currentTotal = sumExpenses(currentExpenses);
            previousTotal = sumExpenses(previousExpenses);
        } else {
            int trackerDay = TrackerDateUtils.getTrackerDay(currentTracker);
            if (trackerDay <= 0) {
                return null;
            }

            currentTotal = sumThroughTrackerDay(currentTracker, currentExpenses, trackerDay);
            previousTotal = sumThroughTrackerDay(previousTracker, previousExpenses, trackerDay);
        }

        if (previousTotal <= 0d || currentTotal == previousTotal) {
            return null;
        }

        int percentage = (int) Math.round(Math.abs((currentTotal - previousTotal) * 100d / previousTotal));
        if (percentage == 0) {
            return null;
        }

        return new TotalTrend(
                percentage,
                currentTotal > previousTotal ? TotalTrend.Direction.UP : TotalTrend.Direction.DOWN
        );
    }

    private double sumExpenses(List<Expense> expenses) {
        double total = 0d;

        if (expenses == null) {
            return total;
        }

        for (Expense expense : expenses) {
            if (expense != null) {
                total += expense.getAmount();
            }
        }

        return total;
    }

    private double sumThroughTrackerDay(Tracker tracker, List<Expense> expenses, int trackerDay) {
        long start = startOfDay(tracker.getCreatedAt());
        long end = start + TimeUnit.DAYS.toMillis(trackerDay) - 1L;
        double total = 0d;

        for (Expense expense : expenses) {
            if (expense == null) {
                continue;
            }

            long expenseDate = startOfDay(expense.getDate());
            if (expenseDate >= start && expenseDate <= end) {
                total += expense.getAmount();
            }
        }

        return total;
    }

    private long daysBetween(long startMillis, long endMillis) {
        long start = startOfDay(startMillis);
        long end = startOfDay(endMillis);
        if (end <= start) {
            return 0L;
        }
        return TimeUnit.MILLISECONDS.toDays(end - start);
    }

    private long startOfDay(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
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

    public void createCategory(String name, RepositoryCallback<Category> callback) {
        if (trackerId == null || trackerId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Tracker id is missing"));
            }
            return;
        }

        int nextOrder = categories != null ? categories.size() + 1 : 1;
        trackerRepository.createCategory(trackerId, name, nextOrder, new RepositoryCallback<Category>() {
            @Override
            public void onSuccess(Category result) {
                refresh();
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onError(Exception exception) {
                if (callback != null) {
                    callback.onError(exception);
                }
            }
        });
    }

    public void updateCategoryName(String categoryId, String name, RepositoryCallback<Void> callback) {
        if (trackerId == null || trackerId.isEmpty() || categoryId == null || categoryId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Category id is missing"));
            }
            return;
        }

        trackerRepository.updateCategoryName(trackerId, categoryId, name, new RepositoryCallback<Void>() {
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
        });
    }

    public void reorderCategories(List<Category> orderedCategories, RepositoryCallback<Void> callback) {
        if (trackerId == null || trackerId.isEmpty() || orderedCategories == null) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Category order is missing"));
            }
            return;
        }

        trackerRepository.reorderCategories(trackerId, orderedCategories, new RepositoryCallback<Void>() {
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
        });
    }

    public void deleteCategory(String categoryId, int remainingCategoryCount, RepositoryCallback<Void> callback) {
        if (trackerId == null || trackerId.isEmpty() || categoryId == null || categoryId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Category id is missing"));
            }
            return;
        }

        trackerRepository.deleteCategory(trackerId, categoryId, remainingCategoryCount, new RepositoryCallback<Void>() {
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
        });
    }
}
