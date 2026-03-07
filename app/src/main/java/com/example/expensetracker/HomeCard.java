package com.example.expensetracker;

import java.time.LocalDate;

public class HomeCard {

    private String name;
    private LocalDate creationDate;
    private String id;
    private String tableID;
    private Boolean cerrado;

    public HomeCard(String id, LocalDate creationDate, String name, Boolean cerrado) {
        this(id, "DATA" + id, creationDate, name, cerrado);
    }

    public HomeCard(String id, String tableID, LocalDate creationDate, String name, Boolean cerrado) {
        this.name = name;
        this.creationDate = creationDate;
        this.id = id;
        this.tableID = tableID;
        this.cerrado = cerrado;
    }

    public static HomeCard fromTrackerId(String trackerId, LocalDate creationDate, String name, Boolean cerrado) {
        String cleanId = trackerId;
        if (trackerId != null && trackerId.startsWith("DATA")) {
            cleanId = trackerId.substring(4);
        }
        return new HomeCard(cleanId, trackerId, creationDate, name, cerrado);
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
}
