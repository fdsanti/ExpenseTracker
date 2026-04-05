package com.example.expensetracker.data;

import androidx.annotation.NonNull;

import com.example.expensetracker.model.Tracker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import com.example.expensetracker.model.Member;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.expensetracker.model.Expense;

import com.example.expensetracker.model.Category;
public class TrackerRepository {

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception exception);
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

                    long createdAt = 0L;
                    boolean closed = closedValue != null && closedValue;

                    Tracker tracker = new Tracker(
                            trackerId,
                            name,
                            createdAt,
                            closed
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

                        double amount = amountValue != null ? amountValue : 0;

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
                                date
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

    public void updateExpense(
            String trackerId,
            String expenseId,
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date
    ) {

        DatabaseReference expenseRef = getExpensesRef(trackerId).child(expenseId);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = formatter.format(new Date(date));

        expenseRef.child("description").setValue(description);
        expenseRef.child("amount").setValue(amount);
        expenseRef.child("participantId").setValue(participantId);
        expenseRef.child("categoryId").setValue(categoryId);
        expenseRef.child("date").setValue(dateString);
    }

    public void createExpense(
            String trackerId,
            String description,
            double amount,
            String participantId,
            String categoryId,
            long date
    ) {
        DatabaseReference expenseRef = getExpensesRef(trackerId).push();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = formatter.format(new Date(date));

        expenseRef.child("description").setValue(description);
        expenseRef.child("amount").setValue(amount);
        expenseRef.child("participantId").setValue(participantId);
        expenseRef.child("categoryId").setValue(categoryId);
        expenseRef.child("date").setValue(dateString);
    }

    public void deleteExpense(String trackerId, String expenseId) {
        getExpensesRef(trackerId)
                .child(expenseId)
                .removeValue();
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
        database.child("trackers_v2")
                .child(trackerId)
                .child("meta")
                .child("closed")
                .setValue(closed);

        database.child("home_index")
                .child(trackerId)
                .child("closed")
                .setValue(closed);
    }
}
