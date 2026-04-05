package com.example.expensetracker.model;

public class Tracker {

    private String id;
    private String name;
    private long createdAt;
    private boolean closed;

    public Tracker() {
    }

    public Tracker(String id, String name, long createdAt, boolean closed) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.closed = closed;
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
}
