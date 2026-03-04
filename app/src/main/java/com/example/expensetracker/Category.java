package com.example.expensetracker;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public enum Category {
    DELIVERY("Delivery", R.drawable.delivery, "#004573"),
    SALIDAS("Salidas", R.drawable.salidas, "#007356"),
    SUPER("Super", R.drawable.supermercado, "#A99E00"),
    GATITAS("Gatitas", R.drawable.gatitas, "#5C3FFF"),
    SERVICIOS("Servicios", R.drawable.servicios, "#9100A4"),
    NAFTA_PEAJES("Nafta / Peajes", R.drawable.nafta_peajes, "#A4003C"),
    OLGA("Olga", R.drawable.olga, "#A40019"),
    AUTO("Auto", R.drawable.auto, "#A45D00"),
    PAGO_CASA("Pago Casa", R.drawable.pago_casa, "#978600"),
    SUSCRIPCIONES("Suscripciones", R.drawable.suscripciones, "#731D00"),
    COMPRAS("Compras", R.drawable.compras, "#006D73"),
    OTROS("Otros", R.drawable.otros, "#5C5C5C");

    private final String displayName;
    private final int iconRes;
    private final int color;

    Category(String displayName, int iconRes, String colorHex) {
        this.displayName = displayName;
        this.iconRes = iconRes;
        this.color = Color.parseColor(colorHex);
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getIconRes() {
        return iconRes;
    }

    public int getColor() {
        return color;
    }

    /**
     * Used by ResumenFragment and RowViewAdapter to find the Enum
     * based on the string stored in Firebase.
     */
    public static Category fromString(String text) {
        if (text == null) return OTROS;
        String normalized = text.toLowerCase().trim();
        for (Category c : Category.values()) {
            if (c.displayName.toLowerCase().equals(normalized)) {
                return c;
            }
        }
        return OTROS;
    }

    /**
     * Fixes the error in RowViewAdapter where you call Category.getDefaultCategories()
     */
    public static List<Category> getDefaultCategories() {
        List<Category> list = new ArrayList<>();
        for (Category c : Category.values()) {
            list.add(c);
        }
        return list;
    }

    /**
     * Added helper so you can call c.getName() if your adapter expects that
     */
    public String getName() {
        return displayName;
    }
}