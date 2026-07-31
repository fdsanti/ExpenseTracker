package com.example.expensetracker;

import java.time.LocalDate;

public class HomeCard {
    public static final String TYPE_MONTHLY = "monthly";
    public static final String TYPE_MANUAL = "manual";

    private String name;
    private LocalDate creationDate;
    private String id;
    private String tableID;
    private Boolean cerrado;
    private boolean isSetupComplete;
    private String type;
    private String monthKey;
    private double totalAmount;
    private int summaryVersion;


    public HomeCard(String tableID, LocalDate creationDate, String name, boolean cerrado, boolean isSetupComplete) {
        this(tableID, creationDate, name, cerrado, isSetupComplete, TYPE_MONTHLY, null);
    }

    public HomeCard(
            String tableID,
            LocalDate creationDate,
            String name,
            boolean cerrado,
            boolean isSetupComplete,
            String type,
            String monthKey
    ) {
        this(tableID, creationDate, name, cerrado, isSetupComplete, type, monthKey, 0d, 0);
    }

    public HomeCard(
            String tableID,
            LocalDate creationDate,
            String name,
            boolean cerrado,
            boolean isSetupComplete,
            String type,
            String monthKey,
            double totalAmount
    ) {
        this(tableID, creationDate, name, cerrado, isSetupComplete, type, monthKey, totalAmount, 0);
    }

    public HomeCard(
            String tableID,
            LocalDate creationDate,
            String name,
            boolean cerrado,
            boolean isSetupComplete,
            String type,
            String monthKey,
            double totalAmount,
            int summaryVersion
    ) {
        this.tableID = tableID;
        this.creationDate = creationDate;
        this.name = name;
        this.cerrado = cerrado;
        this.isSetupComplete = isSetupComplete;
        this.type = type == null || type.trim().isEmpty() ? TYPE_MONTHLY : type;
        this.monthKey = monthKey;
        this.totalAmount = totalAmount;
        this.summaryVersion = summaryVersion;

        if (tableID != null && tableID.startsWith("DATA")) {
            this.id = tableID.substring(4);
        } else {
            this.id = tableID;
        }
    }

    public static HomeCard fromTrackerId(String trackerId, LocalDate creationDate, String name, Boolean cerrado, boolean isSetupComplete) {
        return new HomeCard(trackerId, creationDate, name, cerrado, isSetupComplete);
    }

    public static HomeCard fromTrackerId(
            String trackerId,
            LocalDate creationDate,
            String name,
            Boolean cerrado,
            boolean isSetupComplete,
            String type,
            String monthKey
    ) {
        return new HomeCard(trackerId, creationDate, name, cerrado, isSetupComplete, type, monthKey);
    }

    public static HomeCard fromTrackerId(
            String trackerId,
            LocalDate creationDate,
            String name,
            Boolean cerrado,
            boolean isSetupComplete,
            String type,
            String monthKey,
            double totalAmount
    ) {
        return new HomeCard(trackerId, creationDate, name, cerrado, isSetupComplete, type, monthKey, totalAmount);
    }

    public static HomeCard fromTrackerId(
            String trackerId,
            LocalDate creationDate,
            String name,
            Boolean cerrado,
            boolean isSetupComplete,
            String type,
            String monthKey,
            double totalAmount,
            int summaryVersion
    ) {
        return new HomeCard(
                trackerId,
                creationDate,
                name,
                cerrado,
                isSetupComplete,
                type,
                monthKey,
                totalAmount,
                summaryVersion
        );
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

    public void setSetupComplete(boolean setupComplete) {
        isSetupComplete = setupComplete;
    }

    public String getType() {
        return type;
    }

    public String getMonthKey() {
        return monthKey;
    }

    public boolean isMonthly() {
        return type == null || TYPE_MONTHLY.equals(type);
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getSummaryVersion() {
        return summaryVersion;
    }

    public void setSummaryVersion(int summaryVersion) {
        this.summaryVersion = summaryVersion;
    }
}
