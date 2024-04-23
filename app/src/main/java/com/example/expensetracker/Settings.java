package com.example.expensetracker;


public class Settings {

    private int income1;
    private String name1;
    private int income2;
    private String name2;
    private String tableID;

    public Settings(String tableID, String name1, int income1, String name2, int income2) {
        this.tableID = tableID;
        this.income1 = income1;
        this.name1 = name1;
        this.income2 = income2;
        this.name2 = name2;
    }

    public int getIncome1() {
        return income1;
    }

    public void setIncome1(int income1) {
        this.income1 = income1;
    }

    public String getName1() {
        return name1;
    }

    public int getIncome(int user) {
        if (user==1) {
            return income1;
        }
        if (user==2) {
            return income2;
        }
        return -1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public int getIncome2() {
        return income2;
    }

    public void setIncome2(int income2) {
        this.income2 = income2;
    }

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }

    public String getTableID() {
        return tableID;
    }

    public void setTableID(String tableID) {
        this.tableID = tableID;
    }
}