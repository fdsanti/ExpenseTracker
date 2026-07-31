package com.example.expensetracker.model;

public class Tracker {
    public static final String TYPE_MONTHLY = "monthly";
    public static final String TYPE_MANUAL = "manual";

    private String id;
    private String name;
    private long createdAt;
    private boolean closed;
    private String type;
    private String monthKey;

    public Tracker() {
    }

    public Tracker(String id, String name, long createdAt, boolean closed) {
        this(id, name, createdAt, closed, TYPE_MONTHLY, null);
    }

    public Tracker(String id, String name, long createdAt, boolean closed, String type, String monthKey) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.closed = closed;
        this.type = type == null || type.trim().isEmpty() ? TYPE_MONTHLY : type;
        this.monthKey = monthKey;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isClosed() {
        return closed;
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
}
