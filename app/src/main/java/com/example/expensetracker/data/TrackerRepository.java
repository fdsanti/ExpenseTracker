package com.example.expensetracker.data;

import androidx.annotation.NonNull;

import com.example.expensetracker.model.Tracker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.expensetracker.model.Member;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.expensetracker.model.Expense;

import com.example.expensetracker.model.Category;
public class TrackerRepository {
    private static final int SUMMARY_VERSION = 1;

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception exception);
    }

    public static class PreviousTrackerExpenses {
        private final Tracker tracker;
        private final List<Expense> expenses;

        public PreviousTrackerExpenses(Tracker tracker, List<Expense> expenses) {
            this.tracker = tracker;
            this.expenses = expenses;
        }

        public Tracker getTracker() {
            return tracker;
        }

        public List<Expense> getExpenses() {
            return expenses;
        }
    }

    private final DatabaseReference database;

    public TrackerRepository() {
        database = FirebaseDatabase.getInstance().getReference();
    }

    public DatabaseReference getTrackerRef(String trackerId) {
        return database
                .child("trackers_v2")
                .child(trackerId);
    }

    public void loadTracker(String trackerId, RepositoryCallback<Tracker> callback) {
        getMetaRef(trackerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    String name = snapshot.child("name").getValue(String.class);
                    String createdAtString = snapshot.child("createdAt").getValue(String.class);
                    Boolean closedValue = snapshot.child("closed").getValue(Boolean.class);
                    String type = snapshot.child("type").getValue(String.class);
                    String monthKey = snapshot.child("monthKey").getValue(String.class);

                    long createdAt = 0L;
                    boolean closed = closedValue != null && closedValue;

                    if (createdAtString != null && !createdAtString.trim().isEmpty()) {
                        try {
                            Date parsedCreatedAt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .parse(createdAtString);
                            if (parsedCreatedAt != null) {
                                createdAt = parsedCreatedAt.getTime();
                            }
                        } catch (Exception ignored) {}
                    }

                    Tracker tracker = new Tracker(
                            trackerId,
                            name,
                            createdAt,
                            closed,
                            type,
                            monthKey
                    );


                    callback.onSuccess(tracker);

                } catch (Exception e) {
                    callback.onError(e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    public void loadPreviousMonthlyTrackerExpenses(
            Tracker currentTracker,
            RepositoryCallback<PreviousTrackerExpenses> callback
    ) {
        if (currentTracker == null
                || currentTracker.getId() == null
                || currentTracker.getCreatedAt() <= 0L
                || !currentTracker.isMonthly()) {
            callback.onSuccess(null);
            return;
        }

        database.child("home_index").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    String previousTrackerId = null;
                    long previousCreatedAt = 0L;
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    for (DataSnapshot child : snapshot.getChildren()) {
                        String trackerId = child.child("trackerId").getValue(String.class);
                        if (trackerId == null || trackerId.equals(currentTracker.getId())) {
                            continue;
                        }

                        String type = child.child("type").getValue(String.class);
                        if (Tracker.TYPE_MANUAL.equals(type)) {
                            continue;
                        }

                        String createdAtString = child.child("createdAt").getValue(String.class);
                        long createdAt = 0L;
                        try {
                            Date parsedDate = formatter.parse(createdAtString);
                            if (parsedDate != null) {
                                createdAt = parsedDate.getTime();
                            }
                        } catch (Exception ignored) {
                        }

                        if (createdAt > 0L
                                && createdAt < currentTracker.getCreatedAt()
                                && createdAt > previousCreatedAt) {
                            previousCreatedAt = createdAt;
                            previousTrackerId = trackerId;
                        }
                    }

                    if (previousTrackerId == null) {
                        callback.onSuccess(null);
                        return;
                    }

                    loadTracker(previousTrackerId, new RepositoryCallback<Tracker>() {
                        @Override
                        public void onSuccess(Tracker previousTracker) {
                            loadExpenses(previousTracker.getId(), new RepositoryCallback<List<Expense>>() {
                                @Override
                                public void onSuccess(List<Expense> expenses) {
                                    callback.onSuccess(new PreviousTrackerExpenses(previousTracker, expenses));
                                }

                                @Override
                                public void onError(Exception exception) {
                                    callback.onError(exception);
                                }
                            });
                        }

                        @Override
                        public void onError(Exception exception) {
                            callback.onError(exception);
                        }
                    });
                } catch (Exception e) {
                    callback.onError(e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    public DatabaseReference getExpensesRef(String trackerId) {
        return getTrackerRef(trackerId).child("expenses");
    }

    public DatabaseReference getParticipantsRef(String trackerId) {
        return getTrackerRef(trackerId).child("participants");
    }

    public DatabaseReference getMetaRef(String trackerId) {
        return getTrackerRef(trackerId).child("meta");
    }

    public void loadParticipants(String trackerId, RepositoryCallback<List<Member>> callback) {
        getParticipantsRef(trackerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    List<Member> result = new ArrayList<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        String id = child.getKey();
                        String name = child.child("name").getValue(String.class);
                        Object incomeValue = child.child("income").getValue();

                        double salary = 0;

                        if (incomeValue instanceof Long) {
                            salary = ((Long) incomeValue).doubleValue();
                        } else if (incomeValue instanceof Integer) {
                            salary = ((Integer) incomeValue).doubleValue();
                        } else if (incomeValue instanceof Double) {
                            salary = (Double) incomeValue;
                        }

                        result.add(new Member(id, name, salary));

                    }

                    callback.onSuccess(result);

                } catch (Exception e) {
                    callback.onError(e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    public void loadExpenses(String trackerId, RepositoryCallback<List<Expense>> callback) {

        getExpensesRef(trackerId).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {

                    List<Expense> result = new ArrayList<>();

                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    for (DataSnapshot child : snapshot.getChildren()) {

                        String id = child.getKey();

                        String description = child.child("description").getValue(String.class);
                        Double amountValue = child.child("amount").getValue(Double.class);
                        String categoryId = child.child("categoryId").getValue(String.class);

                        // 🔥 fallback para trackers viejos
                        if (categoryId == null || categoryId.isEmpty()) {
                            categoryId = "otros";
                        }
                        String participantId = child.child("participantId").getValue(String.class);
                        String dateString = child.child("date").getValue(String.class);
                        Boolean individualValue = child.child("individual").getValue(Boolean.class);

                        double amount = amountValue != null ? amountValue : 0;
                        boolean individual = individualValue != null && individualValue;

                        long date = 0;

                        try {
                            if (dateString != null) {
                                Date parsedDate = formatter.parse(dateString);
                                if (parsedDate != null) {
                                    date = parsedDate.getTime();
                                }
                            }
                        } catch (Exception ignored) {}

                        Expense expense = new Expense(
                                id,
                                description,
                                amount,
                                categoryId,
                                participantId,
                                date,
                                individual
                        );

                        result.add(expense);
                    }

                    callback.onSuccess(result);

                } catch (Exception e) {
                    callback.onError(e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    public void loadCategories(String trackerId, RepositoryCallback<List<Category>> callback) {
        getCategoriesRef(trackerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    List<Category> result = new ArrayList<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        String id = child.getKey();
                        String name = child.child("name").getValue(String.class);
                        Boolean activeValue = child.child("active").getValue(Boolean.class);
                        Long orderValue = child.child("order").getValue(Long.class);

                        boolean active = activeValue != null && activeValue;
                        int order = orderValue != null ? orderValue.intValue() : 0;

                        result.add(new Category(id, name, active, order));
                    }

                    Collections.sort(result, new Comparator<Category>() {
                        @Override
                        public int compare(Category category1, Category category2) {
                            int orderCompare = Integer.compare(category1.getOrder(), category2.getOrder());
                            if (orderCompare != 0) {
                                return orderCompare;
                            }

                            String name1 = category1.getName() != null ? category1.getName() : "";
                            String name2 = category2.getName() != null ? category2.getName() : "";
                            return name1.compareToIgnoreCase(name2);
                        }
                    });

                    callback.onSuccess(result);

                } catch (Exception e) {
                    callback.onError(e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    public DatabaseReference getCategoriesRef(String trackerId) {
        return getTrackerRef(trackerId).child("categories");
    }

    public void createCategory(String trackerId, String name, int order, RepositoryCallback<Category> callback) {
        String categoryId = getCategoriesRef(trackerId).push().getKey();

        if (categoryId == null) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Could not create category id"));
            }
            return;
        }

        Map<String, Object> categoryValues = new HashMap<>();
        categoryValues.put("name", name);
        categoryValues.put("order", order);
        categoryValues.put("active", true);
        categoryValues.put("system", false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("trackers_v2/" + trackerId + "/categories/" + categoryId, categoryValues);
        updates.put("trackers_v2/" + trackerId + "/summary/categoryCount", order);

        database.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(new Category(categoryId, name, true, order));
                    }
                })
                .addOnFailureListener(exception -> {
                    if (callback != null) {
                        callback.onError(exception);
                    }
                });
    }

    public void updateCategoryName(String trackerId, String categoryId, String name, RepositoryCallback<Void> callback) {
        getCategoriesRef(trackerId)
                .child(categoryId)
                .child("name")
                .setValue(name)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(exception -> {
                    if (callback != null) {
                        callback.onError(exception);
                    }
                });
    }

    public void reorderCategories(String trackerId, List<Category> categories, RepositoryCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();

        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            updates.put("trackers_v2/" + trackerId + "/categories/" + category.getId() + "/order", i + 1);
        }

        database.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(exception -> {
                    if (callback != null) {
                        callback.onError(exception);
                    }
                });
    }

    public void deleteCategory(String trackerId, String categoryId, int remainingCategoryCount, RepositoryCallback<Void> callback) {
        if (DefaultCategories.OTHERS_ID.equals(categoryId)) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Otros cannot be deleted"));
            }
            return;
        }

        getExpensesRef(trackerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("trackers_v2/" + trackerId + "/categories/" + categoryId, null);
                updates.put("trackers_v2/" + trackerId + "/summary/categoryCount", remainingCategoryCount);

                for (DataSnapshot expenseSnapshot : snapshot.getChildren()) {
                    String expenseCategoryId = expenseSnapshot.child("categoryId").getValue(String.class);
                    String expenseId = expenseSnapshot.getKey();

                    if (expenseId != null && categoryId.equals(expenseCategoryId)) {
                        updates.put(
                                "trackers_v2/" + trackerId + "/expenses/" + expenseId + "/categoryId",
                                DefaultCategories.OTHERS_ID
                        );
                    }
                }

                database.updateChildren(updates)
                        .addOnSuccessListener(unused -> {
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        })
                        .addOnFailureListener(exception -> {
                            if (callback != null) {
                                callback.onError(exception);
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }

    public void updateExpense(
            String trackerId,
            String expenseId,
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date
    ) {
        updateExpense(trackerId, expenseId, description, amount, participantId, categoryId, date, false);
    }

    public void updateExpense(
            String trackerId,
            String expenseId,
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date,
            boolean individual
    ) {
        updateExpense(trackerId, expenseId, description, amount, participantId, categoryId, date, individual, null);
    }

    public void updateExpense(
            String trackerId,
            String expenseId,
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date,
            boolean individual,
            RepositoryCallback<Void> callback
    ) {

        DatabaseReference expenseRef = getExpensesRef(trackerId).child(expenseId);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = formatter.format(new Date(date));

        Map<String, Object> updates = new HashMap<>();
        updates.put("description", description);
        updates.put("amount", amount);
        updates.put("participantId", participantId);
        updates.put("categoryId", categoryId);
        updates.put("date", dateString);
        updates.put("individual", individual);

        expenseRef.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    recalculateTrackerSummary(trackerId, callback);
                })
                .addOnFailureListener(exception -> {
                    if (callback != null) {
                        callback.onError(exception);
                    }
                });
    }

    public void createExpense(
            String trackerId,
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date
    ) {
        createExpense(trackerId, description, amount, participantId, categoryId, date, false);
    }

    public void createExpense(
            String trackerId,
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date,
            boolean individual
    ) {
        createExpense(trackerId, description, amount, participantId, categoryId, date, individual, null);
    }

    public void createExpense(
            String trackerId,
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date,
            boolean individual,
            RepositoryCallback<Void> callback
    ) {
        DatabaseReference expenseRef = getExpensesRef(trackerId).push();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = formatter.format(new Date(date));

        Map<String, Object> expenseValues = new HashMap<>();
        expenseValues.put("description", description);
        expenseValues.put("amount", amount);
        expenseValues.put("participantId", participantId);
        expenseValues.put("categoryId", categoryId);
        expenseValues.put("date", dateString);
        expenseValues.put("individual", individual);

        expenseRef.updateChildren(expenseValues)
                .addOnSuccessListener(unused -> {
                    recalculateTrackerSummary(trackerId, callback);
                })
                .addOnFailureListener(exception -> {
                    if (callback != null) {
                        callback.onError(exception);
                    }
                });
    }

    public void deleteExpense(String trackerId, String expenseId) {
        getExpensesRef(trackerId)
                .child(expenseId)
                .removeValue()
                .addOnSuccessListener(unused -> recalculateTrackerSummary(trackerId, null));
    }

    private void recalculateTrackerSummary(String trackerId, RepositoryCallback<Void> callback) {
        if (trackerId == null || trackerId.isEmpty()) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Tracker id is missing"));
            }
            return;
        }

        getTrackerRef(trackerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    double totalAmount = 0d;
                    long expenseCount = 0L;

                    for (DataSnapshot expenseSnapshot : snapshot.child("expenses").getChildren()) {
                        totalAmount += safeDouble(expenseSnapshot.child("amount"), 0d);
                        expenseCount++;
                    }

                    long participantCount = snapshot.child("participants").getChildrenCount();
                    long categoryCount = snapshot.child("categories").getChildrenCount();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("trackers_v2/" + trackerId + "/summary/expenseCount", expenseCount);
                    updates.put("trackers_v2/" + trackerId + "/summary/totalAmount", totalAmount);
                    updates.put("trackers_v2/" + trackerId + "/summary/participantCount", participantCount);
                    updates.put("trackers_v2/" + trackerId + "/summary/categoryCount", categoryCount);
                    updates.put("trackers_v2/" + trackerId + "/summary/version", SUMMARY_VERSION);
                    updates.put("home_index/" + trackerId + "/totalAmount", totalAmount);
                    updates.put("home_index/" + trackerId + "/summaryVersion", SUMMARY_VERSION);

                    database.updateChildren(updates)
                            .addOnSuccessListener(unused -> {
                                if (callback != null) {
                                    callback.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(exception -> {
                                if (callback != null) {
                                    callback.onError(exception);
                                }
                            });
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onError(error.toException());
                }
            }
        });
    }

    private double safeDouble(DataSnapshot snapshot, double fallback) {
        Object value = snapshot.getValue();
        if (value == null) return fallback;
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Double) return (Double) value;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    public void updateTrackerName(String trackerId, String newName) {
        database.child("trackers_v2")
                .child(trackerId)
                .child("meta")
                .child("name")
                .setValue(newName);

        database.child("home_index")
                .child(trackerId)
                .child("name")
                .setValue(newName);
    }

    public void updateTrackerClosed(String trackerId, boolean closed) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("trackers_v2/" + trackerId + "/meta/closed", closed);
        updates.put("home_index/" + trackerId + "/closed", closed);

        database.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    if (closed) {
                        recalculateTrackerSummary(trackerId, null);
                    }
                });
    }
}
