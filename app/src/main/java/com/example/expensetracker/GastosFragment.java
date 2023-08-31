package com.example.expensetracker;


import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GastosFragment extends Fragment implements CallBackItemTouch, SwipeRefreshLayout.OnRefreshListener {

    private Context context;
    private RecyclerView recyclerView;
    private RowViewAdapter adapter;
    private Connection connection;
    private DBHandler handler;
    private LinearProgressIndicator progressBar;
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
    private ImageView icon_sort;
    private int sort_menu_selected = 1;
    private MenuBuilder menuBuilder;

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

    @SuppressLint({"ClickableViewAccessibility", "RestrictedApi", "ResourceType"})
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
                rows = rows1;
            }
            if (!chip_1.isChecked()) {
                adapter.changeToBoth();
                rows = rowsBoth;
            }
        });
        chip_2.setOnClickListener(v -> {
            if (chip_2.isChecked()) {
                adapter.changeToName2();
                rows = rows2;
            }
            if (!chip_2.isChecked()) {
                adapter.changeToBoth();
                rows = rowsBoth;
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
            SimpleDateFormat simpleFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            date = new Date();
            txt_FechaGasto.setText(simpleFormat.format(date));
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

            //cuando haces click en "crear" subir la base de datos si...
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
                        ExpenseRow newRow = new ExpenseRow(txt_NombreGasto.getText().toString(), date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), Double.parseDouble(txt_Gasto.getText().toString()), dropdown_nombres.getText().toString());
                        alertDialog.dismiss();
                        //show progressBar
                        progressBar.show();
                        progressBar.setVisibility(View.VISIBLE);

                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        DatabaseReference myRef = database.getReference();
                        myRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DataSnapshot> task) {
                                if (!task.isSuccessful()) {
                                    Toast.makeText(context, "Error de conectividad", Toast.LENGTH_SHORT).show();
                                    Log.e("firebase", "Error getting data", task.getException());
                                }
                                else {
                                    //Upload new row
                                    //Find biggest id
                                    int biggest = 0;
                                    for (DataSnapshot row : task.getResult().child(HCardDB.getSelected().getTableID()).getChildren()) {
                                        System.out.println(row.getKey());
                                        Integer intID = Integer.parseInt(row.getKey().toString());
                                        if (intID > biggest) {
                                            biggest = intID;
                                        }
                                    }
                                    newRow.setId(biggest+1);
                                    System.out.println(newRow);
                                    myRef.child(HCardDB.getSelected().getTableID().toString()).child(String.valueOf(newRow.getId())).child("Date").setValue(newRow.getLocalDate().toString());
                                    myRef.child(HCardDB.getSelected().getTableID().toString()).child(String.valueOf(newRow.getId())).child("Description").setValue(newRow.getDescription().toString());
                                    myRef.child(HCardDB.getSelected().getTableID().toString()).child(String.valueOf(newRow.getId())).child("Value").setValue(newRow.getValue());
                                    myRef.child(HCardDB.getSelected().getTableID().toString()).child(String.valueOf(newRow.getId())).child("Who").setValue(newRow.getWho().toString());

                                    if (dropdown_nombres.getText().toString().equals(SettingsDB.getSetting(HCardDB.getSelected()).getName1())) {
                                        rows1.add(newRow);
                                        if (sort_menu_selected == 1) {
                                            Collections.sort(rows1, new RowSortDate());
                                        }
                                        else if (sort_menu_selected == 2) {
                                            Collections.sort(rows1, new RowSortPrice().reversed());
                                        }
                                        else {
                                            Collections.sort(rows1, new RowSortPrice());
                                        }

                                    }
                                    else {
                                        rows2.add(newRow);
                                        if (sort_menu_selected == 1) {
                                            Collections.sort(rows2, new RowSortDate());
                                        }
                                        else if (sort_menu_selected == 2) {
                                            Collections.sort(rows2, new RowSortPrice().reversed());
                                        }
                                        else {
                                            Collections.sort(rows2, new RowSortPrice());
                                        }
                                    }
                                    rowsBoth.add(newRow);
                                    if (sort_menu_selected == 1) {
                                        Collections.sort(rowsBoth, new RowSortDate());
                                    }
                                    else if (sort_menu_selected == 2) {
                                        Collections.sort(rowsBoth, new RowSortPrice().reversed());
                                    }
                                    else {
                                        Collections.sort(rowsBoth, new RowSortPrice());
                                    }

                                    int currPos = 0;

                                    //Find position of the item added
                                    if (!chip_1.isChecked() && !chip_2.isChecked()) {
                                        for (ExpenseRow currRow : rowsBoth) {
                                            if (currRow.getId() == newRow.getId()) {
                                                break;
                                            }
                                            currPos += 1;
                                        }
                                    }
                                    if (chip_1.isChecked()) {
                                        for (ExpenseRow currRow : rows1) {
                                            if (currRow.getId() == newRow.getId()) {
                                                break;
                                            }
                                            currPos += 1;
                                        }
                                    }
                                    if (chip_2.isChecked()){
                                        for (ExpenseRow currRow : rows2) {
                                            if (currRow.getId() == newRow.getId()) {
                                                break;
                                            }
                                            currPos += 1;
                                        }
                                    }

                                    adapter.notifyItemInserted(currPos);
                                    adapter.notifyItemRangeChanged(currPos,rows.size());
                                    if (currPos == 0) {
                                        recyclerView.smoothScrollToPosition(0);
                                    }

                                    //hide progressBar
                                    progressBar.hide();
                                    progressBar.setVisibility(View.INVISIBLE);
                                    loadTotals();
                                    saldosFragment.calculate();

                                    Toast.makeText(context, "¡El expense ha sido creado con éxito!", Toast.LENGTH_SHORT).show();
                                    Log.d("firebase", String.valueOf(task.getResult().getValue()));
                                }
                            }
                        });
                    }
                }
            });

        });


        //Sorting happening here
        menuBuilder = new MenuBuilder(context);
        MenuInflater inflater = new MenuInflater(context);
        inflater.inflate(R.menu.sort_popupmenu,menuBuilder);
        icon_sort.setOnClickListener(v -> {

            MenuPopupHelper popupHelper = new MenuPopupHelper(context, menuBuilder, v);
            popupHelper.setForceShowIcon(true);
            popupHelper.setGravity(Gravity.END);

            TypedValue typedValue = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.whiteBlack, typedValue, true);
            TypedValue typedValueTransparent = new TypedValue();
            getContext().getTheme().resolveAttribute(R.color.transparent_100, typedValueTransparent, true);

            //setting 1st icon as checked and color

            for (int i = 1; i<4; i++) {
                if (sort_menu_selected == i) {
                    menuBuilder.getItem(i-1).setIcon(R.drawable.ic_check);
                    menuBuilder.getItem(i-1).getIcon().setTint(typedValue.data);
                }
                if (sort_menu_selected != i) {
                    menuBuilder.getItem(i-1).setIcon(R.drawable.rectangle_2);
                    menuBuilder.getItem(i-1).getIcon().setTint(typedValueTransparent.data);
                }
            }

            menuBuilder.setCallback(new MenuBuilder.Callback() {
                @Override
                public boolean onMenuItemSelected(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.sort_item_one:
                            sort_menu_selected = 1;
                            Collections.sort(rows1, new RowSortDate());
                            Collections.sort(rows2, new RowSortDate());
                            Collections.sort(rowsBoth, new RowSortDate());
                            adapter.notifyDataSetChanged();
                            return true;

                        case R.id.sort_item_two:
                            sort_menu_selected = 2;
                            Collections.sort(rows1, new RowSortPrice().reversed());
                            Collections.sort(rows2, new RowSortPrice().reversed());
                            Collections.sort(rowsBoth, new RowSortPrice().reversed());
                            adapter.notifyDataSetChanged();
                            return true;

                        case R.id.sort_item_three:
                            sort_menu_selected = 3;
                            Collections.sort(rows1, new RowSortPrice());
                            Collections.sort(rows2, new RowSortPrice());
                            Collections.sort(rowsBoth, new RowSortPrice());
                            adapter.notifyDataSetChanged();
                            return true;
                    }

                    return false;
                }

                @Override
                public void onMenuModeChange(@NonNull MenuBuilder menu) {

                }
            });

            popupHelper.show();

        });

        //OnDataChange - Disabled for now. Need to think this through
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference();
        myRef.child(HCardDB.getSelected().getTableID().toString()).addValueEventListener(new ValueEventListener() {
            int numberOfChanges = 0;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                //String value = dataSnapshot.getValue(String.class);
                if (numberOfChanges > 0) {
                    //Toast.makeText(context, "Hay un update", Toast.LENGTH_SHORT).show();
                }
                numberOfChanges += 1;
                //Log.d(TAG, "Value is: " + value);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
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
        System.out.println("Total 1: " + str_total1);
        System.out.println("Total 2: " + str_total2);

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
        icon_sort = view.findViewById(R.id.icon_sort);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void loadRows(View view) {
        System.out.println("Loading rows");

        rows = new ArrayList<ExpenseRow>();
        rows1 = new ArrayList<ExpenseRow>();
        rows2 = new ArrayList<ExpenseRow>();
        rowsBoth = new ArrayList<ExpenseRow>();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference();
        //fetch rows from Firebase db
        myRef.child(HCardDB.getSelected().getTableID()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    for (DataSnapshot currRow : task.getResult().getChildren()) {
                        ExpenseRow row = new ExpenseRow();
                        row.setId(Integer.parseInt(currRow.getKey()));
                        row.setDescription(currRow.child("Description").getValue(String.class));
                        LocalDate newDate = LocalDate.parse(currRow.child("Date").getValue(String.class));
                        row.setDate(newDate);
                        row.setValue(currRow.child("Value").getValue(Long.class).doubleValue());
                        row.setWho(currRow.child("Who").getValue(String.class));

                        rowsBoth.add(row);
                        if (currRow.child("Who").getValue().equals(SettingsDB.getSetting(HCardDB.getSelected()).getName1())) {
                            rows1.add(row);
                        }
                        if (currRow.child("Who").getValue().equals(SettingsDB.getSetting(HCardDB.getSelected()).getName2())) {
                            rows2.add(row);
                        }
                    }
                    rows = rowsBoth;
                    Collections.sort(rowsBoth, new RowSortDate());
                    if (!rows1.isEmpty()) {
                        Collections.sort(rows1, new RowSortDate());
                    }
                    if (!rows2.isEmpty()) {
                        Collections.sort(rows2, new RowSortDate());
                    }

                    adapter = new RowViewAdapter(context);
                    adapter.passFragment(GastosFragment.this);
                    adapter.setRows(rows, rows1, rows2, rowsBoth);
                    adapter.changeToBoth();

                    //hide progressBar
                    progressBar.hide();
                    progressBar.setVisibility(View.INVISIBLE);

                    recyclerView = view.findViewById(R.id.expenseRecycler);
                    recyclerView.setAdapter(adapter);
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                    ItemTouchHelper.Callback callback = new ExpenseItemSwipeCallback(GastosFragment.this);
                    ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
                    touchHelper.attachToRecyclerView(recyclerView);
                    loadTotals();
                    saldosFragment.calculate();

                    Log.d("firebase", String.valueOf(task.getResult().getValue()));
                }
            }
        });
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
        rows = rowsBoth;
        Collections.sort(rowsBoth, new RowSortDate());
        if (!rows1.isEmpty()) {
            Collections.sort(rows1, new RowSortDate());
        }
        if (!rows2.isEmpty()) {
            Collections.sort(rows2, new RowSortDate());
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
        loadRows(getView());
        chip_1.setChecked(false);
        chip_2.setChecked(false);
    }

    @Override
    public void itemTuchOnMove(int oldPosition, int newPosition) {
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        int position = viewHolder.getAdapterPosition();


        System.out.println(position);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle("¿Eliminar " + rows.get(position).getDescription() + "?");
        dialog.setMessage("¿Estás seguro que querés eliminar esta línea del expense?");


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
                myRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show();
                            Log.e("firebase", "Error getting data", task.getException());
                        }
                        else {
                            myRef.child(HCardDB.getSelected().getTableID()).child(String.valueOf(rows.get(position).getId())).removeValue();

                            ArrayList<ExpenseRow> copyRows;
                            copyRows = (ArrayList<ExpenseRow>) rows.clone();

                            //si la row es de Name1, entonces eliminar la row de la rows1
                            if (rows.get(position).getWho().equals(SettingsDB.getSetting(HCardDB.getSelected()).getName1())) {
                                for (ExpenseRow currRow : rows1) {
                                    //encontrar la row con la misma id que la row seleccionada
                                    if (currRow.getId() == copyRows.get(position).getId()) {
                                        rows1.remove(currRow);
                                        break;
                                    }
                                }
                            }
                            //si la row es de Name2, entonces eliminar la row de la rows2
                            if (rows.get(position).getWho().equals(SettingsDB.getSetting(HCardDB.getSelected()).getName2())) {
                                for (ExpenseRow currRow : rows2) {
                                    //encontrar la row con la misma id que la row seleccionada
                                    if (currRow.getId() == copyRows.get(position).getId()) {
                                        rows2.remove(currRow);
                                        break;
                                    }
                                }
                            }
                            //eliminar de la rowsBoth
                            for (ExpenseRow currRow : rowsBoth) {
                                if (currRow.getId() == copyRows.get(position).getId()) {
                                    rowsBoth.remove(currRow);
                                    break;
                                }
                            }

                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position,rows.size());


                            //adapter.notifyDataSetChanged();
                            loadTotals();
                            saldosFragment.calculate();
                            Toast.makeText(context, "¡El expense ha sido eliminado con éxito!", Toast.LENGTH_SHORT).show();
                            Log.d("firebase", String.valueOf(task.getResult().getValue()));
                        }
                    }
                });
            }
        });

        dialog.show();
    }
}
