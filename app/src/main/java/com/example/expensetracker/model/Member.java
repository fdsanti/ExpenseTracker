package com.example.expensetracker.model;

public class Member {

    private String id;
    private String name;
    private String userId;
    private double salary;
    private boolean incomePending;

    public Member() {
    }

    public Member(String id, String name, double salary) {
        this(id, name, null, salary, false);
    }

    public Member(String id, String name, String userId, double salary, boolean incomePending) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.salary = salary;
        this.incomePending = incomePending;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public double getSalary() {
        return salary;
    }

    public boolean isIncomePending() {
        return incomePending;
    }

}
