package com.example.expensetracker;

import androidx.annotation.NonNull;

import android.util.Log;

import com.example.expensetracker.data.DefaultCategories;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeFirebaseV2Repository {

    private static final String TAG = "HomeFirebaseV2Repo";
    private static final int SUMMARY_VERSION = 1;

    public interface LoadCallback {
        void onSuccess();
        void onError(@NonNull Exception e);
    }

    public static final String ROOT = "home_index";

    public void loadHomeData(@NonNull LoadCallback callback) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child(ROOT);

        rootRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    callback.onError(task.getException() != null ? task.getException() : new Exception("No se pudo leer trackers_v2"));
                    return;
                }

                try {
                    ArrayList<HomeCard> tempHCArray = new ArrayList<>();
                    HCardDB.clearMap();
                    SettingsDB.getHashMap().clear();

                    for (DataSnapshot trackerSnapshot : task.getResult().getChildren()) {

                        String trackerId = safeString(trackerSnapshot.child("trackerId"), "");
                        String name = safeString(trackerSnapshot.child("name"), "Sin nombre");
                        String createdAt = safeString(trackerSnapshot.child("createdAt"), LocalDate.now().toString());
                        boolean closed = Boolean.TRUE.equals(trackerSnapshot.child("closed").getValue(Boolean.class));
                        String type = safeString(trackerSnapshot.child("type"), HomeCard.TYPE_MONTHLY);
                        String monthKey = safeString(trackerSnapshot.child("monthKey"), null);
                        double totalAmount = safeDouble(trackerSnapshot.child("totalAmount"), 0d);
                        int summaryVersion = trackerSnapshot.child("totalAmount").exists()
                                ? safeInt(trackerSnapshot.child("summaryVersion"), 0)
                                : 0;

                        LocalDate creationDate;
                        try {
                            creationDate = LocalDate.parse(createdAt);
                        } catch (Exception e) {
                            creationDate = LocalDate.now();
                        }

                        boolean isSetupComplete = Boolean.TRUE.equals(trackerSnapshot.child("isSetupComplete").getValue(Boolean.class));
                        HomeCard tempCard = HomeCard.fromTrackerId(
                                trackerId,
                                creationDate,
                                name,
                                closed,
                                isSetupComplete,
                                type,
                                monthKey,
                                totalAmount,
                                summaryVersion
                        );
                        tempHCArray.add(tempCard);
                    }

                    DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
                    ensureCurrentMonthlyTracker(databaseRef, tempHCArray, new LoadCallback() {
                        @Override
                        public void onSuccess() {
                            hydrateHomeSummaries(databaseRef, tempHCArray, new LoadCallback() {
                                @Override
                                public void onSuccess() {
                                    Collections.sort(tempHCArray, new HomeCardSortDate());
                                    for (HomeCard hc : tempHCArray) {
                                        HCardDB.addExpense(hc.getTableID(), hc);
                                    }

                                    callback.onSuccess();
                                }

                                @Override
                                public void onError(@NonNull Exception e) {
                                    callback.onError(e);
                                }
                            });
                        }

                        @Override
                        public void onError(@NonNull Exception e) {
                            callback.onError(e);
                        }
                    });
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    private void hydrateHomeSummaries(
            @NonNull DatabaseReference rootRef,
            @NonNull ArrayList<HomeCard> cards,
            @NonNull LoadCallback callback
    ) {
        ArrayList<HomeCard> cardsToHydrate = new ArrayList<>();
        HomeCard currentMonthly = findCurrentMonthly(cards);

        if (currentMonthly != null) {
            addCardToHydrate(cardsToHydrate, currentMonthly);
        }

        for (HomeCard card : cards) {
            if (card != null
                    && ((!card.isMonthly() && !card.isCerrado())
                    || card.isCerrado())
                    && card.getTableID() != null) {
                addCardToHydrate(cardsToHydrate, card);
            }
        }

        hydrateNextSummary(rootRef, cardsToHydrate, 0, callback);
    }

    private void addCardToHydrate(@NonNull ArrayList<HomeCard> cardsToHydrate, @NonNull HomeCard card) {
        if (card.getSummaryVersion() >= SUMMARY_VERSION) {
            return;
        }

        for (HomeCard existing : cardsToHydrate) {
            if (existing != null
                    && existing.getTableID() != null
                    && existing.getTableID().equals(card.getTableID())) {
                return;
            }
        }

        cardsToHydrate.add(card);
    }

    private void hydrateNextSummary(
            @NonNull DatabaseReference rootRef,
            @NonNull ArrayList<HomeCard> cards,
            int index,
            @NonNull LoadCallback callback
    ) {
        if (index >= cards.size()) {
            callback.onSuccess();
            return;
        }

        HomeCard card = cards.get(index);
        if (card == null || card.getTableID() == null) {
            hydrateNextSummary(rootRef, cards, index + 1, callback);
            return;
        }

        if (card.getSummaryVersion() >= SUMMARY_VERSION) {
            hydrateNextSummary(rootRef, cards, index + 1, callback);
            return;
        }

        rootRef.child("trackers_v2").child(card.getTableID()).get()
                .addOnSuccessListener(snapshot -> {
                    double totalAmount = 0d;
                    long expenseCount = 0L;

                    for (DataSnapshot expenseSnapshot : snapshot.child("expenses").getChildren()) {
                        totalAmount += safeDouble(expenseSnapshot.child("amount"), 0d);
                        expenseCount++;
                    }

                    long participantCount = snapshot.child("participants").getChildrenCount();
                    long categoryCount = snapshot.child("categories").getChildrenCount();

                    card.setTotalAmount(totalAmount);
                    card.setSummaryVersion(SUMMARY_VERSION);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("trackers_v2/" + card.getTableID() + "/summary/expenseCount", expenseCount);
                    updates.put("trackers_v2/" + card.getTableID() + "/summary/totalAmount", totalAmount);
                    updates.put("trackers_v2/" + card.getTableID() + "/summary/participantCount", participantCount);
                    updates.put("trackers_v2/" + card.getTableID() + "/summary/categoryCount", categoryCount);
                    updates.put("trackers_v2/" + card.getTableID() + "/summary/version", SUMMARY_VERSION);
                    updates.put("home_index/" + card.getTableID() + "/totalAmount", totalAmount);
                    updates.put("home_index/" + card.getTableID() + "/summaryVersion", SUMMARY_VERSION);

                    rootRef.updateChildren(updates)
                            .addOnSuccessListener(unused -> hydrateNextSummary(rootRef, cards, index + 1, callback))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    private void ensureCurrentMonthlyTracker(
            @NonNull DatabaseReference rootRef,
            @NonNull ArrayList<HomeCard> cards,
            @NonNull LoadCallback callback
    ) {
        YearMonth currentMonth = YearMonth.now();
        String currentMonthKey = currentMonth.toString();

        for (HomeCard card : cards) {
            if (card != null
                    && card.isMonthly()
                    && currentMonthKey.equals(resolveMonthKey(card))) {
                callback.onSuccess();
                return;
            }
        }

        String trackerId = "DATA" + (findBiggestId(cards) + 1);
        LocalDate createdAt = currentMonth.atDay(1);
        String name = formatMonthlyName(currentMonth);
        Map<String, Object> defaultCategories = DefaultCategories.asFirebaseMap();

        Map<String, Object> updates = new HashMap<>();
        updates.put("trackers_v2/" + trackerId + "/meta/legacyId", trackerId);
        updates.put("trackers_v2/" + trackerId + "/meta/name", name);
        updates.put("trackers_v2/" + trackerId + "/meta/createdAt", createdAt.toString());
        updates.put("trackers_v2/" + trackerId + "/meta/updatedAt", LocalDate.now().toString());
        updates.put("trackers_v2/" + trackerId + "/meta/closed", false);
        updates.put("trackers_v2/" + trackerId + "/meta/type", HomeCard.TYPE_MONTHLY);
        updates.put("trackers_v2/" + trackerId + "/meta/monthKey", currentMonthKey);
        updates.put("trackers_v2/" + trackerId + "/meta/autoCreated", true);
        updates.put("trackers_v2/" + trackerId + "/meta/version", 2);
        updates.put("trackers_v2/" + trackerId + "/meta/migratedFrom", "auto-monthly");
        updates.put("trackers_v2/" + trackerId + "/categories", defaultCategories);
        updates.put("trackers_v2/" + trackerId + "/expenses", new HashMap<>());
        updates.put("trackers_v2/" + trackerId + "/summary/expenseCount", 0);
        updates.put("trackers_v2/" + trackerId + "/summary/totalAmount", 0);
        updates.put("trackers_v2/" + trackerId + "/summary/participantCount", 0);
        updates.put("trackers_v2/" + trackerId + "/summary/categoryCount", defaultCategories.size());
        updates.put("trackers_v2/" + trackerId + "/summary/version", SUMMARY_VERSION);

        updates.put("home_index/" + trackerId + "/trackerId", trackerId);
        updates.put("home_index/" + trackerId + "/name", name);
        updates.put("home_index/" + trackerId + "/createdAt", createdAt.toString());
        updates.put("home_index/" + trackerId + "/closed", false);
        updates.put("home_index/" + trackerId + "/isSetupComplete", false);
        updates.put("home_index/" + trackerId + "/type", HomeCard.TYPE_MONTHLY);
        updates.put("home_index/" + trackerId + "/monthKey", currentMonthKey);
        updates.put("home_index/" + trackerId + "/autoCreated", true);
        updates.put("home_index/" + trackerId + "/totalAmount", 0);
        updates.put("home_index/" + trackerId + "/summaryVersion", SUMMARY_VERSION);

        writeMonthlyTracker(rootRef, trackerId, updates, () -> {
            cards.add(HomeCard.fromTrackerId(
                    trackerId,
                    createdAt,
                    name,
                    false,
                    false,
                    HomeCard.TYPE_MONTHLY,
                    currentMonthKey,
                    0d,
                    SUMMARY_VERSION
            ));
            callback.onSuccess();
        }, callback);
    }

    private void writeMonthlyTracker(
            @NonNull DatabaseReference rootRef,
            @NonNull String trackerId,
            @NonNull Map<String, Object> updates,
            @NonNull Runnable onVerified,
            @NonNull LoadCallback callback
    ) {
        Log.d(TAG, "Creating monthly tracker " + trackerId + " at " + rootRef.toString());
        rootRef.updateChildren(updates)
                .addOnSuccessListener(unused -> verifyMonthlyTrackerCreated(rootRef, trackerId, onVerified, callback))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Monthly tracker write failed: " + trackerId, error);
                    callback.onError(error);
                });
    }

    private void verifyMonthlyTrackerCreated(
            @NonNull DatabaseReference rootRef,
            @NonNull String trackerId,
            @NonNull Runnable onVerified,
            @NonNull LoadCallback callback
    ) {
        rootRef.child("home_index").child(trackerId).get()
                .addOnSuccessListener(homeSnapshot -> {
                    if (!homeSnapshot.exists()) {
                        IllegalStateException error = new IllegalStateException(
                                "El tracker mensual no existe en home_index luego de crearlo: " + trackerId
                        );
                        Log.e(TAG, error.getMessage());
                        callback.onError(error);
                        return;
                    }

                    rootRef.child("trackers_v2").child(trackerId).child("meta").get()
                            .addOnSuccessListener(metaSnapshot -> {
                                if (!metaSnapshot.exists()) {
                                    IllegalStateException error = new IllegalStateException(
                                            "El tracker mensual no existe en trackers_v2 luego de crearlo: " + trackerId
                                    );
                                    Log.e(TAG, error.getMessage());
                                    callback.onError(error);
                                    return;
                                }

                                Log.d(TAG, "Monthly tracker verified in Firebase: " + trackerId);
                                onVerified.run();
                            })
                            .addOnFailureListener(error -> {
                                Log.e(TAG, "Monthly tracker trackers_v2 verification failed: " + trackerId, error);
                                callback.onError(error);
                            });
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Monthly tracker home_index verification failed: " + trackerId, error);
                    callback.onError(error);
                });
    }

    private HomeCard findPreviousMonthly(ArrayList<HomeCard> cards, YearMonth currentMonth) {
        HomeCard previous = null;
        YearMonth previousMonth = null;

        for (HomeCard card : cards) {
            if (card == null || !card.isMonthly() || card.getCreationDate() == null) {
                continue;
            }

            YearMonth cardMonth = YearMonth.from(card.getCreationDate());
            if (!cardMonth.isBefore(currentMonth)) {
                continue;
            }

            if (previousMonth == null || cardMonth.isAfter(previousMonth)) {
                previousMonth = cardMonth;
                previous = card;
            }
        }

        return previous;
    }

    private HomeCard findCurrentMonthly(ArrayList<HomeCard> cards) {
        String currentMonthKey = YearMonth.now().toString();

        for (HomeCard card : cards) {
            if (card != null
                    && card.isMonthly()
                    && currentMonthKey.equals(resolveMonthKey(card))) {
                return card;
            }
        }

        return null;
    }

    private int findBiggestId(ArrayList<HomeCard> cards) {
        int biggest = 0;

        for (HomeCard card : cards) {
            if (card == null) {
                continue;
            }

            String rawId = card.getId();
            if (rawId == null || rawId.isEmpty()) {
                rawId = card.getTableID();
            }
            if (rawId == null) {
                continue;
            }

            String numericPart = rawId.startsWith("DATA") ? rawId.substring(4) : rawId;
            try {
                biggest = Math.max(biggest, Integer.parseInt(numericPart));
            } catch (NumberFormatException ignored) {
            }
        }

        return biggest;
    }

    private String resolveMonthKey(HomeCard card) {
        if (card.getMonthKey() != null && !card.getMonthKey().trim().isEmpty()) {
            return card.getMonthKey();
        }

        if (card.getCreationDate() == null) {
            return null;
        }

        return YearMonth.from(card.getCreationDate()).toString();
    }

    private String formatMonthlyName(YearMonth month) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "AR"));
        String raw = month.atDay(1).format(formatter);
        if (raw.isEmpty()) {
            return month.toString();
        }
        return raw.substring(0, 1).toUpperCase(new Locale("es", "AR")) + raw.substring(1);
    }

    private String safeString(DataSnapshot snapshot, String fallback) {
        String value = snapshot.getValue(String.class);
        return value != null ? value : fallback;
    }

    private int safeInt(DataSnapshot snapshot, int fallback) {
        Object value = snapshot.getValue();
        if (value == null) return fallback;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
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
}
