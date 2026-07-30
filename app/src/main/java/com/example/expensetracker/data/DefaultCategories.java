package com.example.expensetracker.data;

import com.example.expensetracker.model.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefaultCategories {

    public static final String OTHERS_ID = "c12";

    private static final String[] NAMES = {
            "Salidas",
            "Delivery",
            "Super",
            "Gatitas",
            "Servicios",
            "Nafta / Peajes",
            "Olga",
            "Auto",
            "Pago Casa",
            "Suscripciones",
            "Compras",
            "Otros"
    };

    private DefaultCategories() {
    }

    public static List<Category> asList() {
        List<Category> categories = new ArrayList<>();

        for (int i = 0; i < NAMES.length; i++) {
            categories.add(new Category(idForIndex(i), NAMES[i], true, i + 1));
        }

        return categories;
    }

    public static Map<String, Object> asFirebaseMap() {
        Map<String, Object> categoriesMap = new HashMap<>();

        for (int i = 0; i < NAMES.length; i++) {
            Map<String, Object> category = new HashMap<>();
            category.put("name", NAMES[i]);
            category.put("order", i + 1);
            category.put("active", true);
            category.put("system", false);

            categoriesMap.put(idForIndex(i), category);
        }

        return categoriesMap;
    }

    private static String idForIndex(int index) {
        return "c" + (index + 1);
    }
}
