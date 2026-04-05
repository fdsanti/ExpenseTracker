package com.example.expensetracker.model;

public class Category {

    private String id;
    private String name;
    private boolean active;
    private int order;

    public Category() {
    }

    public Category(String id, String name, boolean active, int order) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.order = order;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public int getOrder() {
        return order;
    }
}
