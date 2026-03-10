package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            comingFromExpense = true;
        } else {
            comingFromExpense = false;
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

        if (SettingsDB.isInDB(HCardDB.getSelected())) {
            HomeCard hcSelected = HCardDB.getSelected();
            Settings selectedSetting = SettingsDB.getSetting(hcSelected);
            txtEditNombre1.setText(selectedSetting.getName1());
            txtEditSueldo1.setText(String.valueOf(selectedSetting.getIncome1()));
            txtEditNombre2.setText(selectedSetting.getName2());
            txtEditSueldo2.setText(String.valueOf(selectedSetting.getIncome2()));
        }

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
            //si todos los fields estan completos, entonces guardar setting en la base de datos
            if (!txtEditNombre1.getText().toString().isEmpty()
                    & !txtEditSueldo1.getText().toString().isEmpty()
                    & !txtEditNombre2.getText().toString().isEmpty()
                    & !txtEditSueldo2.getText().toString().isEmpty()) {

                Settings newSet = new Settings(HCardDB.getSelected().getTableID(), txtEditNombre1.getText().toString(), Integer.parseInt(txtEditSueldo1.getText().toString()), txtEditNombre2.getText().toString(), Integer.parseInt(txtEditSueldo2.getText().toString()));

                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference();
                myRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (!task.isSuccessful()) {
                            Log.e("firebase", "Error getting data", task.getException());
                        }
                        else {
                            //Updating firebase DB
                            myRef.child("trackers_v2").child(newSet.getTableID()).child("participants").child("p1").child("name").setValue(newSet.getName1());
                            myRef.child("trackers_v2").child(newSet.getTableID()).child("participants").child("p2").child("name").setValue(newSet.getName2());
                            myRef.child("trackers_v2").child(newSet.getTableID()).child("participants").child("p1").child("income").setValue(newSet.getIncome1());
                            myRef.child("trackers_v2").child(newSet.getTableID()).child("participants").child("p2").child("income").setValue(newSet.getIncome2());
                            myRef.child("home_index").child(newSet.getTableID()).child("isSetupComplete").setValue(true);
                            HCardDB.getSelected().setSetupComplete(true);

                            // 2. NEW: Initialize categories for this tracker if they don't exist
                            // We check if the tracker already has categories (in case the user is just editing settings)
                            myRef.child("trackers_v2").child(newSet.getTableID()).child("categories").get().addOnCompleteListener(catTask -> {
                                if (catTask.isSuccessful() && !catTask.getResult().exists()) {
                                    // No categories found for this ID, so we push the defaults
                                    myRef.child("trackers_v2").child(newSet.getTableID()).child("categories").setValue(buildDefaultCategoriesMap());
                                }
                            });

                            SettingsDB.addToDB(newSet);
                            Intent intent = new Intent(SettingsActivity.this, ExpenseActivity.class);
                            SettingsActivity.this.startActivity(intent);
                            finish();
                            Log.d("firebase", String.valueOf(task.getResult().getValue()));
                        }
                    }
                });
            }

            //si falta algun dato, pedir de completar los input fields
            else {
                txtEditNombre1.clearFocus();
                txtEditNombre2.clearFocus();
                txtEditSueldo1.clearFocus();
                txtEditSueldo2.clearFocus();
                if (txtEditNombre1.getText().toString().isEmpty()) {
                    txtFieldNombre1.setErrorEnabled(true);
                    txtFieldNombre1.setError("Es necesario completar este campo.");
                    txtFieldNombre1.setErrorIconDrawable(R.drawable.ic_info);
                }
                if (txtEditSueldo1.getText().toString().isEmpty()) {
                    txtFieldSueldo1.setErrorEnabled(true);
                    txtFieldSueldo1.setError("Es necesario completar este campo.");
                    txtFieldSueldo1.setErrorIconDrawable(R.drawable.ic_info);
                }
                if (txtEditNombre2.getText().toString().isEmpty()) {
                    txtFieldNombre2.setErrorEnabled(true);
                    txtFieldNombre2.setError("Es necesario completar este campo.");
                    txtFieldNombre2.setErrorIconDrawable(R.drawable.ic_info);
                }
                if (txtEditSueldo2.getText().toString().isEmpty()) {
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
}
