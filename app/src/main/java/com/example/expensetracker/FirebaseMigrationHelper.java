package com.example.expensetracker;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Migra la estructura legacy:
 * - allTables/{trackerId}
 * - settings/{trackerId}
 * - categories/{trackerId}
 * - {trackerId} -> lista de gastos
 *
 * Hacia una nueva estructura duplicada en:
 * - trackers_v2/{trackerId}/meta
 * - trackers_v2/{trackerId}/participants
 * - trackers_v2/{trackerId}/categories
 * - trackers_v2/{trackerId}/expenses
 * - trackers_v2/{trackerId}/summary
 *
 * No reemplaza nada viejo.
 */
public final class FirebaseMigrationHelper {

    public interface MigrationCallback {
        void onSuccess(int migratedTrackers);
        void onError(@NonNull Exception e);
    }

    private FirebaseMigrationHelper() {}

    public static void migrateLegacyToTrackersV2(@NonNull MigrationCallback callback) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        rootRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Exception e = task.getException() != null
                        ? task.getException()
                        : new IllegalStateException("No se pudo leer la base para migrar");
                callback.onError(e);
                return;
            }

            try {
                DataSnapshot root = task.getResult();
                DataSnapshot allTablesSnapshot = root.child("allTables");
                DataSnapshot settingsSnapshot = root.child("settings");
                DataSnapshot categoriesSnapshot = root.child("categories");

                Map<String, Object> updates = new HashMap<>();
                int migratedCount = 0;

                for (DataSnapshot trackerSnapshot : allTablesSnapshot.getChildren()) {
                    String trackerId = trackerSnapshot.getKey();
                    if (trackerId == null) continue;

                    Boolean closedValue = trackerSnapshot.child("cerrado").getValue(Boolean.class);
                    boolean closed = closedValue != null && closedValue;

                    Map<String, Object> meta = new LinkedHashMap<>();
                    meta.put("legacyId", trackerId);
                    meta.put("name", safeString(trackerSnapshot.child("tableDescription").getValue()));
                    meta.put("createdAt", safeString(trackerSnapshot.child("creationDate").getValue()));
                    meta.put("status", closed ? "closed" : "open");
                    meta.put("closed", closed);
                    meta.put("version", 2);
                    meta.put("migratedFrom", "legacy-root-structure");

                    updates.put("trackers_v2/" + trackerId + "/meta", meta);

                    // Participants
                    DataSnapshot settingsForTracker = settingsSnapshot.child(trackerId);
                    LinkedHashMap<String, Object> participants = new LinkedHashMap<>();
                    Map<String, String> participantIdByName = new HashMap<>();
                    int nextParticipantOrder = 1;

                    String name1 = safeTrimmedString(settingsForTracker.child("name1").getValue());
                    Object income1 = settingsForTracker.child("sueldo1").getValue();
                    if (!name1.isEmpty()) {
                        String participantId = "p1";
                        participants.put(participantId, buildParticipantMap(name1, income1, 1));
                        participantIdByName.put(name1, participantId);
                        nextParticipantOrder = 2;
                    }

                    String name2 = safeTrimmedString(settingsForTracker.child("name2").getValue());
                    Object income2 = settingsForTracker.child("sueldo2").getValue();
                    if (!name2.isEmpty()) {
                        String participantId = participants.containsKey("p1") ? "p2" : "p1";
                        int order = participants.containsKey("p1") ? 2 : 1;
                        participants.put(participantId, buildParticipantMap(name2, income2, order));
                        participantIdByName.put(name2, participantId);
                        nextParticipantOrder = order + 1;
                    }

                    // Categories
                    DataSnapshot categoriesForTracker = categoriesSnapshot.child(trackerId);
                    LinkedHashMap<String, Object> categoryMap = new LinkedHashMap<>();
                    Map<String, String> categoryIdByName = new HashMap<>();
                    int nextCategoryOrder = 1;

                    for (DataSnapshot categorySnapshot : categoriesForTracker.getChildren()) {
                        String categoryName = safeTrimmedString(categorySnapshot.child("name").getValue());
                        if (categoryName.isEmpty()) continue;
                        String categoryId = "c" + nextCategoryOrder;
                        categoryMap.put(categoryId, buildCategoryMap(categoryName, nextCategoryOrder));
                        categoryIdByName.put(categoryName, categoryId);
                        nextCategoryOrder++;
                    }

                    // Expenses
                    DataSnapshot expensesLegacy = root.child(trackerId);
                    LinkedHashMap<String, Object> expenses = new LinkedHashMap<>();
                    int expenseCounter = 1;
                    long totalAmount = 0L;

                    for (DataSnapshot expenseSnapshot : expensesLegacy.getChildren()) {
                        if (!expenseSnapshot.exists()) continue;
                        if (expenseSnapshot.getValue() == null) continue;

                        String who = safeTrimmedString(expenseSnapshot.child("Who").getValue());
                        if (!who.isEmpty() && !participantIdByName.containsKey(who)) {
                            String participantId = "p" + nextParticipantOrder;
                            participants.put(participantId, buildParticipantMap(who, null, nextParticipantOrder));
                            participantIdByName.put(who, participantId);
                            nextParticipantOrder++;
                        }

                        String categoryName = safeTrimmedString(expenseSnapshot.child("Category").getValue());
                        if (!categoryName.isEmpty() && !categoryIdByName.containsKey(categoryName)) {
                            String categoryId = "c" + nextCategoryOrder;
                            categoryMap.put(categoryId, buildCategoryMap(categoryName, nextCategoryOrder));
                            categoryIdByName.put(categoryName, categoryId);
                            nextCategoryOrder++;
                        }

                        Object amountValue = expenseSnapshot.child("Value").getValue();
                        long amount = toLong(amountValue);
                        totalAmount += amount;

                        Map<String, Object> expense = new LinkedHashMap<>();
                        expense.put("legacyIndex", expenseSnapshot.getKey());
                        expense.put("description", safeString(expenseSnapshot.child("Description").getValue()));
                        expense.put("amount", amount);
                        expense.put("date", safeString(expenseSnapshot.child("Date").getValue()));
                        expense.put("participantId", who.isEmpty() ? null : participantIdByName.get(who));
                        expense.put("participantNameLegacy", who.isEmpty() ? null : who);
                        expense.put("categoryId", categoryName.isEmpty() ? null : categoryIdByName.get(categoryName));
                        expense.put("categoryNameLegacy", categoryName.isEmpty() ? null : categoryName);
                        expense.put("notes", "");
                        expense.put("deleted", false);

                        expenses.put("e" + expenseCounter, expense);
                        expenseCounter++;
                    }

                    updates.put("trackers_v2/" + trackerId + "/participants", participants);
                    updates.put("trackers_v2/" + trackerId + "/categories", categoryMap);
                    updates.put("trackers_v2/" + trackerId + "/expenses", expenses);

                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("expenseCount", expenses.size());
                    summary.put("totalAmount", totalAmount);
                    summary.put("participantCount", participants.size());
                    summary.put("categoryCount", categoryMap.size());
                    updates.put("trackers_v2/" + trackerId + "/summary", summary);

                    migratedCount++;
                }

                int finalMigratedCount = migratedCount;
                rootRef.updateChildren(updates).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        callback.onSuccess(finalMigratedCount);
                    } else {
                        Exception e = updateTask.getException() != null
                                ? updateTask.getException()
                                : new IllegalStateException("La migración falló al guardar trackers_v2");
                        callback.onError(e);
                    }
                });

            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    @NonNull
    private static Map<String, Object> buildParticipantMap(@NonNull String name, Object income, int order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("income", income);
        map.put("order", order);
        map.put("active", true);
        return map;
    }

    @NonNull
    private static Map<String, Object> buildCategoryMap(@NonNull String name, int order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("order", order);
        map.put("active", true);
        map.put("system", false);
        return map;
    }

    @NonNull
    private static String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @NonNull
    private static String safeTrimmedString(Object value) {
        return safeString(value).trim();
    }

    private static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Double) return Math.round((Double) value);
        if (value instanceof Float) return Math.round((Float) value);
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
