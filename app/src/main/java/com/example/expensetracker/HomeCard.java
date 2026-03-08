package com.example.expensetracker;

import java.time.LocalDate;

public class HomeCard {

    private String name;
    private LocalDate creationDate;
    private String id;
    private String tableID;
    private Boolean cerrado;
    private boolean isSetupComplete;


    public HomeCard(String tableID, LocalDate creationDate, String name, boolean cerrado, boolean isSetupComplete) {
        this.tableID = tableID;
        this.creationDate = creationDate;
        this.name = name;
        this.cerrado = cerrado;
        this.isSetupComplete = isSetupComplete;

        if (tableID != null && tableID.startsWith("DATA")) {
            this.id = tableID.substring(4);
        } else {
            this.id = tableID;
        }
    }

    public static HomeCard fromTrackerId(String trackerId, LocalDate creationDate, String name, Boolean cerrado, boolean isSetupComplete) {
        return new HomeCard(trackerId, creationDate, name, cerrado, isSetupComplete);
    }

    public String getTableID() {
        return tableID;
    }

    public void setTableID(String tableID) {
        this.tableID = tableID;
    }

    public String getName() {
        return name;
    }

    public Boolean isCerrado() {
        return this.cerrado;
    }

    public void setName(String title) {
        this.name = title;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCerrado(Boolean cerrado) {
        this.cerrado = cerrado;
    }

    public boolean isSetupComplete() {
        return isSetupComplete;
    }
}
