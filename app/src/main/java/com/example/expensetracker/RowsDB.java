package com.example.expensetracker;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class RowsDB {
    public static HashMap<String, ArrayList<ExpenseRow>> rowsMap;
    private Connection connection;
    private DBHandler handler;
    private ProgressDialog progressDialog;


    public static void loadRows(Context context, Connection connection, ArrayList<HomeCard> homeCards) {
        HashMap<String, ArrayList<ExpenseRow>> tempRowMap = new HashMap<String, ArrayList<ExpenseRow>>();
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Handler handlerUI = new Handler();
        Runnable runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                synchronized (this) {
                    getRows(connection, homeCards);
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
                    }
                });
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public static HashMap<String, ArrayList<ExpenseRow>> getRowsMap() {
        return rowsMap;
    }

    public static boolean isEmpty() {
        return rowsMap.isEmpty();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void getRows(Connection connection, ArrayList<HomeCard> homeCards) {
        rowsMap = new HashMap<String, ArrayList<ExpenseRow>>();
        for(HomeCard card : homeCards) {
            ArrayList<ExpenseRow> tempRow = new ArrayList<ExpenseRow>();
            String query = "SELECT * FROM " + card.getTableID();

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
                    tempRow.add(row);
                }
                rowsMap.put(card.getTableID(), tempRow);

            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }
}
