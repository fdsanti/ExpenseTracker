package com.example.expensetracker;

public class AnalysisCategory {
    private String name;
    private double total;
    private float percentage;
    private int color; // The color assigned in the PieChart
    private int iconResId;

    public AnalysisCategory(String name, double total) {
        this.name = name;
        this.total = total;
    }

    // Getters and Setters
    public String getName() { return name; }
    public double getTotal() { return total; }
    public float getPercentage() { return percentage; }
    public void setPercentage(float percentage) { this.percentage = percentage; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}