package com.example.expensetracker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private Toolbar toolbar;
    private RecyclerView homeRecycler;
    private Connection connection;
    private DBHandler handler;
    private ProgressDialog progressDialog;
    private ExtendedFloatingActionButton btnNewHomeCard;
    private HCardsViewAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_containerMain);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);



        toolbar = findViewById(R.id.toolbar_widget);
        homeRecycler = findViewById(R.id.homeRecycler);
        btnNewHomeCard = findViewById(R.id.btnNewHomeCard);


        setSupportActionBar(toolbar);

        if (HCardDB.isNull()) {
            loadReportsFromDB();
        }
        if (!HCardDB.isNull()) {
            loadReportsFromArrayList();
        }

        //Click listener for "Crear nuevo expense" btn
        btnNewHomeCard.setOnClickListener(v -> {
            //Craete Alert dialog
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            dialog.setCancelable(false);
            dialog.setTitle("Agregar reporte");
            dialog.setMessage("Por favor escriba un nombre para el reporte");
            //Add input field to dialog
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.input1, null);
            dialog.setView(dialogView);
            dialog.setNegativeButton("Cancelar", null);
            dialog.setPositiveButton("Crear", null);

            AlertDialog alertDialog = dialog.create();
            alertDialog.show();

            //Click en "Crear" btn
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextInputEditText editText = dialogView.findViewById(R.id.edit_text);
                    //si el nombre ya existe, entonces mostrar error state
                    if (HCardDB.containsDescription(editText.getText().toString())) {

                        TextInputLayout inputField = dialogView.findViewById(R.id.filledTextField);
                        inputField.setErrorEnabled(true);
                        inputField.setError("El nombre ya existe. Elija otro.");
                        inputField.setErrorIconDrawable(R.drawable.ic_info);
                    }
                    //si el input field esta vacio, mostrar error state
                    else if (editText.getText().toString().trim().isEmpty()) {

                        TextInputLayout inputField = dialogView.findViewById(R.id.filledTextField);
                        inputField.setErrorEnabled(true);
                        inputField.setError("Es necesario elegir un nombre.");
                        inputField.setErrorIconDrawable(R.drawable.ic_info);
                    }
                    //Show progress bar and create new report
                    else {
                        alertDialog.dismiss();
                        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.show();
                        progressDialog.setContentView(R.layout.progress_dialog);
                        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                        //Find biggest Id and create the new report with id + 1
                        int newID = HCardDB.getBiggestID() + 1;
                        HomeCard hc = new HomeCard(String.valueOf(newID), LocalDate.now(), editText.getText().toString());
                        //Create runnable so that the server communication happens in a background thread
                        Handler handlerUI = new Handler();
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                synchronized (this) {
                                    HCardDB.createDBTable(hc, MainActivity.this);
                                }

                                handlerUI.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        HCardDB.addExpense(String.valueOf(newID), hc);
                                        adapter.addHCard(hc);
                                        progressDialog.dismiss();
                                        Toast.makeText(MainActivity.this, "¡El expense ha sido creado con éxito!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        };
                        Thread thread = new Thread(runnable);
                        thread.start();
                    }
                }
            });
        });

    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        loadReportsFromDB();
        return super.onOptionsItemSelected(item);
    }*/

    private void loadReportsFromDB() {
        progressDialog = new ProgressDialog(MainActivity.this);
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
                    connection = handler.getConnection(MainActivity.this);
                    HCardDB.loadDB(connection);
                    SettingsDB.loadDB(connection);
                    try {
                        connection.close();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }

                handlerUI.post(new Runnable() {
                    @Override
                    public void run() {
                        if(!HCardDB.isEmpty()) {
                            ArrayList<HomeCard> hCards = HCardDB.getReports();
                            adapter = new HCardsViewAdapter(MainActivity.this);
                            adapter.setCards(hCards);
                            homeRecycler.setAdapter(adapter);
                            homeRecycler.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                            progressDialog.dismiss();
                        }
                        else {
                            TextView txtEmpty = new TextView(MainActivity.this);
                            txtEmpty.setText("There are no reports yet!");
                        }
                    }
                });
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void loadReportsFromArrayList() {
        if(!HCardDB.isEmpty()) {
            ArrayList<HomeCard> hCards = HCardDB.getReports();
            adapter = new HCardsViewAdapter(MainActivity.this);
            adapter.setCards(hCards);
            homeRecycler.setAdapter(adapter);
            homeRecycler.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        }
        else {
            TextView txtEmpty = new TextView(MainActivity.this);
            txtEmpty.setText("There are no reports yet!");
        }

    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 50);
        loadReportsFromDB();
    }
}