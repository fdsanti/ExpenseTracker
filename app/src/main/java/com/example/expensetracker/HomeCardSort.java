package com.example.expensetracker;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Comparator;

public class HomeCardSort implements Comparator<HomeCard> {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int compare(HomeCard hc1, HomeCard hc2) {
        //System.out.println("Row 1 : " + row1.getLocalDate().toString() + "  -  Row2: " + row2.getLocalDate().toString());
        return hc1.isCerrado().compareTo(hc2.isCerrado());
    }
}
