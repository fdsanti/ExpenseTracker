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
    public int id;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public ExpenseRow(String description, LocalDate date, double value, String who) {
        this.description = description;
        this.date1 = date;
        this.value = value;
        this.who = who;
        this.date = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(date);
    }

    public ExpenseRow() {
    }

    public void setId(int id) {
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

    public void setDate(String date) {
        this.date = date;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public void setWho(String who) {
        this.who = who;
    }

    public int getId() {
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
}
