package com.example.expensetracker.data;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserProfileRepository {
    public static final String TBD_NAME = "TBD";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception exception);
    }

    private final DatabaseReference database;

    public UserProfileRepository() {
        database = FirebaseDatabase.getInstance().getReference();
    }

    public FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void loadCurrentNickname(@NonNull Callback<String> callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("User is not authenticated"));
            return;
        }

        database.child("users").child(user.getUid()).child("nickname").get()
                .addOnSuccessListener(snapshot -> {
                    String nickname = snapshot.getValue(String.class);
                    callback.onSuccess(nickname != null ? nickname.trim() : "");
                })
                .addOnFailureListener(callback::onError);
    }

    public void saveCurrentNickname(@NonNull String nickname, @NonNull Callback<Void> callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("User is not authenticated"));
            return;
        }

        String cleanNickname = nickname.trim();
        String today = LocalDate.now().toString();

        Map<String, Object> updates = new HashMap<>();
        updates.put("users/" + user.getUid() + "/nickname", cleanNickname);
        updates.put("users/" + user.getUid() + "/email", user.getEmail());
        updates.put("users/" + user.getUid() + "/updatedAt", today);

        database.updateChildren(updates)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void loadDefaultParticipants(@NonNull Callback<Map<String, Object>> callback) {
        database.child("households").child("default").child("defaultParticipants").get()
                .addOnSuccessListener(defaultSnapshot -> {
                    if (!defaultSnapshot.exists()) {
                        callback.onSuccess(new HashMap<>());
                        return;
                    }

                    Map<String, Object> participants = new LinkedHashMap<>();

                    for (DataSnapshot participantSnapshot : defaultSnapshot.getChildren()) {
                        String participantId = participantSnapshot.getKey();
                        String userId = participantSnapshot.child("userId").getValue(String.class);
                        int order = readInt(participantSnapshot.child("order").getValue(), participants.size() + 1);

                        if (participantId == null || userId == null || userId.trim().isEmpty()) {
                            continue;
                        }

                        Map<String, Object> participant = new HashMap<>();
                        participant.put("active", true);
                        participant.put("income", 1);
                        participant.put("incomePending", true);
                        participant.put("order", order);
                        participant.put("userId", userId);

                        participants.put(participantId, participant);
                    }

                    callback.onSuccess(participants);
                })
                .addOnFailureListener(callback::onError);
    }

    private int readInt(Object value, int fallback) {
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }
}
