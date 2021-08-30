package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Bundle b = getIntent().getExtras();
        if(b != null) {
            comingFromExpense = true;
        }
        else {
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

                progressDialog = new ProgressDialog(SettingsActivity.this);
                progressDialog.show();
                progressDialog.setContentView(R.layout.progress_dialog);
                progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                Handler handlerUI = new Handler();
                Runnable runnable = new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {
                        synchronized (this) {
                            handler = new DBHandler();
                            connection = handler.getConnection(SettingsActivity.this);
                            Settings newSet = new Settings(HCardDB.getSelected().getTableID(), txtEditNombre1.getText().toString(), Integer.parseInt(txtEditSueldo1.getText().toString()), txtEditNombre2.getText().toString(), Integer.parseInt(txtEditSueldo2.getText().toString()));
                            if (comingFromExpense) {
                                if (!txtEditNombre1.getText().toString().equals(SettingsDB.getSetting(HCardDB.getSelected()).getName1())) {
                                    updateNamesOnRows(SettingsDB.getSetting(HCardDB.getSelected()).getName1(), txtEditNombre1.getText().toString(), connection);
                                }
                                if (!txtEditNombre2.getText().toString().equals(SettingsDB.getSetting(HCardDB.getSelected()).getName2())) {
                                    updateNamesOnRows(SettingsDB.getSetting(HCardDB.getSelected()).getName2(), txtEditNombre2.getText().toString(), connection);
                                }
                            }
                            SettingsDB.save(connection,newSet);
                            SettingsDB.addToDB(newSet);
                            try {
                                connection.close();
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                            }
                        }

                        handlerUI.post(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(SettingsActivity.this, ExpenseActivity.class);
                                SettingsActivity.this.startActivity(intent);
                                progressDialog.dismiss();
                            }
                        });
                    }
                };
                Thread thread = new Thread(runnable);
                thread.start();
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
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void loadIDs() {
        btnContinuar = findViewById(R.id.btnContinuar);

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
