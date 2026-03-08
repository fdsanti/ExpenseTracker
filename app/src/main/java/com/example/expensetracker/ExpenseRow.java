package com.example.expensetracker;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class ExpenseRow {
    private String description;
    private LocalDate date1;
    private String date;
    private Double value;
    private String who;
    private String id;
    private String category;

    public ExpenseRow() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setDate(Date date) {
        this.date1 = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        this.date = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setDate(LocalDate date) {
        this.date1 = date;
        this.date = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(date);
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public void setWho(String who) {
        this.who = who;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public LocalDate getLocalDate() {
        return date1;
    }

    public double getValue() {
        return value;
    }

    public String getWho() {
        return who;
    }

    public String getCategory() {
        // Fallback for old expenses that don't have this field yet
        return (category == null || category.isEmpty()) ? "Otros" : category;
    }

    public void setCategory(String category) {
        this.category = category;
    }


}
