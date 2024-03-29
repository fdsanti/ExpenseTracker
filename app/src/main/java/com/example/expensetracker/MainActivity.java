package com.example.expensetracker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements CallBackItemTouch, SwipeRefreshLayout.OnRefreshListener{

    private Toolbar toolbar;
    private LinearProgressIndicator progressBar;
    private RecyclerView homeRecycler;
    private Connection connection;
    private DBHandler handler;
    private ProgressDialog progressDialog;
    private ExtendedFloatingActionButton btnNewHomeCard;
    private HCardsViewAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ArrayList<HomeCard> hCards;
    private Drawable mIcon;
    private MaterialButton btnSync;
    private static final String TAG = "MainActivity";
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
        progressBar = findViewById(R.id.progressBar);
        setSupportActionBar(toolbar);
        homeRecycler = findViewById(R.id.homeRecycler);
        //btnSync = findViewById(R.id.btnSync);
        //btnNewHomeCard = findViewById(R.id.btnNewHomeCard);


        if (HCardDB.isNull()) {
            progressBar.show();
            progressBar.setVisibility(View.VISIBLE);
            loadReportsFromFirebase();
        }
        if (!HCardDB.isNull()) {
            loadReportsFromArrayList();
        }

        //Click listener for "Crear nuevo expense" btn

        ItemTouchHelper.Callback callback = new MainItemDragSwipeCallback(this);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(homeRecycler);


    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
            @RequiresApi(api = Build.VERSION_CODES.O)
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

                    //Find biggest Id and create the new report with id + 1
                    int newID = HCardDB.getBiggestID() + 1;
                    HomeCard hc = new HomeCard(String.valueOf(newID), LocalDate.now(), editText.getText().toString());

                    //add new card to Firebase DB & update the array
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference myRef = database.getReference();
                    myRef.child("allTables").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            if (!task.isSuccessful()) {
                                Log.e("firebase", "Error getting data", task.getException());
                            }
                            else {
                                myRef.child("allTables").child(hc.getTableID()).child("creationDate").setValue(String.valueOf(hc.getCreationDate()));
                                myRef.child("allTables").child(hc.getTableID()).child("tableDescription").setValue(hc.getName());
                                myRef.child("allTables").child(hc.getTableID()).child("tableName").setValue(hc.getTableID());
                                HCardDB.addExpense(String.valueOf(newID), hc);
                                hCards.add(0,hc);
                                adapter.notifyItemInserted(0);
                                adapter.notifyItemRangeChanged(0,hCards.size());
                                homeRecycler.smoothScrollToPosition(0);
                                Toast.makeText(MainActivity.this, "¡El expense ha sido creado con éxito!", Toast.LENGTH_SHORT).show();
                                Log.d("firebase", String.valueOf(task.getResult().getValue()));
                            }
                        }
                    });


                }
            }
        });
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void itemTuchOnMove(int oldPosition, int newPosition) {

    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        int position = viewHolder.getAdapterPosition();

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(MainActivity.this);
        dialog.setTitle("¿Eliminar reporte?");
        dialog.setMessage("¿Estás seguro que querés eliminar el reporte " + hCards.get(position).getName() + "?");
        dialog.setIcon(R.drawable.ic_delete);


        dialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                adapter.notifyDataSetChanged();
            }
        });
        //OnClickListener para el boton de Eliminar (dentro del popup)
        dialog.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference();
                myRef.child("allTables").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (!task.isSuccessful()) {
                            Log.e("firebase", "Error getting data", task.getException());
                        }
                        else {
                            //remove table in "allTables", the table with all the rows & the data in "settings"
                            //then, remove it from the local array & notify the changes
                            myRef.child("allTables").child(hCards.get(position).getTableID()).removeValue();
                            myRef.child(hCards.get(position).getTableID()).removeValue();
                            myRef.child("settings").child(hCards.get(position).getTableID()).removeValue();
                            HCardDB.removeReportFromArrayList(hCards.get(position).getTableID());
                            SettingsDB.removeReportFromArrayList(hCards.get(position).getTableID());
                            hCards.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position,hCards.size());
                            Toast.makeText(MainActivity.this, "El reporte se ha eliminado", Toast.LENGTH_SHORT).show();
                            Log.d("firebase", String.valueOf(task.getResult().getValue()));
                        }
                    }
                });
            }
        });

        dialog.show();
    }



    /*ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            int position = viewHolder.getAdapterPosition();

            switch (direction) {
                case ItemTouchHelper.LEFT:


                    MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(MainActivity.this);
                    dialog.setTitle("¿Eliminar reporte?");
                    dialog.setMessage("¿Estás seguro que querés eliminar el reporte " + hCards.get(position).getName() + "?");
                    dialog.setIcon(R.drawable.ic_delete);


                    dialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.notifyDataSetChanged();
                        }
                    });
                    //OnClickListener para el boton de Eliminar (dentro del popup)
                    dialog.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                            progressDialog.show();
                            progressDialog.setContentView(R.layout.progress_dialog);
                            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                            Handler handlerUI = new Handler();
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (this) {
                                        DBHandler handler = new DBHandler();
                                        Connection connection = handler.getConnection(MainActivity.this);
                                        HCardDB.removeReport(connection, hCards.get(position).getId());
                                        SettingsDB.removeSettings(hCards.get(position), MainActivity.this);
                                        try {
                                            connection.close();
                                        } catch (SQLException throwables) {
                                            throwables.printStackTrace();
                                        }
                                    }

                                    handlerUI.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            hCards.remove(position);
                                            adapter.notifyItemRemoved(position);

                                            progressDialog.dismiss();
                                            Toast.makeText(MainActivity.this, "El reporte se ha eliminado", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            };
                            Thread thread = new Thread(runnable);
                            thread.start();
                        }
                    });

                    dialog.show();

                    break;

                case ItemTouchHelper.RIGHT:
                    break;
            }
        }

    };*/



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
                        //Code for migrating from MySQL
                        handler = new DBHandler();
                        connection = handler.getConnection(MainActivity.this);

                        RowsDB.loadRows(MainActivity.this, connection,HCardDB.getReports());

                        progressDialog.dismiss();
                        loadReportsFromArrayList();
                    }
                });
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void loadReportsFromFirebase(){
        ArrayList<HomeCard> tempHCArray = new ArrayList<HomeCard>();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference();
        //fetch homecards from Firebase db
        myRef.child("allTables").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    for (DataSnapshot child : task.getResult().getChildren()) {

                        String id = child.child("tableName").getValue().toString().substring(4);
                        LocalDate date = Date.valueOf(child.child("creationDate").getValue(String.class)).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        String reportName = child.child("tableDescription").getValue().toString();
                        HomeCard tempCard = new HomeCard(id, date, reportName);
                        tempHCArray.add(tempCard);
                    }
                    //sort the array so that the new hashmap is ordered from newest to oldest
                    Collections.sort(tempHCArray, new HomeCardSortDate());
                    //add expense to the hashmap in HCardDB
                    HCardDB.clearMap();
                    for (HomeCard hc : tempHCArray) {
                        HCardDB.addExpense(hc.getId(), hc);
                    }
                    //load reports in the UI
                    loadReportsFromArrayList();
                    Log.d("firebase", String.valueOf(task.getResult().getValue()));
                }
            }
        });
        //fetch settings from Firebase db
        myRef.child("settings").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    for (DataSnapshot child : task.getResult().getChildren()) {

                        String tableName = child.getKey().toString();
                        String name1 = child.child("name1").getValue().toString();
                        String name2 = child.child("name2").getValue().toString();
                        Integer sueldo1 = Integer.parseInt(child.child("sueldo1").getValue().toString());
                        Integer sueldo2 = Integer.parseInt(child.child("sueldo2").getValue().toString());

                        Settings tempSettings = new Settings(tableName, name1, sueldo1, name2, sueldo2);
                        SettingsDB.addToDB(tempSettings);
                    }
                    Log.d("firebase", String.valueOf(task.getResult().getValue()));
                }
            }
        });


    }

    private void loadReportsFromArrayList() {
        if(!HCardDB.isEmpty()) {
            System.out.println("HCardDB not empty");
            //hide progressBar
            progressBar.hide();
            progressBar.setVisibility(View.INVISIBLE);
            hCards = HCardDB.getReports();
            adapter = new HCardsViewAdapter(MainActivity.this);
            adapter.setCards(hCards);
            homeRecycler.setAdapter(adapter);
            homeRecycler.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        }
        else {
            TextView txtEmpty = new TextView(MainActivity.this);
            txtEmpty.setText("There are no reports yet!");
            System.out.println("HCardDB empty");
        }

    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 50);
        progressBar.show();
        progressBar.setVisibility(View.VISIBLE);
        loadReportsFromFirebase();
    }
}