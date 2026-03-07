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

    public static final String ROOT = "trackers_v2";

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
                        String trackerId = trackerSnapshot.getKey();
                        DataSnapshot metaSnapshot = trackerSnapshot.child("meta");
                        if (!metaSnapshot.exists() || trackerId == null) {
                            continue;
                        }

                        String name = safeString(metaSnapshot.child("name"), "Sin nombre");
                        String createdAt = safeString(metaSnapshot.child("createdAt"), LocalDate.now().toString());
                        String status = safeString(metaSnapshot.child("status"), "open");
                        boolean closed = "closed".equalsIgnoreCase(status)
                                || Boolean.TRUE.equals(metaSnapshot.child("closed").getValue(Boolean.class));

                        LocalDate creationDate;
                        try {
                            creationDate = LocalDate.parse(createdAt);
                        } catch (Exception e) {
                            creationDate = LocalDate.now();
                        }

                        HomeCard tempCard = HomeCard.fromTrackerId(trackerId, creationDate, name, closed);
                        tempHCArray.add(tempCard);

                        DataSnapshot participantsSnapshot = trackerSnapshot.child("participants");
                        if (participantsSnapshot.exists()) {
                            String name1 = "";
                            String name2 = "";
                            int sueldo1 = 0;
                            int sueldo2 = 0;
                            int index = 0;

                            for (DataSnapshot participantSnapshot : participantsSnapshot.getChildren()) {
                                String participantName = safeString(participantSnapshot.child("name"), "");
                                int income = safeInt(participantSnapshot.child("income"), 0);

                                if (index == 0) {
                                    name1 = participantName;
                                    sueldo1 = income;
                                } else if (index == 1) {
                                    name2 = participantName;
                                    sueldo2 = income;
                                }
                                index++;
                            }

                            if (!name1.isEmpty() || !name2.isEmpty()) {
                                SettingsDB.addToDB(new Settings(trackerId, name1, sueldo1, name2, sueldo2));
                            }
                        }
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
