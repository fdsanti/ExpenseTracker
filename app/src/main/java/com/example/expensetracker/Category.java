package com.example.expensetracker;

import java.util.ArrayList;
import java.util.List;

public class Category {
    private String name;

    // Required empty constructor for Firebase
    public Category() {}

    public Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name; // Helpful for Spinners later
    }

    public static List<Category> getDefaultCategories() {
        List<Category> defaults = new ArrayList<>();
        defaults.add(new Category("Salidas"));
        defaults.add(new Category("Delivery"));
        defaults.add(new Category("Super"));
        defaults.add(new Category("Gatitas"));
        defaults.add(new Category("Servicios"));
        defaults.add(new Category("Nafta / Peajes"));
        defaults.add(new Category("Olga"));
        defaults.add(new Category("Peajes"));
        defaults.add(new Category("Pago Casa"));
        defaults.add(new Category("Suscripciones"));
        defaults.add(new Category("Compras"));
        defaults.add(new Category("Otros"));
        return defaults;
    }
}