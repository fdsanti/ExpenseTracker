package com.example.expensetracker;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Comparator;

public class HomeCardSortDate implements Comparator<HomeCard> {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int compare(HomeCard hc1, HomeCard hc2) {
        return hc1.getCreationDate().compareTo(hc2.getCreationDate());
    }
}