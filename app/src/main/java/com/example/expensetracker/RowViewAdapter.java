package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RowViewAdapter extends RecyclerView.Adapter<RowViewAdapter.ViewHolder> {

    private Context context;
    private DBHandler handler;
    private Connection connection;
    private LocalDate date;
    private GastosFragment fragment;
    private ArrayList<ExpenseRow> rows = new ArrayList<ExpenseRow>(), rows1 = new ArrayList<ExpenseRow>(), rows2 = new ArrayList<ExpenseRow>(), rowsBoth = new ArrayList<ExpenseRow>();

    public RowViewAdapter(Context context) {
        this.context = context;
    }

    public void passFragment(GastosFragment fragment) {
        this.fragment = fragment;
    }

    public void setRows(ArrayList<ExpenseRow> rows, ArrayList<ExpenseRow> rows1, ArrayList<ExpenseRow> rows2, ArrayList<ExpenseRow> rowsBoth) {
        this.rows = rows;
        this.rows1 = rows1;
        this.rows2 = rows2;
        this.rowsBoth = rowsBoth;
        notifyDataSetChanged();
    }

    public int whichRow() {
        if (rows == rowsBoth) {
            return 0;
        }
        else if (rows == rows1) {
            return 1;
        }
        return 2;
    }


    public void changeToName1() {
        rows = rows1;
        notifyDataSetChanged();
    }

    public void changeToName2() {
        rows = rows2;
        notifyDataSetChanged();
    }

    public void changeToBoth() {
        rows = rowsBoth;
        notifyDataSetChanged();
    }

    public void addToRow1(ExpenseRow row) {
        rows1.add(row);
        rowsBoth.add(row);
        notifyDataSetChanged();
    }

    public void addToRow2(ExpenseRow row) {
        rows2.add(row);
        rowsBoth.add(row);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.expense_row, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @SuppressLint({"ClickableViewAccessibility", "ResourceType"})
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.txtNombreGasto.setText(rows.get(position).getDescription());
        holder.txtFechaGasto.setText(rows.get(position).getDate());
        holder.txtPersona.setText(rows.get(position).getWho());
        String COUNTRY = "US";
        String LANGUAGE = "en";
        String str = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(rows.get(position).getValue());
        holder.txtPrecio.setText(str);

        //al hacer click en la card, abrir el dialog para editar la info
        holder.row_fg.setOnClickListener(v -> {

            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
            dialog.setCancelable(false);
            dialog.setTitle("Editar expense");
            //Add input field to dialog
            LayoutInflater inflater = fragment.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.input_newrow, null);
            dialog.setView(dialogView);
            dialog.setNegativeButton("Cancelar", null);
            dialog.setPositiveButton("Guardar", null);
            AlertDialog alertDialog = dialog.create();
            alertDialog.show();

            TextInputLayout inputLayout_Who = dialogView.findViewById(R.id.inputLayout_Who);
            TextInputLayout inputLayout_NombreGasto = dialogView.findViewById(R.id.inputLayout_NombreGasto);
            TextInputLayout inputLayout_FechaGasto = dialogView.findViewById(R.id.inputLayout_FechaGasto);
            TextInputLayout inputLayout_Gasto = dialogView.findViewById(R.id.inputLayout_Gasto);

            AutoCompleteTextView dropdown_nombres = dialogView.findViewById(R.id.dropdown_nombres);
            dropdown_nombres.setText(rows.get(position).getWho());
            dropdown_nombres.setFocusable(false);
            TextInputEditText txt_NombreGasto = dialogView.findViewById(R.id.editText_NombreGasto);
            txt_NombreGasto.setText(rows.get(position).getDescription());
            TextInputEditText txt_FechaGasto = dialogView.findViewById(R.id.editText_FechaGasto);
            txt_FechaGasto.setText(rows.get(position).getDate());
            TextInputEditText txt_Gasto = dialogView.findViewById(R.id.editText_Gasto);
            txt_Gasto.setText(String.valueOf(rows.get(position).getValue()));


            //hacer que si hay error, cuando toques de vuelta para escribir otra cosa, el error desaparezca
            dropdown_nombres.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        inputLayout_Who.setErrorEnabled(false);
                    }
                }
            });
            txt_NombreGasto.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        inputLayout_NombreGasto.setErrorEnabled(false);
                    }
                }
            });
            txt_Gasto.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        inputLayout_Gasto.setErrorEnabled(false);
                    }
                }
            });
            txt_FechaGasto.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        inputLayout_FechaGasto.setErrorEnabled(false);
                    }
                }
            });

            //create date para que si no se cambia, a la hora de editar la row en la base de datos, "date" no sea null
            TimeZone timeZoneUTC = TimeZone.getDefault();
            // It will be negative, so that's the -1
            int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;
            SimpleDateFormat simpleFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            date = rows.get(position).getLocalDate();


            txt_FechaGasto.setInputType(InputType.TYPE_NULL);

            //hacer click en el input field de la fecha
            txt_FechaGasto.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        MaterialDatePicker.Builder builder = MaterialDatePicker.Builder.datePicker();
                        builder.setTitleText("Select Date");
                        builder.setSelection(MaterialDatePicker.todayInUtcMilliseconds());
                        MaterialDatePicker materialDatePicker = builder.build();

                        if (fragment.getFragmentManager() != null) {
                            materialDatePicker.show(fragment.getFragmentManager(), "MATERIAL_DATE_PICKER");
                        }

                        materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override public void onPositiveButtonClick(Long selection) {
                                TimeZone timeZoneUTC = TimeZone.getDefault();
                                // It will be negative, so that's the -1
                                int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;

                                // Create a date format, then a date object with our offset
                                SimpleDateFormat simpleFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                                Date date1 = new Date(selection + offsetFromUTC);
                                date = date1.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                txt_FechaGasto.setText(simpleFormat.format(date1));
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

                        ProgressDialog progressDialog = new ProgressDialog(context);
                        progressDialog.show();
                        progressDialog.setContentView(R.layout.progress_dialog);
                        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                        Handler handlerUI = new Handler();
                        Runnable runnable = new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {

                                // updatear la row en la table en la base de datos
                                synchronized (this) {
                                    handler = new DBHandler();
                                    connection = handler.getConnection(context);

                                    String update = "UPDATE " + SettingsDB.getSetting(HCardDB.getSelected()).getTableID() + " SET Description=?,Date=?,Value=?,Who=? WHERE id=?";

                                    try {
                                        PreparedStatement pst = connection.prepareStatement(update);
                                        pst.setString(1, txt_NombreGasto.getText().toString());
                                        pst.setDate(2, java.sql.Date.valueOf(date.toString()));
                                        pst.setDouble(3, Double.parseDouble(txt_Gasto.getText().toString()));
                                        pst.setString(4, dropdown_nombres.getText().toString());
                                        pst.setInt(5,rows.get(position).getId());
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

                                handlerUI.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //since the array list within the adapter is referencing the arraylist in this class, we
                                        //just need to add the new row to the arraylists in this class

                                        //si el nombre del dropdown es igual a nombre1, entonces editar la row del arraylist rows1
                                        if (dropdown_nombres.getText().toString().equals(SettingsDB.getSetting(HCardDB.getSelected()).getName1())) {
                                            for (ExpenseRow currRow : rows1) {
                                                //encontrar la row con la misma id que la row seleccionada
                                                if (currRow.getId() == rows.get(position).getId()) {
                                                    currRow.setDescription(txt_NombreGasto.getText().toString());
                                                    currRow.setDate(java.sql.Date.valueOf(date.toString()));
                                                    currRow.setValue(Double.parseDouble(txt_Gasto.getText().toString()));
                                                    currRow.setWho(dropdown_nombres.getText().toString());
                                                }
                                            }
                                        }
                                        //si el nombre del dropdown es igual a nombre2, entonces editar la row del arraylist rows2
                                        else {
                                            for (ExpenseRow currRow : rows2) {
                                                //encontrar la row con la misma id que la row seleccionada
                                                if (currRow.getId() == rows.get(position).getId()) {
                                                    currRow.setDescription(txt_NombreGasto.getText().toString());
                                                    currRow.setDate(java.sql.Date.valueOf(date.toString()));
                                                    currRow.setValue(Double.parseDouble(txt_Gasto.getText().toString()));
                                                    currRow.setWho(dropdown_nombres.getText().toString());
                                                }
                                            }
                                        }

                                        //editar la row de rowsBoth
                                        for (ExpenseRow currRow : rowsBoth) {
                                            if (currRow.getId() == rows.get(position).getId()) {
                                                rowsBoth.get(position).setDescription(txt_NombreGasto.getText().toString());
                                                rowsBoth.get(position).setDate(java.sql.Date.valueOf(date.toString()));
                                                rowsBoth.get(position).setValue(Double.parseDouble(txt_Gasto.getText().toString()));
                                                rowsBoth.get(position).setWho(dropdown_nombres.getText().toString());
                                            }
                                        }

                                        notifyDataSetChanged();
                                        fragment.loadTotals();
                                        fragment.getSaldosFragment().calculate();
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



    @Override
    public int getItemCount() {
        return rows.size();
    }

    public void addHCard(ExpenseRow row) {
        rows.add(row);
        notifyDataSetChanged();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView txtNombreGasto;
        private TextView txtFechaGasto;
        private TextView txtPersona;
        private TextView txtPrecio;
        MaterialCardView row_fg, row_bg;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombreGasto = itemView.findViewById(R.id.txtNombreGasto);
            txtFechaGasto = itemView.findViewById(R.id.txtFechaGasto);
            txtPersona = itemView.findViewById(R.id.txtPersona);
            txtPrecio = itemView.findViewById(R.id.txtPrecio);
            row_fg = itemView.findViewById(R.id.row_card);
            row_bg = itemView.findViewById(R.id.row_delete);
        }
    }

}