package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {
    private ProgressDialog progressDialog;
    private DBHandler handler;
    private Connection connection;
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
    private MaterialButton btnTest;

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
                //Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                //SettingsActivity.this.startActivity(intent);
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
                myRef.child("settings").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (!task.isSuccessful()) {
                            Log.e("firebase", "Error getting data", task.getException());
                        }
                        else {
                            //Updating firebase DB
                            myRef.child("settings").child(newSet.getTableID()).child("name1").setValue(newSet.getName1());
                            myRef.child("settings").child(newSet.getTableID()).child("name2").setValue(newSet.getName2());
                            myRef.child("settings").child(newSet.getTableID()).child("sueldo1").setValue(newSet.getIncome1());
                            myRef.child("settings").child(newSet.getTableID()).child("sueldo2").setValue(newSet.getIncome2());

                            //if the name has changed, then we need to iterate over all the rows and update the names
                            //this still needs to be tested
                            if (comingFromExpense) {
                                if (!txtEditNombre1.getText().toString().equals(SettingsDB.getSetting(HCardDB.getSelected()).getName1())) {
                                    updateNamesOnRowsFB(SettingsDB.getSetting(HCardDB.getSelected()).getName1(), txtEditNombre1.getText().toString());
                                }
                                if (!txtEditNombre2.getText().toString().equals(SettingsDB.getSetting(HCardDB.getSelected()).getName2())) {
                                    updateNamesOnRowsFB(SettingsDB.getSetting(HCardDB.getSelected()).getName2(), txtEditNombre2.getText().toString());
                                }
                            }
                            //adding to local array
                            SettingsDB.addToDB(newSet);
                            Intent intent = new Intent(SettingsActivity.this, ExpenseActivity.class);
                            SettingsActivity.this.startActivity(intent);
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

        //testing purposes - Ignore. To uncomment this, you should uncomment the btn on the "activity_settings.xml"
        /*btnTest.setOnClickListener(v -> {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference();

            myRef.child("DATA1").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    }
                    else {
                        for (DataSnapshot child : task.getResult().getChildren()) {
                            System.out.println(child.getKey().toString());
                        }
                        //Updating firebase DB
                        System.out.println("Test " + task.getResult().child("1").getValue());

                    }
                }
            });
        });*/

    }

    private void updateNamesOnRowsFB(String oldName, String newName) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference();
        myRef.child(HCardDB.getSelected().getTableID().toString()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    //Updating firebase DB

                    for (DataSnapshot row : task.getResult().getChildren()) {
                        String currName = row.child("who").getValue().toString();
                        if (currName == oldName) {
                            myRef.child(HCardDB.getSelected().getTableID().toString()).child(row.getKey().toString()).child("who").setValue(newName);
                        }
                    }
                    Log.d("firebase", String.valueOf(task.getResult().getValue()));
                }
            }
        });
    }

    private void updateNamesOnRows(String oldName, String newName, Connection connection) {
        String update = "UPDATE " + SettingsDB.getSetting(HCardDB.getSelected()).getTableID() + " SET Who=? WHERE Who=?";

        try {
            PreparedStatement pst = connection.prepareStatement(update);
            pst.setString(1,newName);
            pst.setString(2,oldName);
            pst.executeUpdate();
        }
        catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    private void updateNameOnSettings(String name1, String toString, Connection connection) {
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
}
