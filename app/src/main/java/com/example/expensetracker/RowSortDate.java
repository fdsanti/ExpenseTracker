package com.example.expensetracker;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Comparator;

public class RowSortDate implements Comparator<ExpenseRow> {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int compare(ExpenseRow row1, ExpenseRow row2) {
        //System.out.println("Row 1 : " + row1.getLocalDate().toString() + "  -  Row2: " + row2.getLocalDate().toString());
        return row1.getLocalDate().compareTo(row2.getLocalDate());
    }
}
