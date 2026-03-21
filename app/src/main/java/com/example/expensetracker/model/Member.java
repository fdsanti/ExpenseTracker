package com.example.expensetracker.model;

public class Member {

    private String id;
    private String name;
    private double salary;

    public Member() {
    }

    public Member(String id, String name, double salary) {
        this.id = id;
        this.name = name;
        this.salary = salary;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getSalary() {
        return salary;
    }

}
