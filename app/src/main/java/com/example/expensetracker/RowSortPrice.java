package com.example.expensetracker;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Comparator;

public class RowSortPrice implements Comparator<ExpenseRow> {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int compare(ExpenseRow row1, ExpenseRow row2) {
        int row1Value = (int) Math.round(row1.getValue());
        int row2Value = (int) Math.round(row2.getValue());
        return row1Value - row2Value;
    }
}
