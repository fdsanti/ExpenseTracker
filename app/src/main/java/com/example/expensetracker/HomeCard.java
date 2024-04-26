package com.example.expensetracker;

import java.time.LocalDate;

public class HomeCard {

    private String name;
    private LocalDate creationDate;
    private String id;
    private String tableID;
    private Boolean cerrado;

    public HomeCard(String id, LocalDate creationDate, String name, Boolean cerrado) {
        this.name = name;
        this.creationDate = creationDate;
        this.id = id;
        tableID = "DATA" + id;
        this.cerrado = cerrado;
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
