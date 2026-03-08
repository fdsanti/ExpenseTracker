package com.example.expensetracker;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;

public class HomeFirebaseV2Repository {

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

                        LocalDate creationDate;
                        try {
                            creationDate = LocalDate.parse(createdAt);
                        } catch (Exception e) {
                            creationDate = LocalDate.now();
                        }

                        boolean isSetupComplete = Boolean.TRUE.equals(trackerSnapshot.child("isSetupComplete").getValue(Boolean.class));
                        HomeCard tempCard = HomeCard.fromTrackerId(trackerId, creationDate, name, closed, isSetupComplete);
                        tempHCArray.add(tempCard);
                    }

                    Collections.sort(tempHCArray, new HomeCardSortDate());
                    for (HomeCard hc : tempHCArray) {
                        HCardDB.addExpense(hc.getTableID(), hc);
                    }

                    callback.onSuccess();
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
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
}
