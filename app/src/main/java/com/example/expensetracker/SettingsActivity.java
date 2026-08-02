package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.data.DefaultCategories;
import com.example.expensetracker.data.UserProfileRepository;
import com.example.expensetracker.model.Member;
import com.example.expensetracker.ui.common.AppSnackbar;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private MaterialToolbar toolbar;
    private TextInputLayout txtFieldNombre1;
    private TextInputLayout txtFieldSueldo1;
    private TextInputLayout txtFieldNombre2;
    private TextInputLayout txtFieldSueldo2;
    private TextInputEditText txtEditNombre1;
    private TextInputEditText txtEditSueldo1;
    private TextInputEditText txtEditNombre2;
    private TextInputEditText txtEditSueldo2;
    private MaterialButton btnContinuar;
    private TextView txtIntegrante1;
    private TextView txtIntegrante2;
    private Boolean comingFromExpense;
    private String trackerId;
    private String currentNickname = "";
    private String currentParticipantId;
    private FirebaseUser currentUser;
    private DataSnapshot participantsSnapshot;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        trackerId = getIntent().getStringExtra("trackerId");
        comingFromExpense = getIntent().getBooleanExtra("fromExpenseV2", false);

        if (trackerId == null && HCardDB.getSelected() != null) {
            trackerId = HCardDB.getSelected().getTableID();
        }

        toolbar = findViewById(R.id.toolbarBack_widget);
        toolbar.getChildAt(1).setOnClickListener(view -> finish());

        loadIDs();
        configureSingleUserForm();
        loadInitialData();

        txtEditSueldo1.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                txtFieldSueldo1.setErrorEnabled(false);
            }
        });

        btnContinuar.setOnClickListener(v -> saveCurrentUserSalary());
    }

    private void loadIDs() {
        btnContinuar = findViewById(R.id.btnContinuar);

        txtIntegrante1 = findViewById(R.id.txtIntegrante1);
        txtIntegrante2 = findViewById(R.id.txtIntegrante2);

        txtFieldNombre1 = findViewById(R.id.txtFieldNombre1);
        txtFieldSueldo1 = findViewById(R.id.txtFieldSueldo1);
        txtFieldNombre2 = findViewById(R.id.txtFieldNombre2);
        txtFieldSueldo2 = findViewById(R.id.txtFieldSueldo2);

        txtEditNombre1 = findViewById(R.id.txtEditNombre1);
        txtEditSueldo1 = findViewById(R.id.txtEditSueldo1);
        txtEditNombre2 = findViewById(R.id.txtEditNombre2);
        txtEditSueldo2 = findViewById(R.id.txtEditSueldo2);
    }

    private void configureSingleUserForm() {
        txtIntegrante1.setText("Tu configuración");
        txtFieldNombre1.setHint("Tu apodo");
        txtFieldSueldo1.setHint("Tu sueldo");
        txtEditNombre1.setEnabled(false);

        txtIntegrante2.setVisibility(View.GONE);
        txtFieldNombre2.setVisibility(View.GONE);
        txtFieldSueldo2.setVisibility(View.GONE);
        txtEditNombre2.setVisibility(View.GONE);
        txtEditSueldo2.setVisibility(View.GONE);
    }

    private void loadInitialData() {
        if (trackerId == null || trackerId.trim().isEmpty()) {
            AppSnackbar.show(this, "No se pudo cargar la configuración");
            return;
        }

        UserProfileRepository userProfileRepository = new UserProfileRepository();
        userProfileRepository.loadCurrentNickname(new UserProfileRepository.Callback<String>() {
            @Override
            public void onSuccess(String nickname) {
                currentNickname = nickname != null ? nickname.trim() : "";
                runOnUiThread(() -> txtEditNombre1.setText(currentNickname));
                loadParticipants();
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error loading profile", exception);
                runOnUiThread(() -> AppSnackbar.show(SettingsActivity.this, "No se pudo cargar tu apodo"));
            }
        });
    }

    private void loadParticipants() {
        FirebaseDatabase.getInstance()
                .getReference()
                .child("trackers_v2")
                .child(trackerId)
                .child("participants")
                .get()
                .addOnSuccessListener(snapshot -> {
                    participantsSnapshot = snapshot;
                    currentParticipantId = resolveCurrentParticipantId(snapshot);
                    Member currentMember = readMember(snapshot, currentParticipantId);

                    runOnUiThread(() -> {
                        if (currentMember != null) {
                            txtEditSueldo1.setText(formatIncome(currentMember.getSalary()));
                        }
                    });
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Error loading participants", exception);
                    AppSnackbar.show(SettingsActivity.this, "No se pudieron cargar los participantes");
                });
    }

    private String resolveCurrentParticipantId(@NonNull DataSnapshot snapshot) {
        for (DataSnapshot child : snapshot.getChildren()) {
            String userId = child.child("userId").getValue(String.class);
            if (currentUser.getUid().equals(userId)) {
                return child.getKey();
            }
        }

        for (DataSnapshot child : snapshot.getChildren()) {
            String name = child.child("name").getValue(String.class);
            if (name != null && name.trim().equalsIgnoreCase(currentNickname)) {
                return child.getKey();
            }
        }

        if (!snapshot.child("p1").exists() || isAvailableParticipant(snapshot.child("p1"))) {
            return "p1";
        }

        if (!snapshot.child("p2").exists() || isAvailableParticipant(snapshot.child("p2"))) {
            return "p2";
        }

        return "p1";
    }

    private boolean isAvailableParticipant(@NonNull DataSnapshot participantSnapshot) {
        String userId = participantSnapshot.child("userId").getValue(String.class);
        String name = participantSnapshot.child("name").getValue(String.class);
        return userId == null
                && (name == null
                || name.trim().isEmpty()
                || UserProfileRepository.TBD_NAME.equalsIgnoreCase(name.trim()));
    }

    private Member readMember(@NonNull DataSnapshot snapshot, String participantId) {
        if (participantId == null || !snapshot.child(participantId).exists()) {
            return null;
        }

        DataSnapshot participantSnapshot = snapshot.child(participantId);
        String name = participantSnapshot.child("name").getValue(String.class);
        double salary = readDouble(participantSnapshot.child("income").getValue());
        return new Member(participantId, name, salary);
    }

    private double readDouble(Object value) {
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveCurrentUserSalary() {
        String salaryText = txtEditSueldo1.getText() != null ? txtEditSueldo1.getText().toString().trim() : "";

        if (salaryText.isEmpty()) {
            txtEditSueldo1.clearFocus();
            txtFieldSueldo1.setErrorEnabled(true);
            txtFieldSueldo1.setError("Es necesario completar este campo.");
            txtFieldSueldo1.setErrorIconDrawable(R.drawable.ic_info);
            return;
        }

        if (currentNickname == null || currentNickname.trim().isEmpty()) {
            AppSnackbar.show(this, "Primero configurá tu apodo");
            return;
        }

        double salary;
        try {
            salary = Double.parseDouble(salaryText);
        } catch (NumberFormatException e) {
            AppSnackbar.show(this, "El sueldo debe ser numérico");
            return;
        }

        if (trackerId == null || trackerId.trim().isEmpty()) {
            AppSnackbar.show(this, "No se pudo guardar la configuración");
            return;
        }

        if (currentParticipantId == null || currentParticipantId.trim().isEmpty()) {
            currentParticipantId = "p1";
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference rootRef = database.getReference();

        Map<String, Object> currentParticipant = new HashMap<>();
        currentParticipant.put("active", true);
        currentParticipant.put("income", salary);
        currentParticipant.put("order", "p2".equals(currentParticipantId) ? 2 : 1);
        currentParticipant.put("userId", currentUser.getUid());
        currentParticipant.put("incomePending", false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("trackers_v2/" + trackerId + "/participants/" + currentParticipantId, currentParticipant);
        ensurePlaceholderParticipant(updates);
        updates.put("trackers_v2/" + trackerId + "/summary/participantCount", 2);
        updates.put("home_index/" + trackerId + "/isSetupComplete", true);

        rootRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error saving settings", task.getException());
                AppSnackbar.show(SettingsActivity.this, "Error al guardar la configuración");
                return;
            }

            HCardDB.setSetupComplete(trackerId, true);
            ensureCategories(rootRef);
        });
    }

    private void ensurePlaceholderParticipant(Map<String, Object> updates) {
        String otherParticipantId = "p1".equals(currentParticipantId) ? "p2" : "p1";
        if (participantsSnapshot != null && participantsSnapshot.child(otherParticipantId).exists()) {
            return;
        }

        Map<String, Object> otherParticipant = new HashMap<>();
        otherParticipant.put("active", true);
        otherParticipant.put("income", 1);
        otherParticipant.put("order", "p2".equals(otherParticipantId) ? 2 : 1);
        otherParticipant.put("incomePending", true);
        updates.put("trackers_v2/" + trackerId + "/participants/" + otherParticipantId, otherParticipant);
    }

    private void ensureCategories(DatabaseReference rootRef) {
        DatabaseReference categoriesRef = rootRef.child("trackers_v2").child(trackerId).child("categories");

        categoriesRef.get().addOnCompleteListener(catTask -> {
            if (!catTask.isSuccessful()) {
                Log.e(TAG, "Error checking categories", catTask.getException());
                AppSnackbar.show(SettingsActivity.this, "Error al guardar las categorías");
                return;
            }

            if (!catTask.getResult().exists()) {
                Map<String, Object> defaultCategories = DefaultCategories.asFirebaseMap();
                categoriesRef.setValue(defaultCategories).addOnCompleteListener(saveCategoriesTask -> {
                    if (!saveCategoriesTask.isSuccessful()) {
                        Log.e(TAG, "Error saving categories", saveCategoriesTask.getException());
                        AppSnackbar.show(SettingsActivity.this, "Error al guardar las categorías");
                        return;
                    }

                    rootRef.child("trackers_v2")
                            .child(trackerId)
                            .child("summary")
                            .child("categoryCount")
                            .setValue(defaultCategories.size());
                    openNextScreen();
                });
            } else {
                rootRef.child("trackers_v2")
                        .child(trackerId)
                        .child("summary")
                        .child("categoryCount")
                        .setValue(catTask.getResult().getChildrenCount());
                openNextScreen();
            }
        });
    }

    private String formatIncome(double income) {
        if (income <= 0) {
            return "";
        }
        if (income == (long) income) {
            return String.valueOf((long) income);
        }
        return String.valueOf(income);
    }

    private void openNextScreen() {
        if (comingFromExpense != null && comingFromExpense) {
            finish();
        } else {
            Intent intent = new Intent(SettingsActivity.this, ExpenseActivityV2.class);
            intent.putExtra("trackerId", trackerId);
            SettingsActivity.this.startActivity(intent);
            finish();
        }
    }
}
