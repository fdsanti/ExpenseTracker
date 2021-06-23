package com.example.expensetracker;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GastosFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private Context context;
    private RecyclerView recyclerView;
    private RowViewAdapter adapter;
    private Connection connection;
    private DBHandler handler;
    public static ArrayList<ExpenseRow> rows;
    public static ArrayList<ExpenseRow> rows1;
    public static ArrayList<ExpenseRow> rows2;
    public static ArrayList<ExpenseRow> rowsBoth;
    private Chip chip_1;
    private Chip chip_2;
    private FloatingActionButton btn_fab;
    private Date date;
    private ProgressDialog progressDialog;
    private TextView txt_abajo_nombre1;
    private TextView txt_abajo_total1;
    private TextView txt_abajo_nombre2;
    private TextView txt_abajo_total2;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private SaldosFragment saldosFragment;


    public GastosFragment() {
    }

    public void setSaldosFragment(SaldosFragment fragment) {
        saldosFragment = fragment;
    }

    public ArrayList<ExpenseRow> getRows1() {
        return rows1;
    }

    public ArrayList<ExpenseRow> getRows2() {
        return rows2;
    }

    public ArrayList<ExpenseRow> getRowsBoth() {
        return rowsBoth;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gastos,container,false);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);


        return rootView;
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadVariables(view);
        loadRows(view);
        loadNames();

        //Al hacer click en los filtros, filtrar
        chip_1.setOnClickListener(v -> {
            if (chip_1.isChecked()) {
                adapter.changeToName1();
            }
            if (!chip_1.isChecked()) {
                adapter.changeToBoth();
            }
        });
        chip_2.setOnClickListener(v -> {
            if (chip_2.isChecked()) {
                adapter.changeToName2();
            }
            if (!chip_2.isChecked()) {
                adapter.changeToBoth();
            }
        });

        //al hacer click en el FAB para agregar un gasto
        btn_fab.setOnClickListener(v -> {

            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
            dialog.setCancelable(false);
            dialog.setTitle("Agregar expense");
            //Add input field to dialog
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.input_newrow, null);
            dialog.setView(dialogView);
            dialog.setNegativeButton("Cancelar", null);
            dialog.setPositiveButton("Crear", null);
            AlertDialog alertDialog = dialog.create();
            alertDialog.show();


            TextInputLayout inputLayout_Who = dialogView.findViewById(R.id.inputLayout_Who);
            TextInputLayout inputLayout_NombreGasto = dialogView.findViewById(R.id.inputLayout_NombreGasto);
            TextInputLayout inputLayout_FechaGasto = dialogView.findViewById(R.id.inputLayout_FechaGasto);
            TextInputLayout inputLayout_Gasto = dialogView.findViewById(R.id.inputLayout_Gasto);

            AutoCompleteTextView dropdown_nombres = dialogView.findViewById(R.id.dropdown_nombres);
            TextInputEditText txt_NombreGasto = dialogView.findViewById(R.id.editText_NombreGasto);
            TextInputEditText txt_FechaGasto = dialogView.findViewById(R.id.editText_FechaGasto);
            TextInputEditText txt_Gasto = dialogView.findViewById(R.id.editText_Gasto);


            //hacer que si hay error, cuando toques de vuelta para escribir otra cosa, el error desaparezca
            dropdown_nombres.setOnClickListener(v1 -> {
                if (inputLayout_Who.isErrorEnabled()) {
                    inputLayout_Who.setErrorEnabled(false);
                }
            });
            txt_NombreGasto.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (inputLayout_NombreGasto.isErrorEnabled()) {
                        inputLayout_NombreGasto.setErrorEnabled(false);
                    }
                    return false;
                }
            });
            txt_Gasto.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (inputLayout_Gasto.isErrorEnabled()) {
                        inputLayout_Gasto.setErrorEnabled(false);
                    }
                    return false;
                }
            });


            String[] nombres = new String[2];
            nombres[0] = SettingsDB.getSetting(HCardDB.getSelected()).getName1();
            nombres[1] = SettingsDB.getSetting(HCardDB.getSelected()).getName2();

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, nombres);
            dropdown_nombres.setAdapter(arrayAdapter);

            txt_FechaGasto.setInputType(InputType.TYPE_NULL);
            txt_FechaGasto.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (inputLayout_FechaGasto.isErrorEnabled()) {
                        inputLayout_FechaGasto.setErrorEnabled(false);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        MaterialDatePicker.Builder builder = MaterialDatePicker.Builder.datePicker();
                        builder.setTitleText("Select Date");
                        builder.setSelection(MaterialDatePicker.todayInUtcMilliseconds());
                        MaterialDatePicker materialDatePicker = builder.build();

                        if (getFragmentManager() != null) {
                            materialDatePicker.show(getFragmentManager(), "MATERIAL_DATE_PICKER");
                        }

                        materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                            @Override public void onPositiveButtonClick(Long selection) {
                                TimeZone timeZoneUTC = TimeZone.getDefault();
                                // It will be negative, so that's the -1
                                int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;

                                // Create a date format, then a date object with our offset
                                SimpleDateFormat simpleFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                                date = new Date(selection + offsetFromUTC);
                                txt_FechaGasto.setText(simpleFormat.format(date));
                            }
                        });
                    }

                    return false;
                }
            });

            //cuando haces click en el FAB, abrir modal
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dropdown_nombres.getText().toString().isEmpty()) {
                        inputLayout_Who.setErrorEnabled(true);
                        inputLayout_Who.setError("Es necesario elegir a alguien");
                        inputLayout_Who.setErrorIconDrawable(R.drawable.ic_info);
                    }
                    if (txt_NombreGasto.getText().toString().isEmpty()) {
                        inputLayout_NombreGasto.setErrorEnabled(true);
                        inputLayout_NombreGasto.setError("Es necesario elegir un nombre.");
                        inputLayout_NombreGasto.setErrorIconDrawable(R.drawable.ic_info);
                    }
                    if (txt_FechaGasto.getText().toString().isEmpty()) {
                        inputLayout_FechaGasto.setErrorEnabled(true);
                        inputLayout_FechaGasto.setError("Es necesario elegir una fecha");
                        inputLayout_FechaGasto.setErrorIconDrawable(R.drawable.ic_info);
                    }
                    if (txt_Gasto.getText().toString().isEmpty()) {
                        inputLayout_Gasto.setErrorEnabled(true);
                        inputLayout_Gasto.setError("Es necesario agregar un precio");
                        inputLayout_Gasto.setErrorIconDrawable(R.drawable.ic_info);
                    }

                    if (!txt_NombreGasto.getText().toString().isEmpty()
                    && !txt_FechaGasto.getText().toString().isEmpty()
                    && !txt_Gasto.getText().toString().isEmpty()
                    && !dropdown_nombres.getText().toString().isEmpty()) {
                        progressDialog.show();
                        ExpenseRow newRow = new ExpenseRow(txt_NombreGasto.getText().toString(), date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), Double.parseDouble(txt_Gasto.getText().toString()), dropdown_nombres.getText().toString());

                        Handler handlerUI = new Handler();
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {

                                synchronized (this) {
                                    handler = new DBHandler();
                                    connection = handler.getConnection(context);

                                    String insert = "INSERT INTO " + SettingsDB.getSetting(HCardDB.getSelected()).getTableID() + "(Description,Date,Value,Who)" + "VALUES (?,?,?,?)";
                                    PreparedStatement pst = null;
                                    try {
                                        pst = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                                        pst.setString(1, newRow.getDescription());
                                        pst.setDate(2, java.sql.Date.valueOf(newRow.getLocalDate().toString()));
                                        pst.setDouble(3, newRow.getValue());
                                        pst.setString(4, newRow.getWho());
                                        pst.executeUpdate();
                                        ResultSet rs = pst.getGeneratedKeys();
                                        if(rs.next()) {
                                            int last_inserted_id = rs.getInt(1);
                                            newRow.setId(last_inserted_id);
                                        }
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

                                handlerUI.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //since the array list within the adapter is referencing the arraylist in this class, we
                                        //just need to add the new row to the arraylists in this class
                                        if (dropdown_nombres.getText().toString().equals(SettingsDB.getSetting(HCardDB.getSelected()).getName1())) {
                                            rows1.add(newRow);
                                        }
                                        else {
                                            rows2.add(newRow);
                                        }
                                        rowsBoth.add(newRow);
                                        adapter.notifyDataSetChanged();
                                        loadTotals();
                                        saldosFragment.calculate();
                                        progressDialog.dismiss();
                                        alertDialog.dismiss();
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

    private void loadNames() {
        txt_abajo_nombre1.setText(SettingsDB.getSetting(HCardDB.getSelected()).getName1());
        txt_abajo_nombre2.setText(SettingsDB.getSetting(HCardDB.getSelected()).getName2());
    }

    public void loadTotals() {
        //sumar total de cada uno y setear el texto de abajo de todo

        double total1 = 0;
        double total2 = 0;

        for (ExpenseRow row : rows1) {
            total1 += row.getValue();
        }

        for (ExpenseRow row : rows2) {
            total2 += row.getValue();
        }

        String COUNTRY = "US";
        String LANGUAGE = "en";
        String str_total1 = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(total1);
        String str_total2 = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(total2);

        txt_abajo_total1.setText(str_total1);
        txt_abajo_total2.setText(str_total2);

    }

    public SaldosFragment getSaldosFragment() {
        return saldosFragment;
    }

    private void loadVariables(View view) {
        context = getContext();
        txt_abajo_nombre1 = view.findViewById(R.id.txt_abajo_nombre1);
        txt_abajo_total1 = view.findViewById(R.id.txt_abajo_total1);
        txt_abajo_nombre2 = view.findViewById(R.id.txt_abajo_nombre2);
        txt_abajo_total2 = view.findViewById(R.id.txt_abajo_total2);
        btn_fab = view.findViewById(R.id.btn_fab);
        chip_1 = view.findViewById(R.id.chip_1);
        chip_2 = view.findViewById(R.id.chip_2);
        chip_1.setText(SettingsDB.getSetting(HCardDB.getSelected()).getName1());
        chip_2.setText(SettingsDB.getSetting(HCardDB.getSelected()).getName2());
    }

    private void loadRows(View view) {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Handler handlerUI = new Handler();
        Runnable runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                synchronized (this) {
                    getRows();
                }

                handlerUI.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter = new RowViewAdapter(context);
                        adapter.passFragment(GastosFragment.this);
                        adapter.setRows(rows, rows1, rows2, rowsBoth);
                        adapter.changeToBoth();
                        recyclerView = view.findViewById(R.id.expenseRecycler);
                        recyclerView.setAdapter(adapter);
                        recyclerView.setLayoutManager(new LinearLayoutManager(context));
                        loadTotals();
                        saldosFragment.calculate();
                        progressDialog.dismiss();
                    }
                });
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getRows() {
        rows = new ArrayList<ExpenseRow>();
        rows1 = new ArrayList<ExpenseRow>();
        rows2 = new ArrayList<ExpenseRow>();
        rowsBoth = new ArrayList<ExpenseRow>();


        handler = new DBHandler();
        connection = handler.getConnection(context);

        String query = "SELECT * FROM " + HCardDB.getSelected().getTableID();

        try {
            ResultSet set = connection.createStatement().executeQuery(query);
            while (set.next()) {
                //Create a row and add it to the arraylist
                ExpenseRow row = new ExpenseRow();
                row.setId(set.getInt("id"));
                row.setDescription(set.getString("Description"));
                row.setDate(set.getDate("Date"));
                row.setValue(set.getDouble("Value"));
                row.setWho(set.getString("Who"));
                rowsBoth.add(row);
                rows.add(row);
                if (set.getString("Who").equals(SettingsDB.getSetting(HCardDB.getSelected()).getName1())) {
                    rows1.add(row);
                }
                if (set.getString("Who").equals(SettingsDB.getSetting(HCardDB.getSelected()).getName2())) {
                    rows2.add(row);
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    @Override
    public void onRefresh() {

        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 50);

        loadRows(getView());
        chip_1.setChecked(false);
        chip_2.setChecked(false);
    }
}
