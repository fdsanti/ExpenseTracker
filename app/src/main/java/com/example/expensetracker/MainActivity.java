package com.example.expensetracker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements CallBackItemTouch, SwipeRefreshLayout.OnRefreshListener{

    private Toolbar toolbar;
    private RecyclerView homeRecycler;
    private Connection connection;
    private DBHandler handler;
    private ProgressDialog progressDialog;
    private ExtendedFloatingActionButton btnNewHomeCard;
    private HCardsViewAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ArrayList<HomeCard> hCards;
    private Drawable mIcon;

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
        setSupportActionBar(toolbar);
        homeRecycler = findViewById(R.id.homeRecycler);
        //btnNewHomeCard = findViewById(R.id.btnNewHomeCard);


        if (HCardDB.isNull()) {
            loadReportsFromDB();
        }
        if (!HCardDB.isNull()) {
            loadReportsFromArrayList();
        }

        //Click listener for "Crear nuevo expense" btn

        ItemTouchHelper.Callback callback = new ItemDragSwipeCallback(this);
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
        return super.onOptionsItemSelected(item);
    }


    //al swipear, eliminar el expense
    /*private ItemTouchHelper.Callback createHelperCallback() {
        return new ItemDragSwipeCallback(this, R.color.delete_red, R.drawable.ic_delete,
                0, ItemTouchHelper.LEFT, new ItemDragSwipeCallback.OnTouchListener() {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
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
                                                adapter.notifyDataSetChanged();
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
        });
    }*/



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
                                progressDialog.dismiss();
                                adapter.notifyItemRemoved(position);
                                adapter.notifyItemRangeChanged(position,hCards.size());

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
                        progressDialog.dismiss();
                        loadReportsFromArrayList();
                    }
                });
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void loadReportsFromArrayList() {
        if(!HCardDB.isEmpty()) {
            hCards = HCardDB.getReports();
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