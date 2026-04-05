package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import com.example.expensetracker.data.TrackerRepository;
import com.example.expensetracker.model.Member;
import java.util.List;
import com.example.expensetracker.ExpenseActivityV2;

public class SettingsActivity extends AppCompatActivity {
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
    private Boolean comingFromExpense;
    private String trackerId;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        trackerId = getIntent().getStringExtra("trackerId");
        comingFromExpense = getIntent().getBooleanExtra("fromExpenseV2", false);

        if (trackerId == null && HCardDB.getSelected() != null) {
            trackerId = HCardDB.getSelected().getTableID();
        }
        toolbar = findViewById(R.id.toolbarBack_widget);
        //Al hacer click en back button
        toolbar.getChildAt(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        loadIDs();

        loadInitialData();

        txtEditNombre1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    txtFieldNombre1.setErrorEnabled(false);
                }
            }
        });
        txtEditSueldo1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    txtFieldSueldo1.setErrorEnabled(false);
                }
            }
        });
        txtEditNombre2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    txtFieldNombre2.setErrorEnabled(false);
                }
            }
        });
        txtEditSueldo2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    txtFieldSueldo2.setErrorEnabled(false);
                }
            }
        });

        btnContinuar.setOnClickListener(v -> {
            String name1 = txtEditNombre1.getText().toString().trim();
            String salary1Text = txtEditSueldo1.getText().toString().trim();
            String name2 = txtEditNombre2.getText().toString().trim();
            String salary2Text = txtEditSueldo2.getText().toString().trim();

            if (!name1.isEmpty()
                    && !salary1Text.isEmpty()
                    && !name2.isEmpty()
                    && !salary2Text.isEmpty()) {

                if (trackerId == null || trackerId.trim().isEmpty()) {
                    Toast.makeText(SettingsActivity.this, "No se pudo guardar la configuración", Toast.LENGTH_SHORT).show();
                    return;
                }

                double salary1;
                double salary2;

                try {
                    salary1 = Double.parseDouble(salary1Text);
                    salary2 = Double.parseDouble(salary2Text);
                } catch (NumberFormatException e) {
                    Toast.makeText(SettingsActivity.this, "Los sueldos deben ser numéricos", Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference();

                Map<String, Object> participant1 = new HashMap<>();
                participant1.put("active", true);
                participant1.put("income", salary1);
                participant1.put("name", name1);
                participant1.put("order", 1);

                Map<String, Object> participant2 = new HashMap<>();
                participant2.put("active", true);
                participant2.put("income", salary2);
                participant2.put("name", name2);
                participant2.put("order", 2);

                Map<String, Object> updates = new HashMap<>();
                updates.put("trackers_v2/" + trackerId + "/participants/p1", participant1);
                updates.put("trackers_v2/" + trackerId + "/participants/p2", participant2);
                updates.put("home_index/" + trackerId + "/isSetupComplete", true);

                myRef.updateChildren(updates).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("SettingsActivity", "Error saving settings", task.getException());
                        Toast.makeText(SettingsActivity.this, "Error al guardar la configuración", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    HCardDB.setSetupComplete(trackerId, true);

                    DatabaseReference categoriesRef = myRef.child("trackers_v2").child(trackerId).child("categories");

                    categoriesRef.get().addOnCompleteListener(catTask -> {
                        if (!catTask.isSuccessful()) {
                            Log.e("SettingsActivity", "Error checking categories", catTask.getException());
                            Toast.makeText(SettingsActivity.this, "Error al guardar las categorías", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!catTask.getResult().exists()) {
                            categoriesRef.setValue(buildDefaultCategoriesMap()).addOnCompleteListener(saveCategoriesTask -> {
                                if (!saveCategoriesTask.isSuccessful()) {
                                    Log.e("SettingsActivity", "Error saving categories", saveCategoriesTask.getException());
                                    Toast.makeText(SettingsActivity.this, "Error al guardar las categorías", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                openNextScreen();
                            });
                        } else {
                            openNextScreen();
                        }
                    });
                });
            } else {
                txtEditNombre1.clearFocus();
                txtEditNombre2.clearFocus();
                txtEditSueldo1.clearFocus();
                txtEditSueldo2.clearFocus();

                if (name1.isEmpty()) {
                    txtFieldNombre1.setErrorEnabled(true);
                    txtFieldNombre1.setError("Es necesario completar este campo.");
                    txtFieldNombre1.setErrorIconDrawable(R.drawable.ic_info);
                }
                if (salary1Text.isEmpty()) {
                    txtFieldSueldo1.setErrorEnabled(true);
                    txtFieldSueldo1.setError("Es necesario completar este campo.");
                    txtFieldSueldo1.setErrorIconDrawable(R.drawable.ic_info);
                }
                if (name2.isEmpty()) {
                    txtFieldNombre2.setErrorEnabled(true);
                    txtFieldNombre2.setError("Es necesario completar este campo.");
                    txtFieldNombre2.setErrorIconDrawable(R.drawable.ic_info);
                }
                if (salary2Text.isEmpty()) {
                    txtFieldSueldo2.setErrorEnabled(true);
                    txtFieldSueldo2.setError("Es necesario completar este campo.");
                    txtFieldSueldo2.setErrorIconDrawable(R.drawable.ic_info);
                }
            }
        });
    }
    private void loadIDs() {
        btnContinuar = findViewById(R.id.btnContinuar);
        //btnTest = findViewById(R.id.btnTest);

        txtFieldNombre1 = findViewById(R.id.txtFieldNombre1);
        txtFieldSueldo1 = findViewById(R.id.txtFieldSueldo1);
        txtFieldNombre2 = findViewById(R.id.txtFieldNombre2);
        txtFieldSueldo2 = findViewById(R.id.txtFieldSueldo2);

        txtEditNombre1 = findViewById(R.id.txtEditNombre1);
        txtEditSueldo1 = findViewById(R.id.txtEditSueldo1);
        txtEditNombre2 = findViewById(R.id.txtEditNombre2);
        txtEditSueldo2 = findViewById(R.id.txtEditSueldo2);
    }

    private Map<String, Object> buildDefaultCategoriesMap() {
        Map<String, Object> categoriesMap = new HashMap<>();

        String[] names = {
                "Salidas",
                "Delivery",
                "Super",
                "Gatitas",
                "Servicios",
                "Nafta / Peajes",
                "Olga",
                "Auto",
                "Pago Casa",
                "Suscripciones",
                "Compras",
                "Otros"
        };

        for (int i = 0; i < names.length; i++) {
            Map<String, Object> category = new HashMap<>();
            category.put("name", names[i]);
            category.put("order", i + 1);
            category.put("active", true);
            category.put("system", false);

            categoriesMap.put("c" + (i + 1), category);
        }

        return categoriesMap;
    }

    private void loadInitialData() {
        if (trackerId == null) {
            if (HCardDB.getSelected() != null && SettingsDB.isInDB(HCardDB.getSelected())) {
                HomeCard hcSelected = HCardDB.getSelected();
                Settings selectedSetting = SettingsDB.getSetting(hcSelected);
                txtEditNombre1.setText(selectedSetting.getName1());
                txtEditSueldo1.setText(String.valueOf(selectedSetting.getIncome1()));
                txtEditNombre2.setText(selectedSetting.getName2());
                txtEditSueldo2.setText(String.valueOf(selectedSetting.getIncome2()));
            }
            return;
        }

        TrackerRepository repository = new TrackerRepository();
        repository.loadParticipants(trackerId, new TrackerRepository.RepositoryCallback<List<Member>>() {
            @Override
            public void onSuccess(List<Member> result) {
                Member member1 = findMemberById(result, "p1");
                Member member2 = findMemberById(result, "p2");

                if (member1 == null && result.size() > 0) {
                    member1 = result.get(0);
                }

                if (member2 == null && result.size() > 1) {
                    member2 = result.get(1);
                }

                if (member1 != null) {
                    txtEditNombre1.setText(member1.getName());
                    txtEditSueldo1.setText(formatIncome(member1.getSalary()));
                }

                if (member2 != null) {
                    txtEditNombre2.setText(member2.getName());
                    txtEditSueldo2.setText(formatIncome(member2.getSalary()));
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.e("SettingsActivity", "Error loading participants", exception);
            }
        });
    }

    private Member findMemberById(List<Member> members, String memberId) {
        for (Member member : members) {
            if (memberId.equals(member.getId())) {
                return member;
            }
        }
        return null;
    }

    private String formatIncome(double income) {
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
