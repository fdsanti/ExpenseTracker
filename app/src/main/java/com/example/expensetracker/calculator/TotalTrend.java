package com.example.expensetracker.calculator;

public class TotalTrend {

    public enum Direction {
        UP,
        DOWN
    }

    private final int percentage;
    private final Direction direction;

    public TotalTrend(int percentage, Direction direction) {
        this.percentage = percentage;
        this.direction = direction;
    }

    public int getPercentage() {
        return percentage;
    }

    public Direction getDirection() {
        return direction;
    }
}
